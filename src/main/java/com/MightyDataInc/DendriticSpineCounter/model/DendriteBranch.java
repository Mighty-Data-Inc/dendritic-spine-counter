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
	private List<DendritePixel> dendritePixels = new ArrayList<DendritePixel>();
	private Img<UnsignedByteType> img = null;
	private PolygonRoi roi = null;
	private int id = 0;

	// The branch's user-provided name. Overrides the default name, if supplied.
	public String name = "";

	private static int nextId = 1;

	public List<DendritePixel> getCenterLine() {
		return this.dendritePixels;
	}

	public PolygonRoi getRoi() {
		return this.roi;
	}

	public PolygonRoi setRoi(Roi roi) {
		PolygonRoi polygonRoi = null;
		try {
			polygonRoi = (PolygonRoi) roi;
		} catch (ClassCastException ex) {
		}

		this.roi = polygonRoi;
		return this.roi;
	}

	public int getId() {
		return id;
	}

	private DendriteBranch(List<DendritePixel> dendritePixels, Img<UnsignedByteType> img) {
		this.dendritePixels = dendritePixels;
		this.img = img;

		if (this.img != null) {
			this.roi = generatePolygonRoi();
		}

		this.id = nextId;
		nextId++;
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

		DendriteBranch dendrite = new DendriteBranch(dendritePixels, img);

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
	private List<Point2D> getSimilarityBoundaryPoints() {
		List<Point2D> leftPixels = this.getSidePath(DendritePixel.PathSide.LEFT);
		List<Point2D> rightPixels = this.getSidePath(DendritePixel.PathSide.RIGHT);

		Collections.reverse(rightPixels);
		leftPixels.addAll(rightPixels);

		return leftPixels;
	}

	private PolygonRoi generatePolygonRoi() {
		List<Point2D> points = this.getSimilarityBoundaryPoints();

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

	public List<Point2D> getRoiPoints() {
		if (this.roi == null) {
			return null;
		}

		int xcoords[] = roi.getXCoordinates();
		int ycoords[] = roi.getYCoordinates();

		if (xcoords.length != ycoords.length) {
			throw new IllegalArgumentException(
					String.format("Dendrite branch's ROI somehow has %d X coordinates but %d Y coordinates",
							xcoords.length, ycoords.length));
		}

		List<Point2D> points = new ArrayList<Point2D>();

		for (int i = 0; i < xcoords.length; i++) {
			int x = (int) (xcoords[i] + roi.getXBase());
			int y = (int) (ycoords[i] + roi.getYBase());
			Point2D point = new Point2D.Double(x, y);
			points.add(point);
		}
		return points;
	}

	public double getLengthInPixels() {
		DendritePixel lastPoint = null;
		double totalLength = 0;
		for (DendritePixel dpixel : dendritePixels) {
			if (lastPoint != null) {
				double dist = dpixel.distance(lastPoint);
				totalLength += dist;
			}
			lastPoint = dpixel;
		}
		return totalLength;
	}

	public String getName() {
		String s = this.name;
		if (s == null) {
			s = "";
		}
		s = s.trim();

		if (s.isEmpty()) {
			if (dendritePixels == null || dendritePixels.size() == 0) {
				s = "Path with no points specified";
			} else if (dendritePixels.size() == 1) {
				Point2D pixelFirst = dendritePixels.get(0);
				s = String.format("Dendrite branch #%d: (%d, %d)", this.id, (int) pixelFirst.getX(),
						(int) pixelFirst.getY());
			} else {
				Point2D pixelFirst = dendritePixels.get(0);
				Point2D pixelLast = dendritePixels.get(dendritePixels.size() - 1);
				s = String.format("Dendrite branch #%d: (%d, %d) to (%d, %d)", this.id, (int) pixelFirst.getX(),
						(int) pixelFirst.getY(), (int) pixelLast.getX(), (int) pixelLast.getY());
			}
		}
		return s;
	}

	@Override
	public String toString() {
		String s = this.getName();
		return s;
	}
}
