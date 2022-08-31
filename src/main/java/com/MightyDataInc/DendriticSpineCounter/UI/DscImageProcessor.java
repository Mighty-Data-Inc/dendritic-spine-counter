package com.MightyDataInc.DendriticSpineCounter.UI;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.scijava.display.DisplayService;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSegment;
import com.MightyDataInc.DendriticSpineCounter.model.PointExtractor;
import com.MightyDataInc.DendriticSpineCounter.model.SearchPixel;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import net.imagej.Dataset;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DefaultImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.overlay.Overlay;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class DscImageProcessor {
	public Dendritic_Spine_Counter ownerPlugin;

	private DefaultImageDisplay workingDisplay;
	public Img<UnsignedByteType> workingImg;

	private String workingImgWindowTitle;

	private OvalRoi currentSelectedSegmentRoi;

	public DscImageProcessor(Dendritic_Spine_Counter ownerPlugin) {
		this.ownerPlugin = ownerPlugin;
	}

	public void update() {
		if (this.workingDisplay != null) {
			this.workingDisplay.update();
		}

		ImagePlus imp = this.getImagePlus();
		if (imp != null) {
			imp.updateAndRepaintWindow();
		}
	}

	public DefaultImageDisplay getDisplay() {
		return workingDisplay;
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

	public Img<UnsignedByteType> createWorkingImage(Dataset origDataset, DisplayService displayService) {
		workingImg = convertToGrayscale(origDataset);

		workingImgWindowTitle = "Dendritic Spine Counter Working Image";
		try {
			String origDataName = origDataset.getName();
			if (origDataName != null && !origDataName.isEmpty()) {
				workingImgWindowTitle += ": " + origDataName;
			}
		} catch (Exception e1) {
		}

		workingDisplay = (DefaultImageDisplay) (displayService.createDisplay(workingImgWindowTitle, workingImg));
		
		// Copy the axis calibrations to this display.
		CalibratedAxis[] axes = new CalibratedAxis[origDataset.numDimensions()];
		origDataset.axes(axes);
		for (int iAxis = 0; iAxis < axes.length; iAxis++) {
			workingDisplay.setAxis(axes[iAxis], iAxis);
		}

		maximizeContrast();

		return workingImg;
	}

	public ImagePlus getImagePlus() {
		return WindowManager.getImage(workingImgWindowTitle);
	}

	private ij.gui.Overlay getOverlay() {
		ij.gui.Overlay overlay = getImagePlus().getOverlay();
		if (overlay == null) {
			overlay = new ij.gui.Overlay();
			getImagePlus().setOverlay(overlay);
		}
		return overlay;
	}

	public Calibration getDimensions() {
		ImagePlus workingImp = getImagePlus();
		if (workingImp == null) {
			return null;
		}
		Calibration cal = workingImp.getCalibration();
		if (cal == null || cal.getUnit() == null) {
			return null;
		}
		return cal;
	}

	public void maximizeContrast() {
		if (workingImg == null) {
			return;
		}

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

		update();
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
		update();
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

	public List<Point2D> getCurrentImagePolylinePathPoints(OverlayService overlayService) {
		List<Point2D> pathPoints = null;

		// We will extract points from an ROI. But the question is, which
		// ROI will we use?

		// If we're running via Fiji, then the ImageJ1.x infrastructure will
		// provide us with a WindowManager, and the WindowManager will have a
		// current image. If we can get an ROI from there, then that is what
		// we will go with.

		ImagePlus currentImage = getImagePlus();
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
		if (pathPoints == null && overlayService != null) {
			List<Overlay> overlays = overlayService.getOverlays();
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

	// TODO: Maybe move this.
	public DendriteSegment traceDendriteWithThicknessEstimation(double thicknessMultiplier,
			double maxSearchDistanceWindows, OverlayService overlayService) {
		List<Point2D> pathPoints = getCurrentImagePolylinePathPoints(overlayService);
		if (pathPoints == null || pathPoints.size() == 0) {
			return null;
		}

		DendriteSegment polylineTracedPath = DendriteSegment.searchPolyline(pathPoints, workingImg, null);

		int pixelWindowSize = this.ownerPlugin.controlPanelDlg.getFeatureDetectionWindowSizeInPixels();
		polylineTracedPath.setMinimumSeparation(pixelWindowSize);
		polylineTracedPath.smoothify(0.5);

		polylineTracedPath.computeTangentsAndOrthogonals();

		polylineTracedPath.findSimilarityBoundaries(pixelWindowSize, pixelWindowSize * maxSearchDistanceWindows);

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

		getOverlay().add(dendriteVolumeRoi, dendriteVolumeRoi.toString());

		// https://forum.image.sc/t/how-to-update-properties-of-roi-simultaneously-as-its-values-in-dialog-box-change/21486/3
		// https://imagej.nih.gov/ij/developer/api/ij/ImagePlus.html#updateAndRepaintWindow--
		getImagePlus().updateAndRepaintWindow();

		path.id = this.ownerPlugin.nextPathId;
		this.ownerPlugin.nextPathId++;

		path.roi = dendriteVolumeRoi;

		/*
		 * Calibration cal = this.getDimensions(); if (cal != null &&
		 * !cal.getUnits().isEmpty()) { double pixelLength = path.pixelLength();
		 * path.nameSuffix = String.format(", length: %.3f %s", cal.getX(pixelLength),
		 * cal.getUnits()); }
		 */

		return path.id;
	}

	public void RemovePathFromDrawOverlay(DendriteSegment path) {
		if (path == null || path.roi == null) {
			return;
		}
		getOverlay().remove(path.roi);
		getImagePlus().updateAndRepaintWindow();
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
		getImagePlus().updateAndRepaintWindow();
	}

	public OvalRoi SetSelectedSegmentCursor(SearchPixel px, double featureSizePixels) {
		if (currentSelectedSegmentRoi != null) {
			this.getOverlay().remove(currentSelectedSegmentRoi);
			currentSelectedSegmentRoi = null;
		}

		if (px == null) {
			return null;
		}

		double totalSpan = px.getPixelSidepathDistance(SearchPixel.PathSide.LEFT)
				+ px.getPixelSidepathDistance(SearchPixel.PathSide.RIGHT);

		totalSpan += 2 * featureSizePixels;
		double halfSpan = totalSpan / 2.0;

		OvalRoi newRoi = new OvalRoi(px.x - halfSpan, px.y - halfSpan, totalSpan, totalSpan);
		newRoi.setFillColor(new Color(0f, 1f, .5f, .5f));
		getOverlay().add(newRoi);
		getImagePlus().updateAndRepaintWindow();

		currentSelectedSegmentRoi = newRoi;
		return newRoi;
	}

	public void AddPointRoisAsSpineMarkers(List<Point2D> spinePoints) {
		PointRoi spinesRoi = new PointRoi();
		for (Point2D spinePoint : spinePoints) {
			spinesRoi.addPoint(spinePoint.getX(), spinePoint.getY());
		}
		getImagePlus().setRoi(spinesRoi, true);
		getImagePlus().updateAndRepaintWindow();
	}

	public List<Point2D> getPointsFromCurrentPolylineRoiSelection() {
		if (getImagePlus() == null) {
			return new ArrayList<Point2D>();
		}
		Roi roi = getImagePlus().getRoi();
		if (roi == null) {
			return new ArrayList<Point2D>();
		}

		List<Point2D> points = PointExtractor.getPointsFromLegacyRoi(roi);
		return points;
	}

	public void moveToForeground() {
		ImagePlus imp = getImagePlus();
		if (imp == null) {
			return;
		}
		ImageWindow impwin = imp.getWindow();
		if (impwin == null) {
			return;
		}
		impwin.toFront();
	}
}