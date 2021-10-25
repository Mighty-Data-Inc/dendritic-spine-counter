package com.MightyDataInc.DendriticSpineCounter;

import java.awt.Polygon;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import net.imagej.overlay.GeneralPathOverlay;
import net.imagej.overlay.Overlay;
import net.imglib2.roi.GeneralPathRegionOfInterest;
import net.imglib2.roi.RegionOfInterest;

@SuppressWarnings("deprecation")
public class PointExtractor {
	/**
	 * Protected constructor, so this class can't be instantiated. Essentially, its
	 * caller is forced to use it as a namespace.
	 */
	protected PointExtractor() {
	}

	/**
	 * Extracts a list of points from the overlay. The points are in dataspace
	 * coordinates, not canvas coordinates, which means that we can use them to
	 * directly extract pixels from the source image.
	 * 
	 * @param overlay The overlay to pull a list of 2D points from.
	 * @return The points from the overlay. If the overlay is null or has an
	 *         unrecognized type, it will return an empty list.
	 */
	public static ArrayList<Point2D> getPointsFromOverlay(Overlay overlay) {
		if (overlay == null) {
			return new ArrayList<Point2D>();
		}
		return getPointsFromROI(overlay.getRegionOfInterest());
	}

	/**
	 * Extracts a list of points from the ROI. The points are in dataspace
	 * coordinates, not canvas coordinates, which means that we can use them to
	 * directly extract pixels from the source image.
	 * 
	 * @param roi The RegionOfInterest to pull a list of 2D points from.
	 * @return The points from the overlay. If the overlay is null or has an
	 *         unrecognized type, it will return an empty list.
	 */
	protected static ArrayList<Point2D> getPointsFromROI(RegionOfInterest roi) {
		if (roi instanceof GeneralPathRegionOfInterest) {
			return getPointsFromROI((GeneralPathRegionOfInterest) roi);
		}
		return new ArrayList<Point2D>();
	}

	/**
	 * Extracts a list of points from the legacy Roi object. The points are in
	 * dataspace coordinates, not canvas coordinates, which means that we can use
	 * them to directly extract pixels from the source image.
	 * 
	 * @param roi The legacy Roi object to pull a list of 2D points from.
	 * @return The points from the overlay. If the overlay is null or has an
	 *         unrecognized type, it will return an empty list.
	 */
	protected static ArrayList<Point2D> getPointsFromLegacyRoi(Roi roi) {
		ArrayList<Point2D> pathPoints = new ArrayList<Point2D>();
		if (roi == null) {
			return pathPoints;
		}
		Polygon polygon = roi.getPolygon();
		if (polygon == null) {
			return pathPoints;
		}

		for (int iPoint = 0; iPoint < polygon.npoints; iPoint++) {
			int x = polygon.xpoints[iPoint];
			int y = polygon.ypoints[iPoint];
			Point2D pathPoint = new Point2D.Double(x, y);
			pathPoints.add(pathPoint);
		}
		return pathPoints;
	}

	/**
	 * Specialization of getPointsFromROI for GeneralPathRegionOfInterest.
	 * 
	 * @param roi The RegionOfInterest to pull a list of 2D points from.
	 * @return The points from the overlay. If the overlay is null or has an
	 *         unrecognized type, it will return an empty list.
	 */
	protected static ArrayList<Point2D> getPointsFromROI(GeneralPathRegionOfInterest roi) {
		ArrayList<Point2D> pathPoints = new ArrayList<Point2D>();
		if (roi == null) {
			return new ArrayList<Point2D>();
		}

		GeneralPath path = roi.getGeneralPath();
		PathIterator pi = path.getPathIterator(null);
		while (!pi.isDone()) {
			// The currentSegment method populates an empty array --
			// essentially, it uses an input variable as an output.
			// It returns the path-segment type (straight, curved, etc.),
			// which we don't really care about for our purposes here.
			// To avoid an out-of-bounds exception, we need to pass it
			// an array that can store *all* of the dimensions of the
			// overlay, even though we're only using (x, y). In practice
			// we should use ImageJ's Axis functions to determine which
			// axis is x and y, but considering that this whole process
			// is only ever intended for use with 2D images and we'll always
			// be oriented flat relative to the axis of the slice,
			// it doesn't really matter.
			double[] coords = new double[roi.numDimensions()];
			pi.currentSegment(coords);

			Point2D point = new Point2D.Double(coords[0], coords[1]);
			pathPoints.add(point);

			pi.next();
		}
		return pathPoints;
	}

	/**
	 * Creates a general path overlay out of a series of points.
	 * 
	 * @param points The points to make a path overlay out of. points in the
	 *               returned path overlay. Points in the overlay will be farther
	 *               apart than this number of pixels.
	 * @return
	 */
	public static GeneralPathOverlay createGeneralPathOverlay(List<Point2D> points) {
		GeneralPath generalPath = new GeneralPath();
		boolean isFirstPoint = true;
		for (Point2D pointInOverlay : points) {
			if (isFirstPoint) {
				generalPath.moveTo(pointInOverlay.getX(), pointInOverlay.getY());
				isFirstPoint = false;
				continue;
			}
			generalPath.lineTo(pointInOverlay.getX(), pointInOverlay.getY());
		}

		GeneralPathRegionOfInterest generalPathROI = new GeneralPathRegionOfInterest();
		generalPathROI.setGeneralPath(generalPath);

		GeneralPathOverlay overlay = new GeneralPathOverlay(null);
		overlay.setRegionOfInterest(generalPathROI);

		return overlay;
	}
}
