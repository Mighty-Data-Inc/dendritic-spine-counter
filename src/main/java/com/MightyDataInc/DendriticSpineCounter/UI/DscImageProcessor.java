package com.MightyDataInc.DendriticSpineCounter.UI;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.scijava.display.DisplayService;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteBranch;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;
import com.MightyDataInc.DendriticSpineCounter.model.PointExtractor;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import net.imagej.Dataset;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.DefaultImageDisplay;
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

	private OvalRoi featureSizeSelectorRoi;

	public DscImageProcessor(Dendritic_Spine_Counter ownerPlugin) {
		this.ownerPlugin = ownerPlugin;
	}

	public void update() {
		// if (this.workingDisplay != null) {
		// this.workingDisplay.update();
		// }

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
		for (int iAxis = 0; iAxis < workingDisplay.numDimensions(); iAxis++) {
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
				t.setReal(255 - t.getRealDouble());
			}
		}
		update();
	}

	public void drawPixels(List<? extends Point2D> pixels, double brightness) {
		if (workingImg == null) {
			return;
		}

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		for (Point2D pixel : pixels) {
			int x = (int) pixel.getX();
			int y = (int) pixel.getY();

			r.setPosition(new long[] { x, y });
			UnsignedByteType t = r.get();

			t.setReal(255 * brightness);
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

	public double getBrightnessAtFluidPixel(double x, double y) {
		// The brightness of the pixel will be weighted by how far into the
		// next pixel the value protrudes.

		long imgWidth = workingImg.dimension(0);
		long imgHeight = workingImg.dimension(1);

		if (x < 1) {
			x = 1;
		}
		if (y < 1) {
			y = 1;
		}
		if (x >= imgWidth - 1) {
			x = (int) imgWidth - 2;
		}
		if (y >= imgHeight - 1) {
			y = (int) imgHeight - 2;
		}

		double xBase = Math.floor(x);
		double yBase = Math.floor(y);

		double x1frac = x - xBase;
		double y1frac = y - yBase;

		double x0frac = 1.0 - x1frac;
		double y0frac = 1.0 - y1frac;

		double w00 = x0frac * y0frac;
		double w10 = x1frac * y0frac;
		double w01 = x0frac * y1frac;
		double w11 = x1frac * y1frac;

		final RandomAccess<UnsignedByteType> r = workingImg.randomAccess();

		r.setPosition(new long[] { (int) x, (int) y });
		UnsignedByteType t = r.get();
		double b00 = t.getRealDouble() / 255.0;

		r.setPosition(new long[] { (int) x + 1, (int) y });
		t = r.get();
		double b10 = t.getRealDouble() / 255.0;

		r.setPosition(new long[] { (int) x, (int) y + 1 });
		t = r.get();
		double b01 = t.getRealDouble() / 255.0;

		r.setPosition(new long[] { (int) x + 1, (int) y + 1 });
		t = r.get();
		double b11 = t.getRealDouble() / 255.0;

		double bWeighted = (b00 * w00) + (b10 * w10) + (b01 * w01) + (b11 * w11);
		return bWeighted;
	}

	/**
	 * Gets a list of values (normalized to a range of 0 to 1) of all pixels in a
	 * circle centered on this pixel (or an offset point).
	 * 
	 * @param xcenter         X position to center the search on.
	 * @param ycenter         Y position to center the search on.
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @param polygonExclude  If provided, the search refrains from examining pixels
	 *                        that are within this region of exclusion.
	 * @return A list of pixel values between 0 and 1, where 0 is black and 1 is
	 *         white, representing values outside the donut hole radius.
	 */
	public List<Double> getBrightnessesWithinRadius(int xcenter, int ycenter, double radius, double donutHoleRadius,
			PolygonRoi polygonExclude) {
		List<Double> pixelValues = new ArrayList<java.lang.Double>();
		List<java.lang.Double> donutHoleValues = new ArrayList<java.lang.Double>();
		// NOTE: We don't actually return donutHoleValues currently.

		long imgWidth = workingImg.dimension(0);
		long imgHeight = workingImg.dimension(1);

		long xStart = (long) (xcenter - Math.ceil(radius));
		if (xStart < 0) {
			xStart = 0;
		}
		long yStart = (long) (ycenter - Math.ceil(radius));
		if (yStart < 0) {
			yStart = 0;
		}
		long xEnd = (long) (xcenter + Math.ceil(radius));
		if (xEnd >= imgWidth) {
			xEnd = imgWidth - 1;
		}
		long yEnd = (long) (ycenter + Math.ceil(radius));
		if (yEnd >= imgHeight) {
			yEnd = imgHeight - 1;
		}

		final RandomAccess<? extends RealType<?>> r = workingImg.randomAccess();

		r.setPosition(new long[] { xStart, yStart });

		for (long y = yStart; y <= yEnd; y++) {
			for (long x = xStart; x <= xEnd; x++) {
				if (polygonExclude != null && polygonExclude.contains((int) x, (int) y)) {
					continue;
				}

				r.setPosition(new long[] { x, y });
				RealType<?> t = r.get();

				double distance = Math.hypot((x - xcenter), (y - ycenter));
				if (distance > radius) {
					continue;
				}

				double value = t.getRealDouble() / t.getMaxValue();
				if (distance < donutHoleRadius) {
					donutHoleValues.add(value);
				} else {
					pixelValues.add(value);
				}
			}
		}
		return pixelValues;
	}

	/**
	 * Gets a statistical description of all pixels in a circle centered on this
	 * pixel (or an offset point).
	 * 
	 * @param xcenter         X position to center the search on.
	 * @param ycenter         Y position to center the search on.
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @param polygonExclude  If provided, the search refrains from examining pixels
	 *                        that are within this region of exclusion.
	 * @return A statistical description of the pixels within the circle.
	 */
	public SummaryStatistics getBrightnessVicinityStats(int xcenter, int ycenter, double radius, double donutHoleRadius,
			PolygonRoi polygonExclude) {
		List<java.lang.Double> values = getBrightnessesWithinRadius(xcenter, ycenter, radius, donutHoleRadius,
				polygonExclude);

		SummaryStatistics stat = new SummaryStatistics();
		for (java.lang.Double value : values) {
			stat.addValue(value);
		}
		return stat;
	}

	public List<Point2D> getCurrentImagePolylinePathPoints(int roiType) {
		List<Point2D> pathPoints = null;

		ImagePlus currentImage = getImagePlus();
		if (currentImage == null) {
			return null;
		}

		Roi currentRoi = currentImage.getRoi();
		if (currentRoi == null) {
			return null;
		}

		PolygonRoi polyRoi = null;
		try {
			polyRoi = (PolygonRoi) currentRoi;

			if (roiType != 0 && polyRoi.getType() != roiType) {
				// NOTE: It's possible that we should also try casting to a PointRoi.
				return null;
			}
		} catch (ClassCastException ex) {
			return null;
		}

		List<Point2D> points = PointExtractor.getPointsFromLegacyRoi(polyRoi);
		if (points.size() > 0) {
			pathPoints = points;
		}

		return pathPoints;
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

	public OvalRoi showHideFeatureSizeSelectorRoi(boolean show) {
		if (!show) {
			if (featureSizeSelectorRoi != null) {
				getOverlay().remove(featureSizeSelectorRoi);
				getImagePlus().deleteRoi();
				getImagePlus().updateAndRepaintWindow();
				featureSizeSelectorRoi = null;
			}
			return null;
		}

		double pixelRadius = this.ownerPlugin.getModel().getFeatureWindowSizeInPixels() / 2;

		if (featureSizeSelectorRoi != null) {
			getOverlay().remove(featureSizeSelectorRoi);
			getImagePlus().deleteRoi();
			getImagePlus().updateAndRepaintWindow();
			featureSizeSelectorRoi = null;
		}

		ImageCanvas canvas = getImagePlus().getCanvas();
		Rectangle rect = canvas.getSrcRect();

		featureSizeSelectorRoi = new OvalRoi(rect.x + (rect.width / 2) - pixelRadius,
				rect.y + (rect.height / 2) - pixelRadius, pixelRadius * 2, pixelRadius * 2);
		featureSizeSelectorRoi.setFillColor(new Color(0f, .5f, 1f, .25f));
		featureSizeSelectorRoi.setName("Feature Size Selector");
		getOverlay().add(featureSizeSelectorRoi);

		this.moveToForeground();
		ownerPlugin.activatePolylineTool();

		this.setCurrentRoi(featureSizeSelectorRoi);
		getImagePlus().updateAndRepaintWindow();

		return featureSizeSelectorRoi;
	}

	public void drawDendriteOverlays() {
		// Remove all dendrite an spine overlays first, and repaint them second.
		getOverlay().clear();

		for (DendriteBranch dendrite : this.ownerPlugin.getModel().getDendrites()) {
			getOverlay().add(dendrite.getRoi());
		}

		for (DendriteSpine spine : this.ownerPlugin.getModel().getSpines()) {
			getOverlay().add(spine.getRoi());
		}
	}

	public Roi setCurrentRoi(Roi roi) {
		// Draw all the persistent overlays, then remove the one that is currently being
		// edited and add it back in as a current selection.
		this.drawDendriteOverlays();

		if (roi != null) {
			getOverlay().remove(roi);
			getImagePlus().setRoi(roi, true);
			return getImagePlus().getRoi();
		} else {
			getImagePlus().deleteRoi();
			return null;
		}
	}

	public double getFeatureSizeSelectorRoiSizeInPixels() {
		if (this.featureSizeSelectorRoi == null) {
			return 0;
		}
		double px = this.featureSizeSelectorRoi.getFloatWidth();
		return px;
	}

}
