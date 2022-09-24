package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import ij.gui.PolygonRoi;

public class DendritePixel extends Point2D {
	public enum PathSide {
		LEFT, RIGHT
	}

	public int x;
	public int y;
	public Img<? extends RealType<?>> fromImg;

	double featureWindowSize = 0;
	public Point2D unitNormal = new Point2D.Double(0, 0);
	public double similarityBoundaryDistanceRight = 0;
	public double similarityBoundaryDistanceLeft = 0;

	public SummaryStatistics vicinityStats = null;

	public static <T extends RealType<?>> List<DendritePixel> fromTracers(List<? extends Point2D> tracers,
			double featureWindowSize, Img<T> img) {
		if (tracers == null || tracers.size() == 0) {
			return null;
		}

		List<DendritePixel> dpixels = new ArrayList<DendritePixel>();

		Point2D firstTracer = tracers.get(0);
		DendritePixel prevDendritePixel = new DendritePixel(img, (int) firstTracer.getX(), (int) firstTracer.getY());
		dpixels.add(prevDendritePixel);

		for (Point2D tracer : tracers) {
			double distFromLastDendritePixel = prevDendritePixel.distance(tracer);
			if (distFromLastDendritePixel < featureWindowSize / 2.0) {
				continue;
			}

			prevDendritePixel = new DendritePixel(img, (int) tracer.getX(), (int) tracer.getY());
			dpixels.add(prevDendritePixel);
		}

		// We need to add the last pixel. But we already have a dendrite pixel
		// at the end of our list, and it is probably closer than the feature size window
		// from the end. So we lop off the last entry in our list and replace it with the true end,
		// allowing for the last segment to be slightly too long.
		Point2D lastTracer = tracers.get(tracers.size() - 1);
		DendritePixel lastDendritePixel = new DendritePixel(img, (int) lastTracer.getX(), (int) lastTracer.getY());
		dpixels.remove(dpixels.size() - 1);
		dpixels.add(lastDendritePixel);
		
		// TODO: Calculate normals.
		
		// TODO: Search for thickness boundaries.

		return dpixels;
	}

	public DendritePixel() {
	}

	public <T extends RealType<?>> DendritePixel(Img<T> img, int x, int y) {
		this.fromImg = img;
		this.x = x;
		this.y = y;
	}

	public String key() {
		String str = "" + x + ", " + y;
		return str;
	}

	@Override
	public String toString() {
		String str = key();
		return str;
	}

	public double getPixelSidepathDistance(PathSide side) {
		double value = side == DendritePixel.PathSide.LEFT ? similarityBoundaryDistanceLeft
				: similarityBoundaryDistanceRight;
		return Math.abs(value);
	}

	void setPixelSidepathDistance(PathSide side, double distance) {
		distance = Math.abs(distance);
		if (side == DendritePixel.PathSide.LEFT) {
			similarityBoundaryDistanceLeft = -distance;
		} else {
			similarityBoundaryDistanceRight = distance;
		}
	}

	/**
	 * Returns a DendritePixel object whose coordinates mark the "similarity
	 * boundary" in a given direction. All the pixel values between the returned
	 * pixel and the "this" pixel are "similar" to the "this" pixel. All the pixel
	 * values beyond the similarity boundary point are "different" from "this". Must
	 * be called after orthogonal and tangential vectors have already been set.
	 * 
	 * @param side Determines if we're looking at the left-hand boundary (where
	 *             direction is determined by the path set by DendritePixelPath) or
	 *             the right-hand one.
	 * @return A point that marks the edge of the similarity boundary.
	 */
	public DendritePixel getSimilarityBoundaryPoint(PathSide side) {
		return this.getSimilarityBoundaryPoint(side, 0);
	}

	/**
	 * Returns a DendritePixel object whose coordinates mark the "similarity
	 * boundary" in a given direction. All the pixel values between the returned
	 * pixel and the "this" pixel are "similar" to the "this" pixel. All the pixel
	 * values beyond the similarity boundary point are "different" from "this". Must
	 * be called after orthogonal and tangential vectors have already been set, and
	 * after findSimilarityBoundaries has already been called.
	 * 
	 * @param side            Determines if we're looking at the left-hand boundary
	 *                        (where direction is determined by the path set by
	 *                        DendritePixelPath) or the right-hand one.
	 * @param outsideDistance How many pixels out from the distance boundary we
	 *                        should check.
	 * @return A point that marks the edge of the similarity boundary.
	 */
	public DendritePixel getSimilarityBoundaryPoint(PathSide side, double outsideDistance) {
		return null;

//		double dist = side == PathSide.LEFT ? this.similarityBoundaryDistanceLeft
//				: this.similarityBoundaryDistanceRight;
//
//		double distMagnitude = Math.abs(dist);
//		double distanceMultiplier = (distMagnitude + outsideDistance) / distMagnitude;
//
//		DendritePixel pxBoundary = new DendritePixel(this.fromImg,
//				this.x + (int) (this.unitOrthogonal.getX() * dist * distanceMultiplier),
//				this.y + (int) (this.unitOrthogonal.getY() * dist * distanceMultiplier));
//		return pxBoundary;
	}

