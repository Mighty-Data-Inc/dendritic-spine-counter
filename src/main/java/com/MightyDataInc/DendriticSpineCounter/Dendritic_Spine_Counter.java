package com.MightyDataInc.DendriticSpineCounter;

import net.imagej.Dataset;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import io.scif.SCIFIO;
import net.imagej.ImageJ;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DefaultImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.overlay.Overlay;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.json.simple.JSONObject;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.display.event.input.InputEvent;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.tool.Tool;
import org.scijava.tool.ToolService;
import org.scijava.ui.UIService;

import com.MightyDataInc.DendriticSpineCounter.DscControlPanelDialog.FeatureDetectionWindowSizeUnitsEnum;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import java.awt.Color;
import java.awt.geom.Point2D;

@Plugin(type = Command.class, menuPath = "Plugins>Dendritic Spine Counter")
public class Dendritic_Spine_Counter implements PlugIn, SciJavaPlugin, Command {
	// Provide package version introspection capabilities by setting the Maven model
	// object.
	// https://stackoverflow.com/questions/3697449/retrieve-version-from-maven-pom-xml-in-code
	public Model maven;

	private static final ImageJ imageJLegacy = new ImageJ();

	@Parameter
	ImageDisplayService imageDisplayService;

	@Parameter
	UIService uiService;

	@Parameter
	SCIFIO scifioService;

	@Parameter
	ToolService toolService;

	@Parameter
	DisplayService displayService;

	@Parameter
	OverlayService overlayService;

	public Dataset origDataset;
	private DscControlPanelDialog controlPanelDlg;
	private DefaultImageDisplay workingDisplay;
	public Img<UnsignedByteType> workingImg;

	public Tool polylineTool;
	public Tool pointTool;

	// This parameter determines how "different" a spine's brightness
	// can be from the brightness of the dendrite that the spine comes off of.
	// If it's set to zero, then the spine would have to be exactly the same
	// brightness (or darkness) as the central line of the dendrite. In terms
	// of units, it represents the number of standard deviations of difference
	// in pixel value samplings that the spine can have and still be considered
	// part of the same dendritic structure.
	public double FEATURE_SENSITIVITY_ALLOWANCE = 4.0;

	// How far out from the polyline to search for the thickness of the dendrite.
	// Expressed as a multiple of how many pixel window sizes to search out to.
	public double MAX_SEARCH_DISTANCE_IN_PIXEL_WINDOW_SIZE_TIMES = 5.0;

	// How big the features to scan for are, in pixels.
	public int featureSizePixels = 5;

	private OvalRoi currentSelectedSegmentRoi;

	// The ID value to assign to the next path that gets added to our overlay.
	// Incremented every time we add a path. Never decremented.
	private int nextPathId = 1;

	private Calibration cachedCalibration = null;

	// The IDE environment injects services with a "legacy" model, whereas
	// Fiji uses a "new" model. But it's inconsistent in both cases. It's maddening.
	void makeServicesWorkWithBothIDEAndFiji() {
		if (this.imageDisplayService == null) {
			this.imageDisplayService = imageJLegacy.imageDisplay();
		}
		if (this.uiService == null) {
			this.uiService = imageJLegacy.ui();
		}
		if (this.scifioService == null) {
			this.scifioService = imageJLegacy.scifio();
		}
		if (this.toolService == null) {
			this.toolService = imageJLegacy.tool();
		}
		if (this.displayService == null) {
			this.displayService = imageJLegacy.display();
		}
		if (this.overlayService == null) {
			this.overlayService = imageJLegacy.overlay();
		}
	}

	private String getWorkingImageWindowTitle() {
		String title = "Dendritic Spine Counter Working Image";
		try {
			String origDataName = this.origDataset.getName();
			if (origDataName != null && !origDataName.isEmpty()) {
				title += ": " + origDataName;
			}
		} catch (Exception e1) {
		}
		return title;
	}

	@Override
	public void run(String arg) {
		System.out.println(
				String.format("Method PlugIn.run was passed String argument: \"%s\". Argument was ignored.", arg));
		this.run();
	}

