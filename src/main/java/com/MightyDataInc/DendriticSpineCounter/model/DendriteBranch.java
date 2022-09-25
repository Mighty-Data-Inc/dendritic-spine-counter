package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class DendriteBranch {
	public List<DendritePixel> dendritePixels = new ArrayList<DendritePixel>();
	public Img<UnsignedByteType> img = null;
	public PolygonRoi roi = null;

	private DendriteBranch() {
	}

	public static DendriteBranch fromPathPoints(List<Point2D> pathPoints, double featureWindowSize,
			Img<UnsignedByteType> img) {
		if (pathPoints == null || pathPoints.size() == 0) {
			return null;
		}

		List<TracerPixel> darktrace = TracerPixel.trace(pathPoints, img, null);
		if (darktrace == null || darktrace.size() == 0) {
			return null;
		}

		List<DendritePixel> dendritePixels = DendritePixel.fromTracers(darktrace, featureWindowSize, img);
		if (dendritePixels == null || dendritePixels.size() == 0) {
			return null;
		}

		DendriteBranch dendrite = new DendriteBranch();
		dendrite.dendritePixels = dendritePixels;
		dendrite.img = img;
		dendrite.roi = dendrite.generatePolygonRoi();

		return dendrite;
	}

	private List<Point2D> getSidePath(DendritePixel.PathSide side) {
		List<Point2D> sidePixels = new ArrayList<Point2D>();
		for (DendritePixel dpixel : dendritePixels) {
			Point2D orthoRightPx = dpixel.getPixelSidepathPoint(side);
			sidePixels.add(orthoRightPx);
		}
		return sidePixels;
	}

	/**
	 * Takes the resulting points on the left and right sides of the similarity
	 * boundaries and creates a closed polygon overlay.
	 * 
	 * @return A legacy polygon Roi that can be added to the image or the
	 *         RoiManager.
	 */
	private List<Point2D> getPolygonPoints() {
		List<Point2D> leftPixels = this.getSidePath(DendritePixel.PathSide.LEFT);
		List<Point2D> rightPixels = this.getSidePath(DendritePixel.PathSide.RIGHT);

		Collections.reverse(rightPixels);
		leftPixels.addAll(rightPixels);

		return leftPixels;
	}

	private PolygonRoi generatePolygonRoi() {
		List<Point2D> points = this.getPolygonPoints();

		float[] xPoints = new float[points.size()];
		float[] yPoints = new float[points.size()];

		for (int i = 0; i < points.size(); i++) {
			Point2D searchPixel = points.get(i);
			xPoints[i] = (float) searchPixel.getX();
			yPoints[i] = (float) searchPixel.getY();
		}

		PolygonRoi roi = new PolygonRoi(xPoints, yPoints, Roi.POLYGON);

		roi.setStrokeColor(Color.BLUE);
		roi.setStrokeWidth(1.5);
		roi.setFillColor(new Color(.4f, .6f, 1f, .4f));

		return roi;
	}
}