	/**
	 * Searches outward in both directions along the orthogonal vector until it
	 * finds a region whose pixels are "different" from the center. "Different"
	 * means that they fail a bespoke statistical test for similarity. This method
	 * expects the orthogonal vector to be set, so it must be called after
	 * DendritePixelPath.computeTangentsAndOrthogonals. This method is quite
	 * computationally expensive.
	 * 
	 * @param vicinityRadius The size of the area to test for similarity.
	 * @param distanceMax    The maximum distance to search out to.
	 */
	public void findSimilarityBoundaries(double vicinityRadius, double distanceMax) {
		this.vicinityStats = this.computePixelVicinityStats(vicinityRadius);

		this.similarityBoundaryDistanceRight = this.findSimilarityBoundaryInOrthogonalDirection(vicinityRadius, 1,
				distanceMax);
		this.similarityBoundaryDistanceLeft = this.findSimilarityBoundaryInOrthogonalDirection(vicinityRadius, -1,
				distanceMax);
	}

	/**
	 * Searches linearly in one direction, orthogonal to the path, until it finds a
	 * region whose pixels are "different" from the center. "Different" means that
	 * they fail a bespoke statistical test for similarity. This method depends on
	 * the vicinityStats member to already be populated, so it must be called after
	 * computePixelVicinityStats. It also expects the orthogonal vector to be set,
	 * so it must be called after DendritePixelPath.computeTangentsAndOrthogonals.
	 * This method is quite computationally expensive.
	 * 
	 * @param vicinityRadius The size of the area to test for similarity.
	 * @param multiplier     Applies to the orthogonal vector. +1 for searching
	 *                       rightward, -1 for leftward.
	 * @param distanceMax    The maximum distance to search out to.
	 * @return The distance, in the direction of the orthogonal vector, at which
	 *         pixels start to look "different".
	 */
	public double findSimilarityBoundaryInOrthogonalDirection(double vicinityRadius, double multiplier,
			double distanceMax) {
		return 0;

//		double distance = vicinityRadius * Math.signum(multiplier);
//		while (true) {
//			distance += multiplier;
//			if (Math.abs(distance) > distanceMax) {
//				return distance;
//			}
//
//			int boundaryTestX = (int) (this.x + (this.unitOrthogonal.getX() * distance));
//			int boundaryTestY = (int) (this.y + (this.unitOrthogonal.getY() * distance));
//
//			DendritePixel boundaryTestPixel = new DendritePixel(this.fromImg, boundaryTestX, boundaryTestY);
//			boundaryTestPixel.vicinityStats = boundaryTestPixel.computePixelVicinityStats(vicinityRadius);
//
//			boolean isDifferent = this.isVicinityDifferent(boundaryTestPixel, 2.0);
//			if (isDifferent == true) {
//				return distance;
//			}
//		}
	}

	/**
	 * Given a collection of pixels, returns the mean brightness of the darkest
	 * vicinity.
	 * 
	 * @param pxs A collection of DendritePixel objects. They need to have all had
	 *            their vicinities computed.
	 * @return The lowest mean vicinity value of all the DendritePixel objects.
	 */
	public static double getDarkestVicinityBrightness(List<DendritePixel> pxs) {
		SummaryStatistics stat = new SummaryStatistics();
		for (DendritePixel px : pxs) {
			stat.addValue(px.vicinityStats.getMean());
		}
		return stat.getMin();
	}

	/**
	 * Given a collection of pixels, returns the mean brightnesses of their
	 * vicinities.
	 * 
	 * @param pxs A collection of DendritePixel objects. They need to have all had
	 *            their vicinities computed.
	 * @return The lowest mean vicinity value of all the DendritePixel objects.
	 */
	public static double getMeanVicinityBrightness(List<DendritePixel> pxs) {
		SummaryStatistics stat = new SummaryStatistics();
		for (DendritePixel px : pxs) {
			stat.addValue(px.vicinityStats.getMean());
		}
		return stat.getMean();
	}

