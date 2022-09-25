package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import ij.gui.PolygonRoi;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

public class DendritePixel extends Point2D {

	// Assigning numerical values to enums in Java is stupidly hard.
	// https://stackoverflow.com/questions/8811815/is-it-possible-to-assign-numeric-value-to-an-enum-in-java
	public enum PathSide {
		LEFT(0), RIGHT(1);

		public final static PathSide[] sides() {
			PathSide[] retval = { LEFT, RIGHT };
			return retval;
		}

		private final int value;

		private PathSide(int value) {
			this.value = value;
		}
	}

	public int x;
	public int y;
	public Img<? extends RealType<?>> fromImg;

	double featureWindowSize = 0;
	public Point2D unitNormal = new Point2D.Double(0, 0);
	public double similarityBoundaryDistance[] = { 0, 0 };

	public SummaryStatistics vicinityStats = null;

	public static <T extends RealType<?>> List<DendritePixel> fromTracers(List<? extends Point2D> tracers,
			double featureWindowSize, Img<T> img) {
		if (tracers == null || tracers.size() == 0) {
			return null;
		}

		List<DendritePixel> dpixels = new ArrayList<DendritePixel>();

		Point2D firstTracer = tracers.get(0);
		DendritePixel prevDendritePixel = new DendritePixel((int) firstTracer.getX(), (int) firstTracer.getY(),
				featureWindowSize, img);
		dpixels.add(prevDendritePixel);

		for (Point2D tracer : tracers) {
			double distFromLastDendritePixel = prevDendritePixel.distance(tracer);
			if (distFromLastDendritePixel < featureWindowSize / 2.0) {
				continue;
			}

			prevDendritePixel = new DendritePixel((int) tracer.getX(), (int) tracer.getY(), featureWindowSize, img);
			dpixels.add(prevDendritePixel);
		}

		// We need to add the last pixel. But we already have a dendrite pixel
		// at the end of our list, and it is probably closer than the feature size
		// window from the end. So we lop off the last entry in our list and replace it
		// with the true end, allowing for the last segment to be slightly too long.
		Point2D lastTracer = tracers.get(tracers.size() - 1);
		DendritePixel lastDendritePixel = new DendritePixel((int) lastTracer.getX(), (int) lastTracer.getY(),
				featureWindowSize, img);
		dpixels.remove(dpixels.size() - 1);
		dpixels.add(lastDendritePixel);

		// Calculate normals.
		// This is basically just the vector going from the point before us
		// to the point after us (for each point, with slightly special treatment
		// for the endpoints).
		for (int i = 0; i < dpixels.size(); i++) {
			DendritePixel dpixel = dpixels.get(i);

			int iprev = Math.max(i - 1, 0);
			DendritePixel dpixelPrev = dpixels.get(iprev);

			int inext = Math.min(i + 1, dpixels.size() - 1);
			DendritePixel dpixelNext = dpixels.get(inext);

			Point2D vecThrough = new Point2D.Double(dpixelNext.getX() - dpixelPrev.getX(),
					dpixelNext.getY() - dpixelPrev.getY());
			double mag = vecThrough.distance(0, 0);
			if (mag == 0) {
				// This should be impossible but whatever.
				// In this theoretically impossible situation, assign it a nonsense default unit
				// normal.
				dpixel.unitNormal = new Point2D.Double(1, 0);
			}

			Point2D vecNormal = new Point2D.Double(vecThrough.getX() / mag, vecThrough.getY() / mag);
			dpixel.unitNormal = vecNormal;
		}

		// Search for thickness boundaries. (This requires having an image.)
		if (img != null) {
			for (DendritePixel dpixel : dpixels) {
				dpixel.findSimilarityBoundaries();
			}

			// Smooth out the similarity boundaries.
			for (PathSide side : PathSide.sides()) {
				for (int i = 1; i < dpixels.size() - 2; i++) {
					DendritePixel dpixel = dpixels.get(i);
					DendritePixel dpixelPrev = dpixels.get(i - 1);
					DendritePixel dpixelNext = dpixels.get(i + 1);

					double dist = dpixel.getPixelSidepathDistance(side);
					double distPrev = dpixelPrev.getPixelSidepathDistance(side);
					double distNext = dpixelNext.getPixelSidepathDistance(side);

					double distMean = (distPrev + distNext) / 2;

					double distNew = (distMean * 0.75) + (dist * 0.25);
					dpixel.setPixelSidepathDistance(side, distNew);
				}
			}
		}

		return dpixels;
	}

