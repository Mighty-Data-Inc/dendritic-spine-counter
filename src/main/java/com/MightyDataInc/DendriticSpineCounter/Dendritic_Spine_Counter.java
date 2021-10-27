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
import net.imagej.ImageJ;
import net.imagej.display.DefaultImageCanvas;
import net.imagej.display.DefaultImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.legacy.LegacyService;
import net.imagej.ops.OpService;
import net.imagej.overlay.Overlay;
import net.imagej.roi.ROIService;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.command.Command;
import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.display.event.input.InputEvent;
import org.scijava.object.ObjectService;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.tool.Tool;
import org.scijava.tool.ToolService;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.awt.Color;
import java.awt.geom.Point2D;

//@Plugin(type = Command.class, menuPath = "Plugins>Dendritic Spine Counter")
public class Dendritic_Spine_Counter implements PlugIn, SciJavaPlugin, Command {

	private static final ImageJ imageJ = new ImageJ();

	final static String WORKING_IMAGE_WINDOW_TITLE = "Dendritic Spine Counter Working Image";

	public Dataset origDataset;
	private DscControlPanelDialog controlPanelDlg;
	private DefaultImageDisplay workingDisplay;
	public Img<UnsignedByteType> workingImg;
	public ImagePlus workingImp;
	private ij.gui.Overlay workingOverlay;

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
	public int featureSizePixels = 7;

	private OvalRoi currentSelectedSegmentRoi;

	// The ID value to assign to the next path that gets added to our overlay.
	// Incremented every time we add a path. Never decremented.
	private int nextPathId = 1;

	@Override
	public void run(String arg) {
		System.out.println(String.format("Method PlugIn.run was passed String argument: \"%s\". Argument was ignored.", arg));
		this.run();
	}	
	