	/**
	 * Compares the vicinity stats of the two pixels. Returns false if they could
	 * have come from the same sample, true if they probably didn't.
	 * 
	 * @param otherPixel   The other pixel to compare the similarity stats of. Must
	 *                     have had computePixelVicinityStats called (as have we).
	 * @param zscoreCutoff The number of standard deviations of difference at which
	 *                     we consider the samples "different". Lower values make
	 *                     the comparison more likely to call two vicinities
	 *                     "different".
	 * @return True if the pixels probably came from different vicinities.
	 */
	public boolean isVicinityDifferent(DendritePixel otherPixel, double zscoreCutoff) {
		boolean isDifferent = areStatsDifferent(this.vicinityStats, otherPixel.vicinityStats, zscoreCutoff);
		return isDifferent;
	}

	/**
	 * Compares two SummaryStatistics objects. Performs a bespoke comparison to
	 * determine if they come from different sample sets.
	 * 
	 * @param st1
	 * @param st2
	 * @param zscoreCutoff The number of standard deviations of difference at which
	 *                     we consider the samples "different". Lower values make
	 *                     the comparison more likely to call two vicinities
	 *                     "different".
	 * @return True if the stats probably came from different samples.
	 */
	public static boolean areStatsDifferent(SummaryStatistics st1, SummaryStatistics st2, double zscoreCutoff) {
		// Compute the boundary test pixel vicinity's zscore relative to our own
		// vicinity.
		double meanDiff = Math.abs(st1.getMean() - st2.getMean());
		double stdMin = Math.min(st1.getStandardDeviation(), st2.getStandardDeviation());
		double stdMax = Math.max(st1.getStandardDeviation(), st2.getStandardDeviation());

		if (stdMin == 0) {
			// One of the samples is uniform. The other sample
			// has to also be uniform in the exact same way.
			if (stdMax == 0 && meanDiff == 0) {
				// They match!
				return false;
			}
			// Else they don't match.
			return true;
		}

		double zscore = meanDiff / st1.getStandardDeviation();
		double zscoreStdevDifferencePenalty = stdMax / stdMin;
		zscore *= zscoreStdevDifferencePenalty;

		if (zscore >= zscoreCutoff) {
			return true;
		}
		return false;
	}

	/**
	 * Gets a statistical description of all pixels in a circle centered on this
	 * pixel.
	 * 
	 * @param center The center pixel from which to scan out and collect values.
	 * @param radius The radius of the circle to collect pixel values from.
	 * @return A statistical description of the pixels within the circle.
	 */
	public SummaryStatistics computePixelVicinityStats(double radius) {
		return computePixelVicinityStats(radius, 0);
	}

	/**
	 * Gets a statistical description of all pixels in a circle centered on this
	 * pixel.
	 * 
	 * @param center          The center pixel from which to scan out and collect
	 *                        values.
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @return A statistical description of the pixels within the circle.
	 */
	public SummaryStatistics computePixelVicinityStats(double radius, double donutHoleRadius) {
		List<Double> values = getPixelValuesWithinRadius(radius, donutHoleRadius)[0];

		SummaryStatistics stat = new SummaryStatistics();
		for (Double value : values) {
			// stat.addValue(value);
		}

		this.vicinityStats = stat;
		return stat;
	}

	/**
	 * Gets a list of values (normalized to a range of 0 to 1) of all pixels in a
	 * circle centered on this pixel.
	 * 
	 * @param radius The radius of the circle to collect pixel values from.
	 * @return A list of pixel values between 0 and 1, where 0 is black and 1 is
	 *         white.
	 */
	public List<Double> getPixelValuesWithinRadius(double radius) {
		return this.getPixelValuesWithinRadius(radius, 0)[0];
	}

	/**
	 * Gets a list of values (normalized to a range of 0 to 1) of all pixels in a
	 * circle centered on this pixel.
	 * 
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @return Two lists of doubles. Both are lists of pixel values between 0 and 1,
	 *         where 0 is black and 1 is white. The first is a list of values
	 *         outside the donut hole radius. The other is a list of values inside.
	 */
	public List<Double>[] getPixelValuesWithinRadius(double radius, double donutHoleRadius) {
		return getPixelValuesWithinRadius(radius, donutHoleRadius, null);
	}