	public <T extends RealType<?>> DendritePixel(int x, int y, double featureWindowSize, Img<T> img) {
		this.x = x;
		this.y = y;

		this.fromImg = img;

		this.featureWindowSize = featureWindowSize;

		double featureWindowRadius = Math.ceil(featureWindowSize / 2.0);

		// Set default similarity boundary distance. This will be revised later
		// once we compute normals.
		this.similarityBoundaryDistance[PathSide.LEFT.value] = featureWindowRadius;
		this.similarityBoundaryDistance[PathSide.RIGHT.value] = featureWindowRadius;

		if (this.fromImg != null) {
			this.vicinityStats = this.computePixelVicinityStats(0, 0, featureWindowRadius, 0);
		}
	}

	public String key() {
		String str = String.format("(%d, %d) unitnorm=(%f, %f)", this.x, this, y, this.unitNormal.getX(),
				this.unitNormal.getY());
		if (this.vicinityStats != null) {
			str += String.format("vicinity brightness %f±%f", this.vicinityStats.getMean(),
					this.vicinityStats.getStandardDeviation());
		}
		return str;
	}

	@Override
	public String toString() {
		String str = key();
		return str;
	}

	public Point2D getNormalUnitVector(double dist) {
		double x = this.unitNormal.getX() * dist;
		double y = this.unitNormal.getY() * dist;
		return new Point2D.Double(x, y);
	}

	public Point2D getOrthogonalUnitVector(PathSide side, double dist) {
		double orthoRightX = -this.unitNormal.getY() * dist;
		double orthoRightY = this.unitNormal.getX() * dist;

		if (side == PathSide.RIGHT) {
			return new Point2D.Double(orthoRightX, orthoRightY);
		} else if (side == PathSide.LEFT) {
			return new Point2D.Double(-orthoRightX, -orthoRightY);
		} else {
			throw new IllegalArgumentException("Unrecognized PathSide value: " + side);
		}
	}

	public Point2D getOrthogonalUnitPoint(PathSide side, double dist) {
		Point2D ortho = this.getOrthogonalUnitVector(side, dist);
		Point2D pt = new Point2D.Double(this.getX() + ortho.getX(), this.getY() + ortho.getY());
		return pt;
	}

	public double getPixelSidepathDistance(PathSide side) {
		return this.similarityBoundaryDistance[side.value];
	}

	public void setPixelSidepathDistance(PathSide side, double distance) {
		this.similarityBoundaryDistance[side.value] = distance;
	}

	public Point2D getPixelSidepathVector(PathSide side) {
		double dist = this.similarityBoundaryDistance[side.value];
		return this.getOrthogonalUnitVector(side, dist);
	}

	public Point2D getPixelSidepathPoint(PathSide side) {
		double dist = this.similarityBoundaryDistance[side.value];
		return this.getOrthogonalUnitPoint(side, dist);
	}