	@Override
	public void run() {
		// First set up Maven introspection.
		try {
			MavenXpp3Reader reader = new MavenXpp3Reader();
			this.maven = reader.read(new FileReader("pom.xml"));
		} catch (XmlPullParserException e1) {
		} catch (IOException e1) {
		}

		makeServicesWorkWithBothIDEAndFiji();

		JSONObject jsonObj = null;

		this.origDataset = imageDisplayService.getActiveDataset();
		if (this.origDataset == null) {
			final File file = uiService.chooseFile(null, "open");
			String filePathFromUserSelection = file.getPath();

			String datasetFilePath = filePathFromUserSelection;
			if (filePathFromUserSelection.toLowerCase().endsWith(".json")) {
				String fileJsonPath = filePathFromUserSelection;
				jsonObj = DscControlPanelDialog.getJsonObjectFromFile(fileJsonPath);

				datasetFilePath = "";
				if (jsonObj.containsKey("originalimagefile")) {
					datasetFilePath = jsonObj.get("originalimagefile").toString().trim();
				}
				if (datasetFilePath == null || datasetFilePath.isEmpty()) {
					JOptionPane.showMessageDialog(null,
							"This JSON file doesn't contain a record of which image file it corresponds to.",
							"Missing original file image record", JOptionPane.ERROR_MESSAGE);
					IJ.noImage();
					return;
				}
			}

			try {
				origDataset = scifioService.datasetIO().open(datasetFilePath);
			} catch (InvalidPathException e1) {
				JOptionPane.showMessageDialog(null, "Couldn't read this string as a file path: " + datasetFilePath,
						"Invalid path", JOptionPane.ERROR_MESSAGE);
			} catch (NoSuchFileException e1) {
				JOptionPane.showMessageDialog(null, "The system could not find any such file: " + datasetFilePath,
						"No such file", JOptionPane.ERROR_MESSAGE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (origDataset == null) {
				IJ.noImage();
				return;
			}
			// show the image
			this.uiService.show(origDataset);
		}

		// Grab a reference to some tools, so that our control panel
		// can make use of them.
		polylineTool = this.toolService.getTool("Polyline");
		pointTool = this.toolService.getTool("Point");

		// Create our control panel.
		controlPanelDlg = new DscControlPanelDialog(this);

		createWorkingImage();

		if (jsonObj != null) {
			controlPanelDlg.loadFromJsonObject(jsonObj);
		}
	}

	public Display<?> getOriginalDatasetDisplay() {
		List<Display<?>> origDatasetDisplays = this.displayService.getDisplays(this.origDataset);
		return origDatasetDisplays.get(0);
	}

	// We can't return an ImagePlus object because of introspection.
	// We're just going to have to cast everywhere.
	public Object getWorkingImagePlus() {
		return WindowManager.getImage(getWorkingImageWindowTitle());
	}

	public ij.gui.Overlay getWorkingOverlay() {
		ij.gui.Overlay overlay = ((ImagePlus) getWorkingImagePlus()).getOverlay();
		if (overlay == null) {
			overlay = new ij.gui.Overlay();
			((ImagePlus) getWorkingImagePlus()).setOverlay(overlay);
		}
		return overlay;
	}

	public void plotSearchPixels(Collection<SearchPixel> pixels, int color) {
		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		for (SearchPixel pixel : pixels) {
			r.setPosition(new long[] { pixel.x, pixel.y });
			UnsignedByteType t = r.get();
			t.setReal(color);
		}
		workingDisplay.update();
	}

	public void createWorkingImage() {
		workingImg = convertToGrayscale(this.origDataset);

		workingDisplay = (DefaultImageDisplay) this.displayService.createDisplay(getWorkingImageWindowTitle(),
				workingImg);

		maximizeContrast();

		// Grab the original dataset's dimensional calibrations.
		CalibratedAxis[] axes = new CalibratedAxis[origDataset.numDimensions()];
		origDataset.axes(axes);
		double pixelsPerPhysicalUnit = axes[0].calibratedValue(1);

		String unitName = axes[0].unit();
		cachedCalibration = new Calibration();
		cachedCalibration.setUnit(unitName);
		cachedCalibration.pixelWidth = pixelsPerPhysicalUnit;
		cachedCalibration.pixelHeight = pixelsPerPhysicalUnit;

		this.controlPanelDlg.textfieldFeatureDetectionWindowSize.setText("0.5");

		this.controlPanelDlg.enumFeatureDetectionWindowSizeUnits = FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS;

		this.controlPanelDlg.updateInputSpecificationButtonEnablements();
	}

	@EventHandler
	void onEvent(InputEvent ev) {
		if (ev.getDisplay() != workingDisplay) {
			// This event wasn't on the display we care about.
			return;
		}
		// TODO: Do something with this event. It has x, y, and modifier info.
		this.controlPanelDlg.updateInputSpecificationButtonEnablements();
	}

	public void invertImage() {
		long width = workingImg.dimension(0);
		long height = workingImg.dimension(1);

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		for (long x = 0; x < width; x++) {
			for (long y = 0; y < height; y++) {
				r.setPosition(new long[] { x, y });
				UnsignedByteType t = r.get();
				t.setReal(255 - t.getRealFloat());
			}
		}
		workingDisplay.update();
	}

	public void maximizeContrast() {
		long width = workingImg.dimension(0);
		long height = workingImg.dimension(1);

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		r.setPosition(new long[] { 0, 0 });

		float darkestVal = r.get().getRealFloat();
		float lightestVal = r.get().getRealFloat();

		for (long y = 0; y < height; y++) {
			for (long x = 0; x < width; x++) {
				r.setPosition(new long[] { x, y });
				UnsignedByteType t = r.get();

				float tVal = t.getRealFloat();

				if (tVal < darkestVal) {
					darkestVal = tVal;
				}
				if (tVal > lightestVal) {
					lightestVal = tVal;
				}

			}
		}

		float newMin = 0;
		float newMax = 255;
		float origMin = darkestVal;
		float origMax = lightestVal;

		float slope = (newMax - newMin) / (origMax - origMin);

		for (long x = 0; x < width; x++) {
			for (long y = 0; y < height; y++) {
				r.setPosition(new long[] { x, y });
				UnsignedByteType t = r.get();
				t.setReal(slope * (t.getRealFloat() - origMin) + newMin);
			}
		}

		if (workingDisplay != null) {
			workingDisplay.update();
		}
	}

	public static <T extends RealType<?>> Img<UnsignedByteType> convertToGrayscale(Img<T> imgIn) {
		ImgFactory<UnsignedByteType> marioGrayImageFactory = new ArrayImgFactory<UnsignedByteType>(
				new UnsignedByteType());
		Img<UnsignedByteType> img = marioGrayImageFactory.create(imgIn.dimension(0), imgIn.dimension(1));

		final RandomAccess<T> colorCursor = imgIn.randomAccess();
		final RandomAccess<UnsignedByteType> grayCursor = img.randomAccess();

		for (long x = 0; x < imgIn.dimension(0); x++) {
			for (long y = 0; y < imgIn.dimension(1); y++) {

				colorCursor.setPosition(new long[] { x, y, 0 });
				T redPixel = colorCursor.get();
				double red = redPixel.getRealDouble() / redPixel.getMaxValue();

				colorCursor.setPosition(new long[] { x, y, 1 });
				T greenPixel = colorCursor.get();
				double green = greenPixel.getRealDouble() / greenPixel.getMaxValue();

				colorCursor.setPosition(new long[] { x, y, 2 });
				T bluePixel = colorCursor.get();
				double blue = bluePixel.getRealDouble() / bluePixel.getMaxValue();

				double combinedBrightness = (red * 0.299) + (green * 0.587) + (blue * 0.114);

				grayCursor.setPosition(new long[] { x, y });
				grayCursor.get().set((int) (combinedBrightness * 255.0));
			}
		}
		return img;
	}

	public List<Point2D> getCurrentImagePolylinePathPoints() {
		List<Point2D> pathPoints = null;

		// We will extract points from an ROI. But the question is, which
		// ROI will we use?

		// If we're running via Fiji, then the ImageJ1.x infrastructure will
		// provide us with a WindowManager, and the WindowManager will have a
		// current image. If we can get an ROI from there, then that is what
		// we will go with.

		ImagePlus currentImage = ((ImagePlus) getWorkingImagePlus());
		if (currentImage != null) {
			Roi currentRoi = currentImage.getRoi();
			if (currentRoi != null) {
				List<Point2D> points = PointExtractor.getPointsFromLegacyRoi(currentRoi);
				if (points.size() > 0) {
					pathPoints = points;
				}
			}
		}

		// If we're running via a debugger or possibly the command line, then
		// we might just have overlay information from the OverlayService.
		// The overlays in the OverlayService might have ROIs that are not
		// registered the ROIService. As such, we need to access them via
		// the overlays, because we don't have access to them anywhere else.
		// If this is the case, then we want the LAST overlay of the
		// appropriate type (hence the reverse), because that's the one that
		// the user added just before (or during) invoking this plugin.
		if (pathPoints == null) {
			List<Overlay> overlays = this.overlayService.getOverlays();
			Collections.reverse(overlays);
			for (Overlay overlay : overlays) {
				List<Point2D> points = PointExtractor.getPointsFromOverlay(overlay);
				if (points.size() > 0) {
					pathPoints = points;
					break;
				}
			}
		}

		return pathPoints;
	}

	public DendriteSegment traceDendriteWithThicknessEstimation(double thicknessMultiplier) {
		List<Point2D> pathPoints = getCurrentImagePolylinePathPoints();
		if (pathPoints == null) {
			return null;
		}

		DendriteSegment polylineTracedPath = DendriteSegment.searchPolyline(pathPoints, workingImg, null);

		int pixelWindowSize = this.controlPanelDlg.getFeatureDetectionWindowSizeInPixels();
		polylineTracedPath.setMinimumSeparation(pixelWindowSize);
		polylineTracedPath.smoothify(0.5);

		polylineTracedPath.computeTangentsAndOrthogonals();

		polylineTracedPath.findSimilarityBoundaries(pixelWindowSize,
				pixelWindowSize * MAX_SEARCH_DISTANCE_IN_PIXEL_WINDOW_SIZE_TIMES);

		polylineTracedPath.multiplyThickness(thicknessMultiplier);
		polylineTracedPath.smoothSimilarityBoundaryDistances(0.5);

		return polylineTracedPath;
	}

	public int AddPathToDrawOverlay(DendriteSegment path) {
		if (path == null) {
			return 0;
		}
		PolygonRoi dendriteVolumeRoi = path.getSimilarityVolume();

		dendriteVolumeRoi.setStrokeColor(Color.BLUE);
		dendriteVolumeRoi.setStrokeWidth(1.5);
		dendriteVolumeRoi.setFillColor(new Color(.4f, .6f, 1f, .4f));

		getWorkingOverlay().add(dendriteVolumeRoi, dendriteVolumeRoi.toString());

		// https://forum.image.sc/t/how-to-update-properties-of-roi-simultaneously-as-its-values-in-dialog-box-change/21486/3
		// https://imagej.nih.gov/ij/developer/api/ij/ImagePlus.html#updateAndRepaintWindow--
		((ImagePlus) getWorkingImagePlus()).updateAndRepaintWindow();

		path.id = this.nextPathId;
		this.nextPathId++;

		path.roi = dendriteVolumeRoi;

		Calibration cal = this.getWorkingImageDimensions();
		if (cal != null && !cal.getUnits().isEmpty()) {
			double pixelLength = path.pixelLength();
			path.nameSuffix = String.format(", length: %.3f %s", cal.getX(pixelLength), cal.getUnits());
		}

		return path.id;
	}

	public void RemovePathFromDrawOverlay(DendriteSegment path) {
		if (path == null || path.roi == null) {
			return;
		}
		getWorkingOverlay().remove(path.roi);
		((ImagePlus) getWorkingImagePlus()).updateAndRepaintWindow();
	}

	public void SelectPath(DendriteSegment path, boolean isSelected) {
		if (path == null || path.roi == null) {
			return;
		}

		if (isSelected) {
			path.roi.setFillColor(new Color(.6f, 1f, 1f, .4f));
		} else {
			path.roi.setFillColor(new Color(.4f, .6f, 1f, .4f));
		}
		((ImagePlus) getWorkingImagePlus()).updateAndRepaintWindow();
	}

	public void SetSelectedSegmentCursor(SearchPixel px) {
		if (this.currentSelectedSegmentRoi != null) {
			this.getWorkingOverlay().remove(currentSelectedSegmentRoi);
			this.currentSelectedSegmentRoi = null;
		}

		if (px == null) {
			return;
		}

		double totalSpan = px.getPixelSidepathDistance(SearchPixel.PathSide.LEFT)
				+ px.getPixelSidepathDistance(SearchPixel.PathSide.RIGHT);

		totalSpan += 2 * this.featureSizePixels;
		double halfSpan = totalSpan / 2.0;

		this.currentSelectedSegmentRoi = new OvalRoi(px.x - halfSpan, px.y - halfSpan, totalSpan, totalSpan);
		this.currentSelectedSegmentRoi.setFillColor(new Color(0f, 1f, .5f, .5f));
		this.getWorkingOverlay().add(currentSelectedSegmentRoi);
		((ImagePlus) getWorkingImagePlus()).updateAndRepaintWindow();
	}

	public void AddPointRoisAsSpineMarkers(List<Point2D> spinePoints) {
		PointRoi spinesRoi = new PointRoi();
		for (Point2D spinePoint : spinePoints) {
			spinesRoi.addPoint(spinePoint.getX(), spinePoint.getY());
		}
		((ImagePlus) getWorkingImagePlus()).setRoi(spinesRoi, true);
		((ImagePlus) getWorkingImagePlus()).updateAndRepaintWindow();
	}

	public List<Double> getBrightnesses(int xStart, int yStart, int xEnd, int yEnd) {
		List<Double> brightnesses = new ArrayList<Double>();

		long imgWidth = workingImg.dimension(0);
		long imgHeight = workingImg.dimension(1);

		if (xStart < 0) {
			xStart = 0;
		}
		if (yStart < 0) {
			yStart = 0;
		}
		if (xEnd >= imgWidth) {
			xEnd = (int) imgWidth - 1;
		}
		if (yEnd >= imgHeight) {
			yEnd = (int) imgHeight - 1;
		}

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();
		r.setPosition(new long[] { xStart, yStart });

		for (int y = yStart; y <= yEnd; y++) {
			for (int x = xStart; x <= xEnd; x++) {
				r.setPosition(new long[] { x, y });
				UnsignedByteType t = r.get();

				brightnesses.add(t.getRealDouble());
			}
		}

		return brightnesses;
	}

	public List<Point2D> getPointsFromCurrentPolylineRoiSelection() {
		if (((ImagePlus) getWorkingImagePlus()) == null) {
			return new ArrayList<Point2D>();
		}
		Roi roi = ((ImagePlus) getWorkingImagePlus()).getRoi();
		if (roi == null) {
			return new ArrayList<Point2D>();
		}

		List<Point2D> points = PointExtractor.getPointsFromLegacyRoi(roi);
		return points;
	}

	public void activatePolylineTool() {
		polylineTool.activate();
		toolService.setActiveTool(polylineTool);
		IJ.setTool("polyline");
	}

	public void setWorkingImageWindowToForeground() {
		if (((ImagePlus) getWorkingImagePlus()) == null) {
			return;
		}
		ImageWindow impwin = ((ImagePlus) getWorkingImagePlus()).getWindow();
		if (impwin == null) {
			return;
		}
		impwin.toFront();
	}

	public Calibration getWorkingImageDimensions() {
		ImagePlus workingImp = ((ImagePlus) getWorkingImagePlus());
		if (workingImp == null) {
			return null;
		}
		Calibration cal = workingImp.getCalibration();
		if (cal == null || !cal.scaled() || cal.getUnits() == null) {
			cal = this.cachedCalibration;
			workingImp.setCalibration(cal);
		}
		return cal;
	}

	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		imageJLegacy.ui().showUI();

		/*
		 * Dataset dataset = null; try { dataset =
		 * ij.scifio().datasetIO().open("C:\\\\Users\\mvol\\Desktop\\testpubz-MinIP.jpg"
		 * ); } catch(Exception ex) { }
		 * 
		 * if (dataset == null) { final File file = ij.ui().chooseFile(null, "open");
		 * String filePathFromUserSelection = file.getPath(); dataset =
		 * ij.scifio().datasetIO().open(filePathFromUserSelection); }
		 * 
		 * // show the image ij.ui().show(dataset);
		 */

		// invoke the plugin
		// ij.command().run(Dendritic_Spine_Counter.class, true);
		// Dendritic_Spine_Counter thePlugin = new Dendritic_Spine_Counter();
		// thePlugin.run( null );

		ij.IJ.runPlugIn("com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter", "unnecessaryArg");
	}

}