	/**
	 * Gets a list of values (normalized to a range of 0 to 1) of all pixels in a
	 * circle centered on this pixel.
	 * 
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @param polygonExclude  If provided, the search refrains from examining pixels
	 *                        that are within this region of exclusion.
	 * @return Two lists of doubles. Both are lists of pixel values between 0 and 1,
	 *         where 0 is black and 1 is white. The first is a list of values
	 *         outside the donut hole radius. The other is a list of values inside.
	 */
	public List<Double>[] getPixelValuesWithinRadius(double radius, double donutHoleRadius, PolygonRoi polygonExclude) {
		return null;
		/*
		 * List<Double> pixelValues = new ArrayList<Double>(); List<Double>
		 * donutHoleValues = new ArrayList<Double>();
		 * 
		 * long imgWidth = this.fromImg.dimension(0); long imgHeight =
		 * this.fromImg.dimension(1);
		 * 
		 * long xStart = (long) (this.x - Math.ceil(radius)); if (xStart < 0) { xStart =
		 * 0; } long yStart = (long) (this.y - Math.ceil(radius)); if (yStart < 0) {
		 * yStart = 0; } long xEnd = (long) (this.x + Math.ceil(radius)); if (xEnd >=
		 * imgWidth) { xEnd = imgWidth - 1; } long yEnd = (long) (this.y +
		 * Math.ceil(radius)); if (yEnd >= imgHeight) { yEnd = imgHeight - 1; }
		 * 
		 * final RandomAccess<? extends RealType<?>> r = this.fromImg.randomAccess();
		 * 
		 * r.setPosition(new long[] { xStart, yStart });
		 * 
		 * for (long y = yStart; y <= yEnd; y++) { for (long x = xStart; x <= xEnd; x++)
		 * { if (polygonExclude != null && polygonExclude.contains((int) x, (int) y)) {
		 * continue; }
		 * 
		 * r.setPosition(new long[] { x, y }); RealType<?> t = r.get();
		 * 
		 * double distance = Math.hypot((x - this.x), (y - this.y)); if (distance >
		 * radius) { continue; }
		 * 
		 * double value = t.getRealDouble() / t.getMaxValue(); if (distance <
		 * donutHoleRadius) { donutHoleValues.add(value); } else {
		 * pixelValues.add(value); } } }
		 * 
		 * // Java does not have the ability to create a new type with the proper //
		 * templating. // https://www.geeksforgeeks.org/array-of-arraylist-in-java/
		 * 
		 * @SuppressWarnings("unchecked") List<Double>[] listsOut = new ArrayList[2];
		 * 
		 * listsOut[0] = pixelValues; listsOut[1] = donutHoleValues; return listsOut;
		 */
	}

	/**
	 * Returns true if this pixel represents the center of a dark circle, surrounded
	 * by a lighter area.
	 * 
	 * @param radius                    The radius of the dark circle.
	 * @param lightOutsideRingThickness The thickness of the ring of lighter pixels
	 *                                  outside the circle.
	 * @return True if the pixels inside the circle are substantially darker than
	 *         the pixels in the surrounding ring.
	 */
	public boolean isDarkCircle(double radius, double lightOutsideRingThickness, PolygonRoi polygonExclude) {
		return false;
		/*
		 * // We call the circle a hit if the darkest quartile of the exterior is
		 * lighter // than the lightest quartile of the interior. List<Double>[]
		 * innerOuterPixelValues = this.getPixelValuesWithinRadius(radius +
		 * lightOutsideRingThickness, radius, polygonExclude);
		 * 
		 * List<Double> innerPixelValues = innerOuterPixelValues[1];
		 * Collections.sort(innerPixelValues); Collections.reverse(innerPixelValues);
		 * double circleLightestQtile = innerPixelValues.get(innerPixelValues.size() /
		 * 4);
		 * 
		 * List<Double> outerPixelValues = innerOuterPixelValues[0];
		 * Collections.sort(outerPixelValues); double ringDarkestQtile =
		 * outerPixelValues.get(outerPixelValues.size() / 4);
		 * 
		 * if (circleLightestQtile < ringDarkestQtile) { return true; } return false;
		 */
	}

	/**
	 * Tests the pixel's surroundings to see if they qualify for a dark circle of
	 * some size.
	 * 
	 * @param boundaryThickness The perimeter around the dark circle that must be
	 *                          light, in order to determine that the interior is
	 *                          dark.
	 * @param maxRadius         The largest radius to search for a dark circle.
	 * @return The radius of the dark circle centered on this pixel, if such a
	 *         circle exists within maxRadius. Otherwise, returns 0.
	 */
	public double findDarkCircleRadius(double boundaryThickness, double maxRadius, PolygonRoi polygonExclude) {
		for (double radius = boundaryThickness; radius < maxRadius; radius += boundaryThickness / 2) {
			if (isDarkCircle(radius, boundaryThickness, polygonExclude)) {
				return radius;
			}
		}
		return 0.0;
	}

	@Override
	public double getX() {
		return (double) this.x;
	}

	@Override
	public double getY() {
		return (double) this.y;
	}

	@Override
	public void setLocation(double x, double y) {
		this.x = (int) x;
		this.y = (int) y;
	}

	/**
	 * Returns true if this pixel's vicinity is darker than both pixel vicinities
	 * adjacent to it along the line perpendicular to the normal. public boolean
	 * isDarkWithLightOnBothSides( double featureWidth, double penumbraWidth, double
	 * groundWidth, double normalUnitX, double normalUnitY ) { }
	 */

}