	/**
	 * Returns a point whose coordinates mark the "similarity boundary" in a given
	 * direction. All the pixel values between the returned pixel and the "this"
	 * pixel are "similar" to the "this" pixel. All the pixel values beyond the
	 * similarity boundary point are "different" from "this". Must be called after
	 * orthogonal and tangential vectors have already been set.
	 * 
	 * @param side Determines if we're looking at the left-hand boundary (where
	 *             direction is determined by the path set by DendritePixelPath) or
	 *             the right-hand one.
	 * @return A point that marks the edge of the similarity boundary.
	 */
	public Point2D getSimilarityBoundaryPoint(PathSide side) {
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
	public void findSimilarityBoundaries() {
		this.similarityBoundaryDistance[PathSide.LEFT.value] = findSimilarityBoundaryOnSide(PathSide.LEFT);
		this.similarityBoundaryDistance[PathSide.RIGHT.value] = findSimilarityBoundaryOnSide(PathSide.RIGHT);
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
	 * @param side           Which side to search on.
	 * @param distanceMax    The maximum distance to search out to.
	 * @return The distance, in the direction of the orthogonal vector, at which
	 *         pixels start to look "different".
	 */
	public double findSimilarityBoundaryOnSide(PathSide side) {
		double distanceMax = this.featureWindowSize * 4;
		double vicinityRadius = this.featureWindowSize / 2;

		// System.out.println(String.format("local vicinity stats: %f±%f(n=%d)",
		// vicinityStats.getMean(),
		// vicinityStats.getStandardDeviation(), vicinityStats.getN()));

		for (double dist = 0; dist < distanceMax; dist++) {
			SummaryStatistics testpointVicinityStats = computePixelVicinityStatsInDirection(side, dist, 0,
					vicinityRadius, 0);

			// System.out.println(String.format(" distance out: %f. Stats: %f±%f(n=%d)",
			// dist, testpointVicinityStats.getMean(),
			// testpointVicinityStats.getStandardDeviation(),
			// testpointVicinityStats.getN()));

			boolean isDifferent = areStatsDifferent(testpointVicinityStats, this.vicinityStats, 1.25);
			if (isDifferent == true) {
				return dist;
			}
		}
		return distanceMax;
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
	 * Gets a statistical description of all pixels in a circle centered on a
	 * certain offset from this DendritePixel. The offset is measured using a basis
	 * vector oriented orthogonally to the DendritePixel, so instead of specifying X
	 * and Y offsets you specify units of distance to go in the normal and
	 * orthogonal directions.
	 * 
	 * @param side            Which side to get orthogonal distance on.
	 * @param distOrtho       How many pixels away in the orthogonal direction.
	 * @param distNorm        How many pixels "forward" in the normal direction.
	 * @param radius          Radius of the vicinity circle.
	 * @param donutHoleRadius Radius of a "donut hole" representing pixels to ignore
	 *                        in the middle of the vicinity circle.
	 * @return A statistical description of the pixels within the circle.
	 */
	public SummaryStatistics computePixelVicinityStatsInDirection(PathSide side, double distOrtho, double distNorm,
			double radius, double donutHoleRadius) {
		Point2D vecOrtho = this.getOrthogonalUnitVector(side, distOrtho);
		Point2D vecNorm = this.getNormalUnitVector(distNorm);
		double xoff = vecOrtho.getX() + vecNorm.getX();
		double yoff = vecOrtho.getY() + vecNorm.getY();

		SummaryStatistics stat = this.computePixelVicinityStats((int) xoff, (int) yoff, radius, donutHoleRadius);
		return stat;
	}

	/**
	 * Gets a statistical description of all pixels in a circle centered on this
	 * pixel (or an offset point).
	 * 
	 * @param xoff            Offset from this DendritePixel's X position to center
	 *                        the search on.
	 * @param yoff            Offset from this DendritePixel's Y position to center
	 *                        the search on.
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @return A statistical description of the pixels within the circle.
	 */
	public SummaryStatistics computePixelVicinityStats(int xoff, int yoff, double radius, double donutHoleRadius) {
		List<java.lang.Double> values = getPixelValuesWithinRadius(xoff, yoff, radius, donutHoleRadius, null);

		SummaryStatistics stat = new SummaryStatistics();
		for (java.lang.Double value : values) {
			stat.addValue(value);
		}
		return stat;
	}

	/**
	 * Gets a list of values (normalized to a range of 0 to 1) of all pixels in a
	 * circle centered on this pixel (or an offset point).
	 * 
	 * @param xoff            Offset from this DendritePixel's X position to center
	 *                        the search on.
	 * @param yoff            Offset from this DendritePixel's Y position to center
	 *                        the search on.
	 * @param radius          The radius of the circle to collect pixel values from.
	 * @param donutHoleRadius A radius of pixels to exclude from the returned
	 *                        collection.
	 * @param polygonExclude  If provided, the search refrains from examining pixels
	 *                        that are within this region of exclusion.
	 * @return A list of pixel values between 0 and 1, where 0 is black and 1 is
	 *         white, representing values outside the donut hole radius.
	 */
	public List<java.lang.Double> getPixelValuesWithinRadius(int xoff, int yoff, double radius, double donutHoleRadius,
			PolygonRoi polygonExclude) {

		if (this.fromImg == null) {
			throw new IllegalArgumentException(
					"Can't get pixel values in vicinity of DendritePixel because DendritePixel has no fromImg.");
		}

		List<java.lang.Double> pixelValues = new ArrayList<java.lang.Double>();
		List<java.lang.Double> donutHoleValues = new ArrayList<java.lang.Double>();
		// NOTE: We don't actually return donutHoleValues currently.

		long imgWidth = this.fromImg.dimension(0);
		long imgHeight = this.fromImg.dimension(1);

		int xcenter = this.x + xoff;
		int ycenter = this.y + yoff;

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

		final RandomAccess<? extends RealType<?>> r = this.fromImg.randomAccess();

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
		 * lighter // than the lightest quartile of the interior.
		 * List<java.lang.Double>[] innerOuterPixelValues =
		 * this.getPixelValuesWithinRadius(radius + lightOutsideRingThickness, radius,
		 * polygonExclude);
		 * 
		 * List<java.lang.Double> innerPixelValues = innerOuterPixelValues[1];
		 * Collections.sort(innerPixelValues); Collections.reverse(innerPixelValues);
		 * double circleLightestQtile = innerPixelValues.get(innerPixelValues.size() /
		 * 4);
		 * 
		 * List<java.lang.Double> outerPixelValues = innerOuterPixelValues[0];
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
