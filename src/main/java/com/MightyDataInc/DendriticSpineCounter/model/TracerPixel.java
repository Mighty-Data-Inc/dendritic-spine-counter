package com.MightyDataInc.DendriticSpineCounter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import java.awt.geom.Point2D;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class TracerPixel extends Point2D implements Comparable<TracerPixel> {
	final static double TRAVELDISTANCE_COST_WEIGHT = 1.0;
	final static double BRIGHTNESS_COST_WEIGHT = 25.0;

	final static double HEURISTIC_COST_WEIGHT = 0.7;
	final static double ACCUMULATED_COST_WEIGHT = 1.0;

	public int x;
	public int y;
	public double brightness;
	public double accumulatedCost = 0;
	public double heuristicCost = 0;
	public TracerPixel fromPixel;
	public Img<? extends RealType<?>> fromImg;

	/**
	 * Searches for the best sequence of pixels to go from the start position to the
	 * end position, adhering to dark pixels using an A* algorithm.
	 * 
	 * @param xStart            Start position's x coordinate
	 * @param yStart            Start position's y coordinate
	 * @param xEnd              End position's x coordinate
	 * @param yEnd              End position's y coordinate
	 * @param workingImg        The image to search within. Must be a grayscale
	 *                          image with a light background and dark foreground.
	 * @param totalSearchVolume Optional (can be null). The total collection of all
	 *                          pixels visited during the search process. Useful for
	 *                          gauging search efficiency.
	 * @return A sequence of pixels traveling from the start to the end positions,
	 *         obeying the constraints of the search parameters laid out in the
	 *         WEIGHT calibrations.
	 */
	public static List<TracerPixel> trace(int xStart, int yStart, int xEnd, int yEnd, Img<UnsignedByteType> img,
			List<TracerPixel> totalSearchVolume) {
		PriorityQueue<TracerPixel> searchFrontier = new PriorityQueue<TracerPixel>();
		HashMap<String, TracerPixel> alreadyVisited = new HashMap<String, TracerPixel>();

		TracerPixel startPixel = new TracerPixel(img, xStart, yStart);
		searchFrontier.add(startPixel);
		alreadyVisited.put(startPixel.key(), startPixel);

		TracerPixel pixel;
		TracerPixel destinationPixel = null;
		while ((pixel = searchFrontier.poll()) != null && destinationPixel == null) {
			PriorityQueue<TracerPixel> nextToSearch = pixel.neighbors();

			for (TracerPixel nextPixel : nextToSearch) {
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

		// We have a handle to the tracer pixel that finally arrived at the
		// destination pixel. We can backtrace from the destination pixel back
		// along its chain of parents to get a path of pixels that constitutes
		// the optimal route. Unfortunately, this route will be backwards!
		// So we'll store it in a temporary backpath variable, and then we'll reverse
		// it.
		ArrayList<TracerPixel> backpath = new ArrayList<TracerPixel>();
		TracerPixel backpathPixel = destinationPixel;

		while (backpathPixel != null) {
			backpath.add(backpathPixel);
			backpathPixel = backpathPixel.fromPixel;
		}
		// Backpath is no longer a backpath after being reversed.
		Collections.reverse(backpath);

		if (totalSearchVolume != null) {
			totalSearchVolume.addAll(alreadyVisited.values());
		}

		return backpath;
	}

	/**
	 * Follows a contiguous trail of dark pixels using an A* search pattern,
	 * sticking close to the waypoints specified in pathPoints.
	 * 
	 * @param pathPoints        A set of waypoints to follow. The resulting path
	 *                          will connect these waypoints (or come very close to
	 *                          them) via the darkest contiguous sequence of pixels
	 *                          it can find.
	 * @param workingImg        The image to search within. Must be a grayscale
	 *                          image with a light background and dark foreground.
	 * @param totalSearchVolume Optional (can be null). The total collection of all
	 *                          pixels visited during the search process. Useful for
	 *                          gauging search efficiency.
	 * @return A sequence of dark pixels that connect (or come near) the pathPoints.
	 */
	public static List<TracerPixel> trace(List<Point2D> pathPoints, Img<UnsignedByteType> workingImg,
			List<TracerPixel> totalSearchVolume) {
		if (pathPoints == null || pathPoints.size() == 0) {
			return null;
		}

		List<TracerPixel> path = new ArrayList<TracerPixel>();

		int xlast = 0;
		int ylast = 0;
		for (Point2D pathPoint : pathPoints) {
			int x = (int) pathPoint.getX();
			int y = (int) pathPoint.getY();

			boolean isFirst = xlast == 0 && ylast == 0;
			if (!isFirst) {
				List<TracerPixel> pixelsInSubsegment = TracerPixel.trace(xlast, ylast, x, y, workingImg,
						totalSearchVolume);
				path.addAll(pixelsInSubsegment);
			}

			xlast = x;
			ylast = y;
		}
		return path;
	}

	public <T extends RealType<?>> TracerPixel(Img<T> img, int x, int y) {
		this.fromImg = img;
		this.x = x;
		this.y = y;

		final RandomAccess<T> r = img.randomAccess();

		r.setPosition(new long[] { x, y });
		T t = r.get();

		this.brightness = t.getRealDouble() / t.getMaxValue();
	}

	public static <T extends RealType<?>> TracerPixel fromImage(Img<T> img, int x, int y) {
		if (img == null) {
			return null;
		}
		if (x < 0 || y < 0) {
			return null;
		}

		long width = img.dimension(0);
		long height = img.dimension(1);

		if (x >= width || y >= height) {
			return null;
		}

		final RandomAccess<T> r = img.randomAccess();

		r.setPosition(new long[] { x, y });

		TracerPixel searchPix = new TracerPixel(img, x, y);
		return searchPix;
	}

	@Override
	public int compareTo(TracerPixel s) {
		double ourTotalCost = (accumulatedCost * ACCUMULATED_COST_WEIGHT) + (heuristicCost * HEURISTIC_COST_WEIGHT);
		double otherTotalCost = (s.accumulatedCost * ACCUMULATED_COST_WEIGHT)
				+ (s.heuristicCost * HEURISTIC_COST_WEIGHT);

		if (ourTotalCost < otherTotalCost) {
			return -1;
		} else if (ourTotalCost > otherTotalCost) {
			return 1;
		}
		return 0;
	}

	public String key() {
		String str = "" + x + ", " + y;
		return str;
	}

	public TracerPixel neighbor(int xOffset, int yOffset) {
		int neighborX = x + xOffset;
		int neighborY = y + yOffset;

		TracerPixel searchPix = fromImage(fromImg, neighborX, neighborY);

		if (searchPix == null) {
			return null;
		}

		searchPix.fromPixel = this;

		double brightnessCostComponent = searchPix.brightness * BRIGHTNESS_COST_WEIGHT;
		double traveldistanceCostComponent = Math.hypot(xOffset, yOffset) * TRAVELDISTANCE_COST_WEIGHT;

		searchPix.accumulatedCost = accumulatedCost + brightnessCostComponent + traveldistanceCostComponent;

		return searchPix;
	}

	public PriorityQueue<TracerPixel> neighbors() {
		PriorityQueue<TracerPixel> adjacentPixels = new PriorityQueue<TracerPixel>();

		for (int yOffset = -1; yOffset <= 1; yOffset++) {
			for (int xOffset = -1; xOffset <= 1; xOffset++) {
				if (xOffset == 0 && yOffset == 0) {
					continue;
				}
				TracerPixel searchPix = neighbor(xOffset, yOffset);
				if (searchPix == null) {
					continue;
				}

				adjacentPixels.add(searchPix);
			}
		}

		return adjacentPixels;
	}

	@Override
	public String toString() {
		String str = key() + "\t brightness: " + brightness + "\t accumulatedCost: " + accumulatedCost;
		return str;
	}

	public void setDistanceToDestination(double distance) {
		heuristicCost = distance;
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
}