	@Override
	public void run() {
		// Need to activate legacy mode, I guess.
		// https://imagej.net/libs/imagej-legacy
		System.setProperty("imagej.legacy.sync", "true");
		
		/*
		List<Dataset> currentDatasets = ij.dataset().getDatasets();
		if (currentDatasets.isEmpty()) {
			System.out.println("TODO: Open a file dialog");
		} else {
			// Get the last dataset.
			this.origDataset = currentDatasets.get(currentDatasets.size() - 1);
		}
		*/
		
		this.origDataset = imageJ.imageDisplay().getActiveDataset();
		if (this.origDataset == null) {
			final File file = imageJ.ui().chooseFile(null, "open");
			String filePathFromUserSelection = file.getPath();
			try {
				origDataset = imageJ.scifio().datasetIO().open(filePathFromUserSelection);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (origDataset == null) {
			IJ.noImage();
			return;
		}
		
		// show the image
		imageJ.ui().show(origDataset);					
		
		// Grab a reference to some tools, so that our control panel
		// can make use of them.
		polylineTool = imageJ.tool().getTool("Polyline");
		pointTool = imageJ.tool().getTool("Point");

		// Create our control panel.
		controlPanelDlg = new DscControlPanelDialog(this);

		createWorkingImage();

		this.workingImp = WindowManager.getCurrentImage();
		this.workingOverlay = this.workingImp.getOverlay();
		if (workingOverlay == null) {
			workingOverlay = new ij.gui.Overlay();
			workingImp.setOverlay(workingOverlay);
		}
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

		workingDisplay = (DefaultImageDisplay) imageJ.display().createDisplay(WORKING_IMAGE_WINDOW_TITLE, workingImg);

		maximizeContrast();
		// maximizeContrastRollingWindow(50);
	}

	public void dimImage() {
		long width = workingImg.dimension(0);
		long height = workingImg.dimension(1);

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		for (long y = 0; y < height; y++) {
			for (long x = 0; x < width; x++) {
				r.setPosition(new long[] { x, y });
				UnsignedByteType t = r.get();
				t.setReal(t.getRealFloat() / 2);
			}
		}
		workingDisplay.update();
	}

	@EventHandler
	void onEvent(InputEvent ev) {
		if (ev.getDisplay() != workingDisplay) {
			// This event wasn't on the display we care about.
			return;
		}
		// TODO: Do something with this event. It has x, y, and modifier info.
	}

	public void listPathPoints() {
		List<Point2D> pathPoints = null;

		// We will extract points from an ROI. But the question is, which
		// ROI will we use?

		// If we're running via Fiji, then the ImageJ1.x infrastructure will
		// provide us with a WindowManager, and the WindowManager will have a
		// current image. If we can get an ROI from there, then that is what
		// we will go with.

		ImagePlus currentImage = WindowManager.getCurrentImage();
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
			List<Overlay> overlays = imageJ.overlay().getOverlays();
			Collections.reverse(overlays);
			for (Overlay overlay : overlays) {
				List<Point2D> points = PointExtractor.getPointsFromOverlay(overlay);
				if (points.size() > 0) {
					pathPoints = points;
					break;
				}
			}
		}

		if (pathPoints != null) {
			for (Point2D point : pathPoints) {
				System.out.println(point);
			}
		}
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

	public void maximizeContrastRollingWindow(int windowRadius) {
		long width = workingImg.dimension(0);
		long height = workingImg.dimension(1);

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		for (int y = 0; y < height; y++) {
			System.out.println("Y: " + y);
			for (int x = 0; x < width; x++) {
				r.setPosition(new long[] { x, y });
				UnsignedByteType t = r.get();
				double pixelBrightness = t.getRealDouble();

				List<Double> brightnesses = this.getBrightnesses(x - windowRadius, y - windowRadius, x + windowRadius,
						y + windowRadius);

				double brightnessMin = Collections.min(brightnesses);
				double brightnessMax = Collections.max(brightnesses);

				double fracThisPixelBrightness = (pixelBrightness - brightnessMin) / (brightnessMax - brightnessMin);

				double pixelBrightnessNew = fracThisPixelBrightness * t.getMaxValue();
				t.setReal(pixelBrightnessNew);
			}
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

		ImagePlus currentImage = WindowManager.getCurrentImage();
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
			List<Overlay> overlays = imageJ.overlay().getOverlays();
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

	public DendriteSegment traceDendriteWithThicknessEstimation() {
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

		workingOverlay.add(dendriteVolumeRoi, dendriteVolumeRoi.toString());

		// https://forum.image.sc/t/how-to-update-properties-of-roi-simultaneously-as-its-values-in-dialog-box-change/21486/3
		// https://imagej.nih.gov/ij/developer/api/ij/ImagePlus.html#updateAndRepaintWindow--
		workingImp.updateAndRepaintWindow();

		path.id = this.nextPathId;
		this.nextPathId++;

		path.roi = dendriteVolumeRoi;

		return path.id;
	}

	public void RemovePathFromDrawOverlay(DendriteSegment path) {
		if (path == null || path.roi == null) {
			return;
		}
		workingOverlay.remove(path.roi);
		workingImp.updateAndRepaintWindow();
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
		workingImp.updateAndRepaintWindow();
	}

	public void SetSelectedSegmentCursor(SearchPixel px) {
		if (this.currentSelectedSegmentRoi != null) {
			this.workingOverlay.remove(currentSelectedSegmentRoi);
			this.currentSelectedSegmentRoi = null;
		}

		if (px == null) {
			return;
		}

		double totalSpan = 
				px.getPixelSidepathDistance(SearchPixel.PathSide.LEFT) + 
				px.getPixelSidepathDistance(SearchPixel.PathSide.RIGHT);

		totalSpan += 2 * this.featureSizePixels;
		double halfSpan = totalSpan / 2.0;

		this.currentSelectedSegmentRoi = new OvalRoi(px.x - halfSpan, px.y - halfSpan, totalSpan, totalSpan);
		this.currentSelectedSegmentRoi.setFillColor(new Color(0f, 1f, .5f, .5f));
		this.workingOverlay.add(currentSelectedSegmentRoi);
		workingImp.updateAndRepaintWindow();
	}
	
	
	public void AddPointRoisAsSpineMarkers(List<Point2D> spinePoints) {		
		PointRoi spinesRoi = new PointRoi();
		for (Point2D spinePoint: spinePoints) {
			spinesRoi.addPoint(spinePoint.getX(), spinePoint.getY());
		}
		workingImp.setRoi(spinesRoi, true);
		workingImp.updateAndRepaintWindow();
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
		if (workingImp == null) {
			return null;
		}
		Roi roi = workingImp.getRoi();
		if (roi == null) {
			return null;
		}

		List<Point2D> points = PointExtractor.getPointsFromLegacyRoi(roi);
		return points;
	}

	public void activatePolylineTool() {
		polylineTool.activate();
		imageJ.tool().setActiveTool(polylineTool);
		IJ.setTool("polyline");
	}

	public void setWorkingImageWindowToForeground() {
		if (this.workingImp == null) {
			return;
		}
		ImageWindow impwin = this.workingImp.getWindow();
		if (impwin == null) {
			return;
		}
		impwin.toFront();
	}

	public Calibration getWorkingImageDimensions() {
		if (this.workingImp == null) {
			return null;
		}
		Calibration cal = this.workingImp.getCalibration();
		if (cal == null) {
			return null;
		}
		if (!cal.scaled()) {
			return null;
		}
		if (cal.getUnits() == null) {
			return null;
		}
		return cal;
	}

	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		imageJ.ui().showUI();

		/*
		Dataset dataset = null;
		try {
			dataset = ij.scifio().datasetIO().open("C:\\\\Users\\mvol\\Desktop\\testpubz-MinIP.jpg");			
		} catch(Exception ex) {			
		}
		
		if (dataset == null) {
			final File file = ij.ui().chooseFile(null, "open");
			String filePathFromUserSelection = file.getPath();
			dataset = ij.scifio().datasetIO().open(filePathFromUserSelection);			
		}

		// show the image
		ij.ui().show(dataset);
		*/

		// invoke the plugin
		//ij.command().run(Dendritic_Spine_Counter.class, true);
		//Dendritic_Spine_Counter thePlugin = new Dendritic_Spine_Counter();
		//thePlugin.run( null );
		
		ij.IJ.runPlugIn("com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter", "unnecessaryArg");
	}

}