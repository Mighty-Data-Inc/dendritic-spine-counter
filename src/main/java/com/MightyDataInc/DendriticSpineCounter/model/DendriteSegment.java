package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class DendriteSegment {
	public ArrayList<SearchPixel> path = new ArrayList<SearchPixel>();

	public double minimumSeparation = 1.0;

	// The Roi object that is drawn on the overlay and associated with this path.
	// Assigned when this path is added to the overlay.
	public Roi roi;

	// The ID gets assigned at the time at which it's added to the overlay.
	public int id = 0;

	// The branch's user-provided name. Overrides the default name, if supplied.
	public String name = "";

	// Additional info provided by the system, which appears in the dendrite
	// branch's
	// displayed name. At the time of this writing, this is just the length of the
	// path.
	public String nameSuffix;

	// Spines associated with this search pixel path.
	public List<Point2D> spines = new ArrayList<Point2D>();

	public DendriteSegment() {
	}

	/**
	 * Searches for the best sequence of pixels to go from the start position to the
	 * end position.
	 * 
	 * @param xStart            Start position's x coordinate
	 * @param yStart            Start position's y coordinate
	 * @param xEnd              End position's x coordinate
	 * @param yEnd              End position's y coordinate
	 * @param workingImg        The image to search within. Must be a grayscale
	 *                          image.
	 * @param totalSearchVolume Optional (can be null). The total collection of all
	 *                          pixels visited during the search process. Useful for
	 *                          gauging search efficiency.
	 * @return A sequence of pixels traveling from the start to the end positions,
	 *         obeying the constraints of the search parameters laid out in the
	 *         WEIGHT calibrations.
	 */
	public static DendriteSegment fromLine(int xStart, int yStart, int xEnd, int yEnd, Img<UnsignedByteType> workingImg,
			List<SearchPixel> totalSearchVolume) {
		PriorityQueue<SearchPixel> searchFrontier = new PriorityQueue<SearchPixel>();
		HashMap<String, SearchPixel> alreadyVisited = new HashMap<String, SearchPixel>();

		SearchPixel startPixel = new SearchPixel();
		startPixel.x = xStart;
		startPixel.y = yStart;
		startPixel.fromImg = workingImg;
		searchFrontier.add(startPixel);
		alreadyVisited.put(startPixel.key(), startPixel);

		SearchPixel pixel;
		SearchPixel destinationPixel = null;
		while ((pixel = searchFrontier.poll()) != null && destinationPixel == null) {
			PriorityQueue<SearchPixel> nextToSearch = pixel.neighbors();

			for (SearchPixel nextPixel : nextToSearch) {
				if (nextPixel.x == xEnd && nextPixel.y == yEnd) {
					destinationPixel = nextPixel;
					break;
				}
				if (alreadyVisited.containsKey(nextPixel.key())) {
					continue;
				}

				double distance = Math.hypot(nextPixel.x - xEnd, nextPixel.y - yEnd);
				nextPixel.setDistanceToDestination(distance);

				searchFrontier.add(nextPixel);
				alreadyVisited.put(nextPixel.key(), nextPixel);
			}
		}

		ArrayList<SearchPixel> backpath = new ArrayList<SearchPixel>();
		SearchPixel backpathPixel = destinationPixel;

		while (backpathPixel != null) {
			backpath.add(backpathPixel);
			backpathPixel = backpathPixel.fromPixel;
		}
		// Backpath is no longer a backpath after being reversed, it is now a frontpath
		Collections.reverse(backpath);

		if (totalSearchVolume != null) {
			totalSearchVolume.addAll(alreadyVisited.values());
		}

		DendriteSegment pathResult = new DendriteSegment();
		pathResult.path = backpath;

		return pathResult;
	}

	/**
	 * 
	 * @param pathPoints        A list of of points for the method to search over
	 * @param workingImg        The image to search within. Must be a grayscale
	 *                          image.
	 * @param totalSearchVolume Optional (can be null). The total collection of all
	 *                          pixels visited during the search process. Useful for
	 *                          gauging search efficiency.
	 * @return A sequence of pixels traveling from the start to the end positions,
	 *         obeying the constraints of the search parameters laid out in the
	 *         WEIGHT calibrations.
	 */
	public static DendriteSegment searchPolyline(List<Point2D> pathPoints, Img<UnsignedByteType> workingImg,
			List<SearchPixel> totalSearchVolume) {
		Point2D previousPoint = null;
		DendriteSegment finalPath = new DendriteSegment();

		for (Point2D pathPoint : pathPoints) {
			if (previousPoint == null) {
				previousPoint = pathPoint;
				continue;
			}
			DendriteSegment nextSegment = DendriteSegment.fromLine((int) previousPoint.getX(),
					(int) previousPoint.getY(), (int) pathPoint.getX(), (int) pathPoint.getY(), workingImg,
					totalSearchVolume);

			finalPath.path.addAll(nextSegment.path);
			previousPoint = pathPoint;
		}

		return finalPath;
	}

	/**
	 * "Cleans up" this search result path by making sure that each pixel is no
	 * closer than some minimum distance (in pixels) than the previous one in the
	 * list. Also adds interpolated points to make sure that the separation between
	 * consecutive points isn't too big, either. After running this method, every
	 * point will be between minSeparation and 2*minSeparation apart from the points
	 * adjacent to it.
	 * 
	 * @param minSeparation The closest distance, in pixels, that consecutive pixels
	 *                      are permitted to be.
	 * @return this, for chaining.
	 */
	public DendriteSegment setMinimumSeparation(double minSeparation) {
		ArrayList<SearchPixel> pixelListOut = new ArrayList<SearchPixel>();
		SearchPixel prevPixel = null;

		for (SearchPixel pixel : this.path) {
			if (prevPixel == null) {
				prevPixel = pixel;
				pixelListOut.add(pixel);
				continue;
			}
			double distToPrevPoint = Math.hypot(pixel.x - prevPixel.x, pixel.y - prevPixel.y);
			if (distToPrevPoint < minSeparation) {
				continue;
			}

			// Perform interpolation to make sure that the separation
			// between consecutive pixels isn't TOO big.
			int numPixelsFitBetween = (int) Math.floor(distToPrevPoint / minSeparation) - 1;
			for (int iInterpixel = 0; iInterpixel < numPixelsFitBetween; iInterpixel++) {
				double positionfrac = (double) (iInterpixel + 1) / (double) (numPixelsFitBetween + 1);

				double interX = (positionfrac * pixel.x) + ((1.0 - positionfrac) * prevPixel.x);
				double interY = (positionfrac * pixel.y) + ((1.0 - positionfrac) * prevPixel.y);

				SearchPixel interpixel = new SearchPixel(pixel.fromImg, (int) Math.floor(interX),
						(int) Math.floor(interY));
				pixelListOut.add(interpixel);
			}

			pixelListOut.add(pixel);

			prevPixel = pixel;
		}
		// The last point in the original points list needs a bit of special handling.
		// It is probably too close to the final point, so we'll remove the final point
		// and extend the last segment to the end of the list.
		SearchPixel lastInputPixel = this.path.get(this.path.size() - 1);
		pixelListOut.remove(pixelListOut.size() - 1);
		pixelListOut.add(lastInputPixel);

		this.path = pixelListOut;
		this.minimumSeparation = minSeparation;

		return this;
	}

	/**
	 * "Cleans up" the path by trying to make the path less jagged. It does so by
	 * nudging each point toward the centerpoint of its two adjacent points. Works
	 * best when run after setMinimumSeparation.
	 * 
	 * @param nudgeStrength A value between 0 and 1 indicating how much to nudge
	 *                      each point by. 0 means don't nudge; you might as well
	 *                      not run this method at all. 1 means nudge it all the way
	 *                      to the midpoint.
	 */
	public void smoothify(double nudgeStrength) {
		for (int i = 1; i < this.path.size() - 2; i++) {
			SearchPixel pixelBefore = this.path.get(i - 1);
			SearchPixel pixelAfter = this.path.get(i + 1);
			SearchPixel pixel = this.path.get(i);

			double meanX = ((double) (pixelBefore.x) + (double) (pixelAfter.x)) / 2.0;
			double meanY = ((double) (pixelBefore.y) + (double) (pixelAfter.y)) / 2.0;

			pixel.x = (int) ((meanX * nudgeStrength) + ((double) (pixel.x) * (1.0 - nudgeStrength)));
			pixel.y = (int) ((meanY * nudgeStrength) + ((double) (pixel.y) * (1.0 - nudgeStrength)));
		}
	}

	/**
	 * Makes the entire segment thicker or thinner.
	 * 
	 * @param thicknessMultiplier The amount by which to multiply the thickness.
	 */
	public void multiplyThickness(double thicknessMultiplier) {
		for (SearchPixel px : this.path) {
			px.similarityBoundaryDistanceLeft *= thicknessMultiplier;
			px.similarityBoundaryDistanceRight *= thicknessMultiplier;
		}
	}

	/**
	 * Iterate through the points in the path and assign them approximate orthogonal
	 * and tangent vectors. The tangents are determined by the ray cast from the
	 * point before the current point in the sequence, through the subsequent point
	 * (with special handling for the first and last points). The orthogonals are
	 * just 90deg rotations of the tangents.
	 * 
	 * @return this, for chaining.
	 */
	public DendriteSegment computeTangentsAndOrthogonals() {
		// We use "old-fashioned" index-based iteration because we are using
		// an ArrayList anyway, and we want to be able to easily look both
		// one pixel ahead and one pixel behind, if they're there.
		for (int i = 0; i < this.path.size(); i++) {
			SearchPixel pixelCurrent = this.path.get(i);
			SearchPixel pixelPrevious = (i == 0) ? pixelCurrent : this.path.get(i - 1);
			SearchPixel pixelNext = (i == this.path.size() - 1) ? pixelCurrent : this.path.get(i + 1);

			Point2D rayFromPrevToNext = new Point2D.Double(pixelNext.x - pixelPrevious.x,
					pixelNext.y - pixelPrevious.y);

			double magnitude = rayFromPrevToNext.distance(0, 0);

			Point2D unitTangent = new Point2D.Double(rayFromPrevToNext.getX() / magnitude,
					rayFromPrevToNext.getY() / magnitude);

			// The orthogonal vector is easy to compute from the tangent vector.
			// Look at the 2D rotation matrix:
			// https://en.wikipedia.org/wiki/Rotation_matrix
			// When theta is 90deg, the result of the matrix degenerates to just:
			// [x, y] * RotationMatrix = [-y, x]
			pixelCurrent.unitOrthogonal = new Point2D.Double(-unitTangent.getY(), unitTangent.getX());
		}
		return this;
	}

	/**
	 * Populates each path pixel's left and right similarity boundaries by checking
	 * where the pixels on the image start being "different". This is a
	 * computationally costly method, so it should only be run after the path's
	 * setMinimumSeparation method has been called.
	 * 
	 * @param vicinityRadius The size of the area to test for similarity.
	 * @param distanceMax    The maximum distance to search out to.
	 */
	public void findSimilarityBoundaries(double vicinityRadius, double distanceMax) {
		for (SearchPixel pixel : this.path) {
			pixel.findSimilarityBoundaries(vicinityRadius, distanceMax);
		}
	}

	public DendriteSegment createSidePath(SearchPixel.PathSide side) {
		return createSidePath(side, 1.0);
	}

	/**
	 * Create a path that follows the specified side of this search path, on the
	 * outside by outerDistance pixels. Sets the sidepath's separation distance to
	 * be the same as this's.
	 * 
	 * @param side          Which side of the path to follow.
	 * @param outerDistance How far past the similarity boundary to create the new
	 *                      side path.
	 * @return A new path that follows outside the designated side.
	 */
	public DendriteSegment createSidePath(SearchPixel.PathSide side, double outerDistance) {
		return createSidePath(side, outerDistance, this.minimumSeparation);
	}

	/**
	 * Create a path that follows the specified side of this search path, on the
	 * outside by outerDistance pixels.
	 * 
	 * @param side              Which side of the path to follow.
	 * @param outerDistance     How far past the similarity boundary to create the
	 *                          new side path.
	 * @param minimumSeparation The minimum separation to set for the new sidepath.
	 * @return A new path that follows outside the designated side.
	 */
	public DendriteSegment createSidePath(SearchPixel.PathSide side, double outerDistance, double minimumSeparation) {
		ArrayList<SearchPixel> sidepath = new ArrayList<SearchPixel>();
		for (SearchPixel pixel : this.path) {
			SearchPixel pxBoundary = pixel.getSimilarityBoundaryPoint(side, outerDistance);
			sidepath.add(pxBoundary);
		}

		if (side == SearchPixel.PathSide.RIGHT) {
			// Order the path so that the dendrite mass is always
			// to the left of the path.
			Collections.reverse(sidepath);
		}

		DendriteSegment pathObjOut = new DendriteSegment();
		pathObjOut.path = sidepath;

		pathObjOut.setMinimumSeparation(minimumSeparation);
		pathObjOut.smoothify(0.5);
		pathObjOut.computeTangentsAndOrthogonals();

		return pathObjOut;
	}

	/**
	 * Converts the pixels in this search path to an equivalent list of Point2D
	 * objects.
	 * 
	 * @return A Point2D path
	 */
	public List<Point2D> toPoint2DList() {
		ArrayList<Point2D> points = new ArrayList<Point2D>();
		for (SearchPixel pixel : this.path) {
			Point2D point = new Point2D.Double(pixel.x, pixel.y);
			points.add(point);
		}
		return points;
	}

	/**
	 * Converts the pixels in a search path to an equivalent polyline type
	 * PolygonRoi.
	 * 
	 * @return A legacy polyline.
	 */
	public static PolygonRoi toLegacyPolylineRoi(Collection<SearchPixel> path) {
		float[] xPoints = new float[path.size()];
		float[] yPoints = new float[path.size()];

		int i = 0;
		for (SearchPixel searchPixel : path) {
			xPoints[i] = searchPixel.x;
			yPoints[i] = searchPixel.y;
			i++;
		}

		PolygonRoi roi = new PolygonRoi(xPoints, yPoints, Roi.POLYLINE);
		return roi;
	}

	/**
	 * Converts the pixels in this search path to an equivalent polyline type
	 * PolygonRoi.
	 * 
	 * @return A legacy polyline.
	 */
	public PolygonRoi toLegacyPolylineRoi() {
		return toLegacyPolylineRoi(this.path);
	}

	/**
	 * Must be called after findSimilarityBoundaries, and preferably after
	 * smoothSimilarityBoundaryDistances. Takes the resulting points on the left and
	 * right sides of the similarity boundaries and creates a closed polygon
	 * overlay.
	 * 
	 * @return A legacy polygon Roi that can be added to the image or the
	 *         RoiManager.
	 */
	public PolygonRoi getSimilarityVolume() {
		ArrayList<SearchPixel> enclosedPath = new ArrayList<SearchPixel>();
		enclosedPath.addAll(createSidePath(SearchPixel.PathSide.RIGHT).path);
		enclosedPath.addAll(createSidePath(SearchPixel.PathSide.LEFT).path);

		float[] xPoints = new float[enclosedPath.size()];
		float[] yPoints = new float[enclosedPath.size()];

		for (int i = 0; i < enclosedPath.size(); i++) {
			SearchPixel searchPixel = enclosedPath.get(i);
			xPoints[i] = searchPixel.x;
			yPoints[i] = searchPixel.y;
		}

		PolygonRoi roi = new PolygonRoi(xPoints, yPoints, Roi.POLYGON);
		return roi;
	}

	/**
	 * Removes jags and outliers from a side path. Needs to be called after
	 * similarity boundaries have already been established. Works best when the path
	 * itself has also been smoothed.
	 * 
	 * @param side          Which side to smooth.
	 * @param nudgeStrength A value between 0 and 1 indicating how much to nudge
	 *                      each point by. 0 means don't nudge; you might as well
	 *                      not run this method at all. 1 means nudge it all the way
	 *                      to the midpoint.
	 */
	public void smoothSimilarityBoundaryDistances(SearchPixel.PathSide side, double nudgeStrength) {
		for (int i = 1; i < this.path.size() - 2; i++) {
			SearchPixel pixelBefore = this.path.get(i - 1);
			SearchPixel pixelAfter = this.path.get(i + 1);
			SearchPixel pixel = this.path.get(i);

			double distBefore = pixelBefore.getPixelSidepathDistance(side);
			double distAfter = pixelAfter.getPixelSidepathDistance(side);
			double distMean = (distBefore + distAfter) / 2.0;

			double dist = pixel.getPixelSidepathDistance(side);
			dist = (distMean * nudgeStrength) + (dist * (1.0 - nudgeStrength));
			pixel.setPixelSidepathDistance(side, dist);
		}
	}

	/**
	 * Removes jags and outliers from both side paths. Needs to be called after
	 * similarity boundaries have already been established.
	 * 
	 * @param nudgeStrength A value between 0 and 1 indicating how much to nudge
	 *                      each point by. 0 means don't nudge; you might as well
	 *                      not run this method at all. 1 means nudge it all the way
	 *                      to the midpoint.
	 */
	public void smoothSimilarityBoundaryDistances(double nudgeStrength) {
		smoothSimilarityBoundaryDistances(SearchPixel.PathSide.LEFT, nudgeStrength);
		smoothSimilarityBoundaryDistances(SearchPixel.PathSide.RIGHT, nudgeStrength);
	}

	/**
	 * Finds the pixel on the search path that's nearest to the specified point.
	 * 
	 * @param x The X coordinate of the point to find the nearest pixel to.
	 * @param y The Y coordinate of the point to find the nearest pixel to.
	 * @return The pixel on the search path that's nearest to the specified point
	 *         (x, y).
	 */
	public SearchPixel findNearestPixel(double x, double y) {
		SearchPixel nearest = null;
		double nearestDist = Double.MAX_VALUE;
		for (SearchPixel pixel : this.path) {
			double dist = Math.hypot(pixel.x - x, pixel.y - y);
			if (nearest == null || dist < nearestDist) {
				nearest = pixel;
				nearestDist = dist;
			}
		}
		return nearest;
	}

	/**
	 * Finds the pixel on the search path that's nearest to the specified point.
	 * 
	 * @param testpixel The pixel to find the nearest path pixel to.
	 * @return The pixel on the search path that's nearest to the specified point
	 *         (x, y).
	 */
	public SearchPixel findNearestPixel(SearchPixel testpixel) {
		return findNearestPixel(testpixel.x, testpixel.y);
	}

	public List<SearchPixel> getDarkestSurroundingPixels(double maxDistance, double ntile) {
		HashMap<String, SearchPixel> pixelsCollected = new HashMap<String, SearchPixel>();

		for (SearchPixel.PathSide side : SearchPixel.PathSide.values()) {
			for (double distance = 1; distance < maxDistance; distance++) {
				DendriteSegment outsidePath = this.createSidePath(side, distance);
				outsidePath.setMinimumSeparation(2);
				for (SearchPixel pixel : outsidePath.path) {
					pixelsCollected.put(pixel.key(), pixel);
				}
			}
		}

		ArrayList<Double> pixelBrightnesses = new ArrayList<Double>();
		for (SearchPixel pixel : pixelsCollected.values()) {
			pixelBrightnesses.add(pixel.brightness);
		}
		Collections.sort(pixelBrightnesses);
		int ntileCutoff = (int) ((double) pixelBrightnesses.size() * ntile);
		double brightnessCutoff = pixelBrightnesses.get(ntileCutoff);

		ArrayList<SearchPixel> pixelsDarkest = new ArrayList<SearchPixel>();
		for (SearchPixel pixel : pixelsCollected.values()) {
			if (pixel.brightness <= brightnessCutoff) {
				pixelsDarkest.add(pixel);
			}
		}

		return pixelsDarkest;
	}

	public List<SearchPixel> findDarkIslands(double islandRadius) {
		final double BRIGHTNESS_DIFFERENCE_THRESHOLD = 0.5;

		List<SearchPixel> islands = new ArrayList<SearchPixel>();

		for (SearchPixel pixel : this.path) {
			pixel.vicinityStats = pixel.computePixelVicinityStats(islandRadius);
		}

		int pixelIndexIslandRadiusOffset = (int) Math.ceil(islandRadius / minimumSeparation);

		for (int iPixel = pixelIndexIslandRadiusOffset; iPixel < this.path.size()
				- pixelIndexIslandRadiusOffset; iPixel++) {
			// We consider the pixel a dark island if a light representative
			// of its vicinity is darker than a dark representative both of
			// its adjacent pixel neighbors.
			SearchPixel pixel = this.path.get(iPixel);
			SearchPixel pixelBefore = this.path.get(iPixel - pixelIndexIslandRadiusOffset);
			SearchPixel pixelAfter = this.path.get(iPixel + pixelIndexIslandRadiusOffset);

			double lightestInside = pixel.vicinityStats.getMean()
					+ pixel.vicinityStats.getStandardDeviation() * BRIGHTNESS_DIFFERENCE_THRESHOLD;
			double darkestBefore = pixelBefore.vicinityStats.getMean()
					- pixelBefore.vicinityStats.getStandardDeviation() * BRIGHTNESS_DIFFERENCE_THRESHOLD;
			double darkestAfter = pixelAfter.vicinityStats.getMean()
					- pixelAfter.vicinityStats.getStandardDeviation() * BRIGHTNESS_DIFFERENCE_THRESHOLD;

			if (lightestInside < darkestBefore && lightestInside < darkestAfter) {
				islands.add(pixel);
			}
		}

		return islands;
	}

	/**
	 * Searches for all sequences of pixels along the sides of the path such that
	 * the pixels are dark enough to be (probably) part of a dendritic structure
	 * rather than the background.
	 * 
	 * @param featureSize A size filter to apply to the sequences of pixels found.
	 *                    Will merge sequences of pixels that are within this
	 *                    separation distance. Will not return pixel sequences
	 *                    smaller than this size.
	 * @return
	 */
	public ArrayList<ArrayList<SearchPixel>> findContinuityChains(int featureSize) {
		final double BRIGHTNESS_THRESHOLD = 0.25;

		ArrayList<ArrayList<SearchPixel>> allContinuityChains = new ArrayList<ArrayList<SearchPixel>>();

		java.util.function.Consumer<ArrayList<SearchPixel>> emitContinuityChain = (continuityChain) -> {
			if (continuityChain == null || continuityChain.size() == 0) {
				return;
			}
			if (allContinuityChains.size() == 0) {
				allContinuityChains.add(continuityChain);
				return;
			}

			ArrayList<SearchPixel> lastChain = allContinuityChains.get(allContinuityChains.size() - 1);
			SearchPixel lastPixelOfLastChain = lastChain.get(lastChain.size() - 1);
			SearchPixel firstPixelOfThisChain = continuityChain.get(0);

			double dist = lastPixelOfLastChain.distanceFrom(firstPixelOfThisChain);
			if (dist < featureSize) {
				lastChain.addAll(continuityChain);
			} else {
				allContinuityChains.add(continuityChain);
			}
		};

		for (SearchPixel.PathSide side : SearchPixel.PathSide.values()) {
			DendriteSegment sidepath = this.createSidePath(side, featureSize);
			sidepath.setMinimumSeparation(1);

			ArrayList<SearchPixel> continuityChain = null;

			for (SearchPixel sidepixel : sidepath.path) {
				SearchPixel nearestPathPixel = this.findNearestPixel(sidepixel);

				// "Too bright to be part of the dendrite" will be defined as
				// some percent of the way from the dendrite vicinity's mean to max brightness.
				double pathVicinityBrightnessThreshold = BRIGHTNESS_THRESHOLD
						+ (nearestPathPixel.vicinityStats.getMean() * (1.0 - BRIGHTNESS_THRESHOLD));

				if (sidepixel.brightness >= pathVicinityBrightnessThreshold) {
					// This sidepixel is not a dark pixel. It is a breaker of chains.
					emitContinuityChain.accept(continuityChain);
					continuityChain = null;
					continue;
				}

				// This sidepixel is darker than the dendrite. It's probably part of
				// an overhanging mass of some kind.
				if (continuityChain == null) {
					continuityChain = new ArrayList<SearchPixel>();
				}
				continuityChain.add(sidepixel);
			}
			emitContinuityChain.accept(continuityChain);
		}

		ArrayList<ArrayList<SearchPixel>> chainsToKeep = new ArrayList<ArrayList<SearchPixel>>();
		for (ArrayList<SearchPixel> chain : allContinuityChains) {
			if (chain.size() <= 2) {
				// This chain is degenerate!
				continue;
			}

			SearchPixel chainPixelFirst = chain.get(0);
			SearchPixel chainPixelLast = chain.get(chain.size() - 1);
			double chainImageSize = chainPixelFirst.distanceFrom(chainPixelLast);
			if (chainImageSize < featureSize) {
				// This chain represents too small of an image area!
				continue;
			}

			chainsToKeep.add(chain);
		}

		return chainsToKeep;
	}

	/**
	 * @param featureSize
	 * @return
	 */
	public List<EllipseRoi> findSpines_NEW(int featureSize, ImagePlus imp) {
		final double CONTRAST_SENSITIVITY = .75;

		ArrayList<EllipseRoi> rois = new ArrayList<EllipseRoi>();

		for (SearchPixel pixel : this.path) {
			for (SearchPixel.PathSide side : SearchPixel.PathSide.values()) {
				// We want to find dark pixels outside the dendrite's boundaries.
				// What does "dark" mean? Let's define a threshold here.
				// To do that, we grab a pixel that's WAY outside the dendrite,
				// get a very large vicinity around it, and define that as the background.
				// This is not a perfect plan, because for all we know we could land
				// squarely inside another dendrite. But it's a reasonable assumption
				// for most images.
				SearchPixel backgroundPixel = pixel.getSimilarityBoundaryPoint(side, featureSize * 5);
				backgroundPixel.computePixelVicinityStats(featureSize * 2);

				System.out.println(String.format("Pixel brightness: %.3f. Background brightness: %.3f",
						pixel.vicinityStats.getMean(), backgroundPixel.vicinityStats.getMean()));

				// Define it as darker than the mean brightness within the
				double mustBeDarkerThan = (backgroundPixel.vicinityStats.getMean() * CONTRAST_SENSITIVITY)
						+ (pixel.vicinityStats.getMean() * (1.0 - CONTRAST_SENSITIVITY));

				int numOutsideDarkEnough = 0;
				SearchPixel farthestDarkPixel = null;

				// We want to find features that extend nontrivially from the surface of the
				// dendrite. As such, we need to follow the darkness outward.
				for (int outsideness = 1; outsideness <= 5; outsideness++) {
					double outsideDistance = (featureSize * outsideness) / 2;

					SearchPixel pxOutsideTest = pixel.getSimilarityBoundaryPoint(side, outsideDistance);

					pxOutsideTest.computePixelVicinityStats(featureSize);
					if (pxOutsideTest.vicinityStats.getMean() >= mustBeDarkerThan) {
						// We're not dark enough.
						continue;
					}

					// If we got here, then the outside pixel *is* dark enough.
					numOutsideDarkEnough++;
					farthestDarkPixel = pxOutsideTest;
				}

				// Was the outside darkness just noise? Or was there enough to confirm
				// that there's probably a feature there?
				if (numOutsideDarkEnough < 3) {
					continue;
				}

				// There was apparently a feature there.

				EllipseRoi roi = new EllipseRoi(farthestDarkPixel.x - featureSize / 2,
						farthestDarkPixel.y - featureSize / 2, farthestDarkPixel.x + featureSize / 2,
						farthestDarkPixel.y + featureSize / 2, 1.0);

				if (imp != null) {
					roi.setImage(imp);
					imp.setRoi(roi, true);
				}

				rois.add(roi);
			}
		}

		return rois;
	}

	/**
	 * @param featureSize
	 * @return
	 */
	public List<EllipseRoi> findSpines(int featureSize, ImagePlus imp, RoiManager roiManager) {
		ArrayList<EllipseRoi> rois = new ArrayList<EllipseRoi>();

		ArrayList<ArrayList<SearchPixel>> allContinuityChains = findContinuityChains(featureSize);

		for (ArrayList<SearchPixel> chain : allContinuityChains) {
			SearchPixel chainPixelFirst = chain.get(0);
			SearchPixel chainPixelLast = chain.get(chain.size() - 1);
			double spineSize = chainPixelFirst.distanceFrom(chainPixelLast);

			// Designate a spot as the center of the spine.
			SearchPixel chainPixelMiddle = chain.get((int) (chain.size() / 2));
			SearchPixel closestPathPixel = this.findNearestPixel(chainPixelMiddle);
			double distToClosest = closestPathPixel.distanceFrom(chainPixelMiddle);
			double unitDistX = ((double) chainPixelMiddle.x - closestPathPixel.x) / distToClosest;
			double unitDistY = ((double) chainPixelMiddle.y - closestPathPixel.y) / distToClosest;
			double distSizeEstimated = spineSize / 2;
			int centerEstX = chainPixelMiddle.x + (int) (unitDistX * distSizeEstimated);
			int centerEstY = chainPixelMiddle.y + (int) (unitDistY * distSizeEstimated);

			String chainName = String.format("Spine of size %.2f at (%d, %d)", spineSize, centerEstX, centerEstY);

			EllipseRoi spineRoi = new EllipseRoi(centerEstX - (spineSize / 2), centerEstY - (spineSize / 2),
					centerEstX + (spineSize / 2), centerEstY + (spineSize / 2), 1.0);

			// ImagePlus imp = WindowManager.getCurrentImage();
			spineRoi.setName(chainName);
			spineRoi.setImage(imp);
			imp.setRoi(spineRoi, true);

			rois.add(spineRoi);

			if (roiManager != null) {
				roiManager.addRoi(spineRoi);
			}
		}

		if (roiManager != null) {
			roiManager.moveRoisToOverlay(imp);
		}

		return rois;
	}

	public double pixelLength() {
		return this.path.size() * this.minimumSeparation;
	}

	public double MeanPixelWidth() {
		double widthTotal = 0;
		for (SearchPixel px : this.path) {
			widthTotal += Math.abs(px.similarityBoundaryDistanceRight) + Math.abs(px.similarityBoundaryDistanceLeft);
		}
		widthTotal /= path.size();
		return widthTotal;
	}

	/**
	 * Intended to be run on a DendriteSegment that is a sidepath. Looks for dark
	 * convolution windows, where "dark" is defined relative to adjacent convolution
	 * windows -- a sort of meta-convolution, if you will.
	 * 
	 * @param pixelWindowSize     The size, expressed as a pixel radius, of the
	 *                            convolution window.
	 * @param scanspan            How many convolution windows on each side to
	 *                            compare at a time. A sort of meta convolution
	 *                            window size.
	 * @param contrastSensitivity How much contrast there needs to be between the
	 *                            convolution window and its neighbors. A value of 0
	 *                            means that no contrast is required; a window
	 *                            merely needs to be darker than its neighbors by
	 *                            any amount. A value of 1.0 means that it needs to
	 *                            be as much darker from its darkest neighbor as its
	 *                            darkest neighbor is from its lightest neighbor.
	 *                            Note that this value can be greater than 1.0.
	 */
	public List<Point2D> findSpinesAlongSidepath(int pixelWindowSize, int scanspan, double contrastSensitivity) {
		for (SearchPixel px : this.path) {
			px.vicinityStats = px.computePixelVicinityStats(pixelWindowSize);
		}

		List<Point2D> spinePoints = new ArrayList<Point2D>();

		for (int iPixel = scanspan; iPixel < this.path.size() - scanspan; iPixel++) {
			SearchPixel px = this.path.get(iPixel);

			int xCenter = px.x;
			int yCenter = px.y;

			List<SearchPixel> pxsBothIncluding = this.path.subList(iPixel - scanspan, iPixel + scanspan + 1);
			double myVicinityBrightness = px.vicinityStats.getMean();
			double darkestNearbyVicinity = SearchPixel.getDarkestVicinityBrightness(pxsBothIncluding);
			if (myVicinityBrightness > darkestNearbyVicinity) {
				// I can't be a spine. Spines are the darkest things around.
				continue;
			}

			// It's not enough for a pixel vicinity to merely be the darkest vicinity in the
			// area; after all, by definition, *every* set of vicinities has *some* darkest
			// member.
			// No, it also has to be "unusually" dark in order to be considered a distinct
			// feature.
			List<SearchPixel> pxsBefore = this.path.subList(iPixel - scanspan, iPixel);
			List<SearchPixel> pxsAfter = this.path.subList(iPixel + 1, iPixel + scanspan + 1);

			double brtBefore = SearchPixel.getMeanVicinityBrightness(pxsBefore);
			double brtAfter = SearchPixel.getMeanVicinityBrightness(pxsAfter);
			double brtAdjacentBright = Math.max(brtBefore, brtAfter);
			double brtAdjacentDark = Math.max(brtBefore, brtAfter);

			double brtAdjacentBrightnessRatio = brtAdjacentDark / brtAdjacentBright;

			double brtUnusualDarknessThreshold = brtAdjacentBrightnessRatio / (1.0 + contrastSensitivity);

			if (myVicinityBrightness > brtUnusualDarknessThreshold) {
				// I'm dark, but I'm not "unusually" dark, so I don't count.
				continue;
			}

			// If we've made it down here, then we're a spine.
			spinePoints.add(new Point2D.Double(xCenter, yCenter));
		}

		return spinePoints;
	}

	public String getName() {
		String s = this.name;
		if (s == null) {
			s = "";
		}
		s = s.trim();

		if (s.isEmpty()) {
			if (path == null || path.size() < 2) {
				s = "Path with no points specified";
			}

			SearchPixel pixelFirst = path.get(0);
			SearchPixel pixelLast = path.get(path.size() - 1);

			s = "Path #" + this.id + " from : (" + pixelFirst.x + ", " + pixelFirst.y + ") to (" + pixelLast.x + ", "
					+ pixelLast.y + ")";
		}
		return s;
	}

	@Override
	public String toString() {
		String s = this.getName();
		s += this.nameSuffix;
		return s;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("id", this.id);
		jsonObj.put("minimumseparation", this.minimumSeparation);

		if (this.name != null && !this.name.trim().isEmpty()) {
			jsonObj.put("name", this.name.trim());
		}

		JSONArray jsonPixels = new JSONArray();
		for (SearchPixel pixel : this.path) {
			JSONObject jsonPixel = new JSONObject();
			jsonPixel.put("x", pixel.x);
			jsonPixel.put("y", pixel.y);
			jsonPixel.put("left", pixel.similarityBoundaryDistanceLeft);
			jsonPixel.put("right", pixel.similarityBoundaryDistanceRight);
			jsonPixel.put("orthoX", pixel.unitOrthogonal.getX());
			jsonPixel.put("orthoY", pixel.unitOrthogonal.getY());
			jsonPixels.add(jsonPixel);
		}
		jsonObj.put("pixels", jsonPixels);

		JSONArray jsonSpines = new JSONArray();
		for (Point2D spine : this.spines) {
			JSONObject jsonSpine = new JSONObject();
			jsonSpine.put("x", spine.getX());
			jsonSpine.put("y", spine.getY());
			jsonSpines.add(jsonSpine);
		}
		jsonObj.put("spines", jsonSpines);

		return jsonObj;
	}

	public <T extends RealType<?>> void fromJSON(JSONObject jsonObj, Img<T> img) {
		this.id = Integer.parseInt(jsonObj.get("id").toString());
		this.minimumSeparation = Double.parseDouble(jsonObj.get("minimumseparation").toString());

		if (jsonObj.containsKey("name")) {
			this.name = jsonObj.get("name").toString().trim();
		}

		JSONArray jsonSpines = (JSONArray) jsonObj.get("spines");
		for (int iSpine = 0; iSpine < jsonSpines.size(); iSpine++) {
			JSONObject jsonSpine = (JSONObject) jsonSpines.get(iSpine);
			double x = Double.parseDouble(jsonSpine.get("x").toString());
			double y = Double.parseDouble(jsonSpine.get("y").toString());
			this.spines.add(new Point2D.Double(x, y));
		}

		JSONArray jsonPixels = (JSONArray) jsonObj.get("pixels");
		for (int iPixel = 0; iPixel < jsonPixels.size(); iPixel++) {
			JSONObject jsonPixel = (JSONObject) jsonPixels.get(iPixel);
			int x = Integer.parseInt(jsonPixel.get("x").toString());
			int y = Integer.parseInt(jsonPixel.get("y").toString());
			double left = Double.parseDouble(jsonPixel.get("left").toString());
			double right = Double.parseDouble(jsonPixel.get("right").toString());
			double orthoX = Double.parseDouble(jsonPixel.get("orthoX").toString());
			double orthoY = Double.parseDouble(jsonPixel.get("orthoY").toString());

			SearchPixel px = SearchPixel.fromImage(img, (int) x, (int) y);
			px.similarityBoundaryDistanceLeft = left;
			px.similarityBoundaryDistanceRight = right;
			px.unitOrthogonal = new Point2D.Double(orthoX, orthoY);

			this.path.add(px);
		}
	}

}
