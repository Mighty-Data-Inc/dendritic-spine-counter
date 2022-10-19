package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.MightyDataInc.DendriticSpineCounter.UI.DscImageProcessor;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class DendriteBranch {
	// NOTE: Dendrite pixels are only necessary for the purpose of generating
	// the ROI object. In practice, we should have a separate DendriteBranchTracer
	// class that owns these DendritePixels, and the DendriteBranch only owns
	// the resultant PolygonRoi.
	private List<DendritePixel> dendritePixels = new ArrayList<DendritePixel>();

	private PolygonRoi roi = null;
	private double lengthInPixels = 0;

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

	private DendriteBranch(List<DendritePixel> dendritePixels) {
		this.dendritePixels = dendritePixels;

		this.lengthInPixels = computeLengthInPixels();
		this.roi = generatePolygonRoi();

		this.id = nextId;
		nextId++;
	}

	// Empty constructor, for deserialization only!
	private DendriteBranch() {
	}

	public static DendriteBranch fromPathPoints(List<Point2D> pathPoints, double featureWindowSize,
			DscImageProcessor imageProcessor) {
		if (pathPoints == null || pathPoints.size() == 0) {
			return null;
		}

		List<TracerPixel> darktrace = TracerPixel.trace(pathPoints, imageProcessor.workingImg, null);
		if (darktrace == null || darktrace.size() == 0) {
			return null;
		}

		List<DendritePixel> dendritePixels = DendritePixel.fromTracers(darktrace, featureWindowSize, imageProcessor);
		if (dendritePixels == null || dendritePixels.size() == 0) {
			return null;
		}

		DendriteBranch dendrite = new DendriteBranch(dendritePixels);

		return dendrite;
	}

	public static DendriteBranch traceDendriteWithThicknessEstimation(double featureWindowSize,
			DscImageProcessor imageProcessor) {
		List<Point2D> pathPoints = imageProcessor.getCurrentImagePolylinePathPoints(Roi.POLYLINE);
		if (pathPoints == null || pathPoints.size() == 0) {
			return null;
		}

		DendriteBranch dendriteBranch = DendriteBranch.fromPathPoints(pathPoints, featureWindowSize, imageProcessor);

		imageProcessor.drawDendriteOverlays();
		imageProcessor.setCurrentRoi(dendriteBranch.getRoi());

		return dendriteBranch;
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
		roi = decorateRoi(roi);

		return roi;
	}

	private static PolygonRoi decorateRoi(PolygonRoi roi) {
		roi.setStrokeColor(Color.BLUE);
		roi.setStrokeWidth(1.5);
		roi.setFillColor(new Color(.4f, .6f, 1f, .4f));
		return roi;
	}

	public List<Point2D> getRoiPoints() {
		List<Point2D> points = new ArrayList<Point2D>();

		if (this.roi == null) {
			return points;
		}

		int xcoords[] = roi.getPolygon().xpoints;
		int ycoords[] = roi.getPolygon().ypoints;

		if (xcoords.length != ycoords.length) {
			throw new IllegalArgumentException(
					String.format("Dendrite branch's ROI somehow has %d X coordinates but %d Y coordinates",
							xcoords.length, ycoords.length));
		}

		for (int i = 0; i < xcoords.length; i++) {
			int x = (int) (xcoords[i]);
			int y = (int) (ycoords[i]);
			Point2D point = new Point2D.Double(x, y);
			points.add(point);
		}
		return points;
	}

	public double getLengthInPixels() {
		return this.lengthInPixels;
	}

	public double computeLengthInPixels() {
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

	public Point2D getRepresentativeCoordinates() {
		// Interestingly enough, this *doesn't* refer to the mean of all
		// the pixel points or ROI points, because the structure could be
		// concave. Instead, we're going to pick a "representative" point.

		if (dendritePixels.size() > 3) {
			// We pick the median dendrite pixel.
			int iMedian = dendritePixels.size() / 2;
			Point2D ptMedian = dendritePixels.get(iMedian);
			return ptMedian;
		}

		List<Point2D> roiPoints = getRoiPoints();
		if (roiPoints.size() < 3) {
			// We have neither a set of dendrite pixels, nor a ROI.
			// We're just a pointless dude.
			return null;
		}

		// We take the point that's 1/4 of the way around the perimeter,
		// and then we take the point that's 3/4 of the way around the perimeter
		// because they should be roughly adjacent to one another.
		int iRoi1Quartile = (int) (roiPoints.size() * 0.25);
		int iRoi3Quartile = (int) (roiPoints.size() * 0.75);
		Point2D pt1Qt = roiPoints.get(iRoi1Quartile);
		Point2D pt3Qt = roiPoints.get(iRoi3Quartile);

		Point2D ptCenter = new Point2D.Double((pt1Qt.getX() + pt3Qt.getX()) / 2.0, (pt1Qt.getY() + pt3Qt.getY()) / 2.0);
		return ptCenter;
	}

	public String getName() {
		String s = this.name;
		if (s == null) {
			s = "";
		}
		s = s.trim();

		if (s.isEmpty()) {
			Point2D ptCenter = getRepresentativeCoordinates();

			if (ptCenter == null) {
				s = String.format("Dendrite branch #%d", this.id);
			} else {
				s = String.format("Dendrite branch #%d @(%d, %d)", this.id, (int) ptCenter.getX(),
						(int) ptCenter.getY());
			}
		}
		return s;
	}

	@Override
	public String toString() {
		String s = this.getName();
		return s;
	}

	public List<Point2D> getPeripheryPoints(double pixelsOutside, double pixelsBetween) {
		List<Point2D> ptsOut = new ArrayList<Point2D>();
		Point2D ptPrev = null;

		for (Point2D ptRim : this.getRoiPoints()) {
			if (ptPrev == null) {
				ptPrev = ptRim;
				continue;
			}

			double dist = ptPrev.distance(ptRim);

			Point2D vecTangent = new Point2D.Double((ptRim.getX() - ptPrev.getX()) / dist,
					(ptRim.getY() - ptPrev.getY()) / dist);
			Point2D vecOrth = new Point2D.Double(vecTangent.getY() * pixelsOutside, -vecTangent.getX() * pixelsOutside);

			for (double i = 0; i < dist; i++) {
				double dfrac = i / dist;
				double x = (ptPrev.getX() * dfrac) + (ptRim.getX() * (1 - dfrac)) + vecOrth.getX();
				double y = (ptPrev.getY() * dfrac) + (ptRim.getY() * (1 - dfrac)) + vecOrth.getY();
				Point2D pt = new Point2D.Double(x, y);
				ptsOut.add(pt);
			}

			ptPrev = ptRim;
		}

		List<Point2D> ptsOutSpaced = new ArrayList<Point2D>();
		ptPrev = null;
		for (Point2D pt : ptsOut) {
			if (ptPrev == null) {
				ptsOutSpaced.add(pt);
				ptPrev = pt;
				continue;
			}

			double dist = pt.distance(ptPrev);
			if (dist < pixelsBetween) {
				continue;
			}

			ptsOutSpaced.add(pt);
			ptPrev = pt;
		}

		return ptsOutSpaced;
	}

	public double getAreaInPixels() {
		List<Point2D> points = getRoiPoints();
		if (points.size() < 3) {
			return 0;
		}
		// Use the Slicker algorithm.
		// https://www.geeksforgeeks.org/slicker-algorithm-to-find-the-area-of-a-polygon-in-java/

		double total = 0;
		for (int i = 0; i < points.size(); i++) {
			int iNext = (i + 1) % points.size();

			Point2D pt = points.get(i);
			Point2D ptNext = points.get(iNext);

			double thisTerm = (pt.getX() * ptNext.getY()) - (ptNext.getX() * pt.getY());
			total += thisTerm;
		}

		double area = total / 2;
		return area;
	}

	public double getAverageWidthInPixels() {
		double avgWidth = this.getAreaInPixels() / this.getLengthInPixels();
		return avgWidth;
	}

	@SuppressWarnings("unchecked")
	public JSONObject saveToJsonObject() {
		JSONObject jsonDendrite = new JSONObject();

		jsonDendrite.put("id", this.id);
		jsonDendrite.put("name", (this.name == null) ? "" : this.name);
		jsonDendrite.put("length_in_pixels", this.lengthInPixels);

		JSONObject jsonPolygon = new JSONObject();

		JSONArray jsonXPoints = new JSONArray();
		JSONArray jsonYPoints = new JSONArray();
		Polygon polygon = this.roi.getPolygon();
		for (int i = 0; i < polygon.npoints; i++) {
			jsonXPoints.add(polygon.xpoints[i]);
			jsonYPoints.add(polygon.ypoints[i]);
		}
		jsonPolygon.put("x", jsonXPoints);
		jsonPolygon.put("y", jsonYPoints);

		jsonDendrite.put("polygon", jsonPolygon);
		return jsonDendrite;
	}

	public static DendriteBranch loadFromJsonObject(JSONObject jsonDendrite) {
		DendriteBranch dendrite = new DendriteBranch();

		dendrite.id = ((Long) jsonDendrite.get("id")).intValue();
		if (dendrite.id >= nextId) {
			nextId = dendrite.id + 1;
		}

		dendrite.name = (String) jsonDendrite.get("name");
		if (dendrite.name == null) {
			dendrite.name = "";
		}

		dendrite.lengthInPixels = (double) jsonDendrite.get("length_in_pixels");

		JSONObject jsonPolygon = (JSONObject) jsonDendrite.get("polygon");

		JSONArray jsonXPoints = (JSONArray) jsonPolygon.get("x");
		JSONArray jsonYPoints = (JSONArray) jsonPolygon.get("y");

		int npoints = jsonXPoints.size();
		if (npoints != jsonYPoints.size()) {
			throw new IllegalArgumentException("PolygonRoi has different number of X and Y coordinates.");
		}

		float[] xpoints = new float[npoints];
		float[] ypoints = new float[npoints];
		for (int iPoint = 0; iPoint < npoints; iPoint++) {
			xpoints[iPoint] = ((Long) jsonXPoints.get(iPoint)).floatValue();
			ypoints[iPoint] = ((Long) jsonYPoints.get(iPoint)).floatValue();
		}

		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, Roi.POLYGON);
		roi = decorateRoi(roi);
		dendrite.roi = roi;

		return dendrite;
	}
}
