package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.simple.JSONObject;

import ij.gui.OvalRoi;

public class DendriteSpine extends Point2D {
	private static double MAX_DISTANCE_OF_SPINE_FROM_DENDRITE_IN_FEATURE_WINDOWS = 2;

	private static int nextId = 1;

	private int id = 0;

	public int getId() {
		return id;
	}

	private double x = 0;
	private double y = 0;

	public double neckLengthInPixels = 0;
	public double neckWidthInPixels = 0;
	public double headWidthInPixels = 0;

	public double contrast = 0;

	public double angle = 0;

	public String notes = "";

	private double featureWindowSize = 5;

	public double getSize() {
		return this.featureWindowSize;
	}

	public void setSize(double size) {
		this.featureWindowSize = size;
		this.createRoiAtCurrentLocation();
	}

	private OvalRoi roi = null;

	public OvalRoi getRoi() {
		return roi;
	}

	private void createRoiAtCurrentLocation() {
		int x = (int) getX();
		int y = (int) getY();
		int size = (int) getSize();

		roi = new OvalRoi(x - size / 2, y - size / 2, size, size);

		roi.setStrokeColor(Color.GREEN);
		roi.setStrokeWidth(3);
		roi.setFillColor(new Color(.4f, 1f, .6f, .4f));
	}

	public DendriteSpine(double x, double y, double size) {
		this.x = x;
		this.y = y;

		this.id = nextId;
		nextId++;

		this.featureWindowSize = size;

		this.createRoiAtCurrentLocation();
	}

	// Empty constructor for deserialization.
	private DendriteSpine() {
	}

	@Override
	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;
		this.createRoiAtCurrentLocation();
	}

	private String classification = "";

	public void setClassification(String c) {
		if (c == null) {
			c = "";
		}
		this.classification = c;
	}

	public String getClassification() {
		if (this.classification == null) {
			return "";
		}
		return this.classification;
	}

	public boolean hasClassification() {
		return this.getClassification() != null && this.getClassification().trim().length() > 0;
	}

	private DendriteBranch nearestDendrite = null;
	private int nearestDendriteId = -1;

	public DendriteBranch getNearestDendrite() {
		return nearestDendrite;
	}

	public int getNearestDendriteId() {
		return nearestDendriteId;
	}

	public void setNearestDendrite(DendriteBranch dendrite) {
		this.nearestDendrite = dendrite;
		if (dendrite != null) {
			this.nearestDendriteId = dendrite.getId();
		} else {
			this.nearestDendriteId = -1;
		}
	}

	public DendriteBranch findNearestDendrite(Collection<DendriteBranch> dendrites) {
		double winnerDist = java.lang.Double.MAX_VALUE;
		DendriteBranch winnerDendrite = null;
		Point2D winnerAngleUnitVector = null;

		for (DendriteBranch dendrite : dendrites) {
			if (dendrite == null) {
				continue;
			}

			List<Point2D> points = dendrite.getRoiPoints();
			if (points == null) {
				continue;
			}

			for (Point2D point : points) {
				double dist = this.distance(point);
				if (dist < winnerDist) {
					winnerDist = dist;
					winnerDendrite = dendrite;

					if (dist > 0) {
						winnerAngleUnitVector = new Point2D.Double(this.getX() - point.getX(),
								this.getY() - point.getY());
					}
				}
			}
		}

		if (winnerDist > MAX_DISTANCE_OF_SPINE_FROM_DENDRITE_IN_FEATURE_WINDOWS * this.featureWindowSize) {
			// This winner is too far from its dendrite to count as being associated with
			// it.
			return null;
		}

		this.setNearestDendrite(winnerDendrite);
		if (winnerAngleUnitVector != null) {
			this.angle = Math.atan2(winnerAngleUnitVector.getY(), winnerAngleUnitVector.getX());

			// From the image, angle=0 means that we're pointed directly to the right.
			// That means that the dendrite is on our left, and the spine is poking
			// rightward.
			// But when we render it in the Classify Spines panel, we want the dendrite body
			// to be on the bottom with the spine extending upward. So we rotate it.
			this.angle = (1.5 * Math.PI) - this.angle;
		}
		return winnerDendrite;
	}

	@Override
	public String toString() {
		String s = String.format("#%d:(%.1f, %.1f)(%s)/%d", this.getId(), this.getX(), this.getY(),
				this.getClassification(), this.getNearestDendriteId());
		return s;
	}

	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y;
	}

	public boolean overlaps(DendriteSpine otherSpine) {
		double centersDist = this.distance(otherSpine);
		double radii = (this.getSize() + otherSpine.getSize()) / 2.0;
		// Allow a little bit of grace.
		radii *= 0.8;
		return centersDist < radii;
	}

	public List<DendriteSpine> findAllOverlaps(Collection<DendriteSpine> spines) {
		List<DendriteSpine> outs = new ArrayList<DendriteSpine>();
		for (DendriteSpine spine : spines) {
			if (spine.overlaps(this)) {
				outs.add(spine);
			}
		}
		return outs;
	}

	@SuppressWarnings("unchecked")
	public JSONObject saveToJsonObject() {
		JSONObject jsonSpine = new JSONObject();

		jsonSpine.put("id", this.id);
		jsonSpine.put("nearest_dendrite_id", this.nearestDendriteId);

		jsonSpine.put("x", this.x);
		jsonSpine.put("y", this.y);

		jsonSpine.put("neck_length_in_pixels", this.neckLengthInPixels);
		jsonSpine.put("neck_width_in_pixels", this.neckWidthInPixels);
		jsonSpine.put("head_width_in_pixels", this.headWidthInPixels);

		jsonSpine.put("contrast", this.contrast);
		jsonSpine.put("angle", this.angle);
		jsonSpine.put("feature_window_size", this.featureWindowSize);

		jsonSpine.put("notes", this.notes);

		return jsonSpine;
	}

	public static DendriteSpine loadFromJsonObject(JSONObject jsonSpine) {
		DendriteSpine spine = new DendriteSpine();

		spine.id = ((Long) jsonSpine.get("id")).intValue();
		if (spine.id >= nextId) {
			nextId = spine.id + 1;
		}

		spine.nearestDendriteId = ((Long) jsonSpine.get("nearest_dendrite_id")).intValue();

		spine.x = (double) jsonSpine.get("x");
		spine.y = (double) jsonSpine.get("y");

		spine.neckLengthInPixels = (double) jsonSpine.get("neck_length_in_pixels");
		spine.neckWidthInPixels = (double) jsonSpine.get("neck_width_in_pixels");
		spine.headWidthInPixels = (double) jsonSpine.get("head_width_in_pixels");

		spine.contrast = (double) jsonSpine.get("contrast");
		spine.angle = (double) jsonSpine.get("angle");
		spine.featureWindowSize = (double) jsonSpine.get("feature_window_size");

		spine.notes = (String) jsonSpine.get("notes");

		spine.createRoiAtCurrentLocation();

		return spine;
	}

	// region Classification functions

	public double getHeadNeckRatio() {
		if (this.neckWidthInPixels == 0) {
			return 0;
		}
		double headNeckRatio = this.headWidthInPixels / this.neckWidthInPixels;
		return headNeckRatio;
	}

	public boolean isNeckDefined() {
		if (this.neckWidthInPixels == 0) {
			return false;
		}
		boolean retval = getHeadNeckRatio() > 1.1;
		return retval;
	}

	public double getLengthHeadRatio() {
		if (this.headWidthInPixels == 0) {
			return 0;
		}
		double lengthHeadRatio = this.neckLengthInPixels / this.headWidthInPixels;
		return lengthHeadRatio;
	}

	public boolean isTall() {
		if (this.headWidthInPixels == 0) {
			return false;
		}
		boolean retval = getLengthHeadRatio() >= 2.5;
		return retval;
	}

	public boolean isVeryLong(double imageScalePhysicalUnitsPerPixel, String imageScalePhysicalUnitName) {
		if (imageScalePhysicalUnitsPerPixel == 0) {
			return false;
		}
		if (imageScalePhysicalUnitName == null || !imageScalePhysicalUnitName.toLowerCase().startsWith("micron")) {
			return false;
		}
		if (this.neckLengthInPixels == 0) {
			return false;
		}
		double micronsLength = this.neckLengthInPixels * imageScalePhysicalUnitsPerPixel;
		boolean retval = micronsLength > 3;
		return retval;
	}

	public boolean isHeadWide(double imageScalePhysicalUnitsPerPixel, String imageScalePhysicalUnitName) {
		if (imageScalePhysicalUnitsPerPixel == 0) {
			return false;
		}
		if (imageScalePhysicalUnitName == null || !imageScalePhysicalUnitName.toLowerCase().startsWith("micron")) {
			return false;
		}
		if (this.headWidthInPixels == 0) {
			return false;
		}
		double micronsWidth = this.headWidthInPixels * imageScalePhysicalUnitsPerPixel;
		boolean retval = micronsWidth >= 0.35;
		return retval;
	}

	public String classify(double imageScalePhysicalUnitsPerPixel, String imageScalePhysicalUnitName,
			Collection<String> spineClasses) {
		if (spineClasses == null) {
			// Just create an empty string collection of some kind.
			spineClasses = new ArrayList<String>();
			spineClasses.add("stubby");
			spineClasses.add("filopodium");
			spineClasses.add("thin");
			spineClasses.add("mushroom");
		}

		if (isNeckDefined()) {
			// Is neck defined? Yes.
			if (isHeadWide(imageScalePhysicalUnitsPerPixel, imageScalePhysicalUnitName)
					&& spineClasses.contains("mushroom")) {
				// Is the head wide? Yes.
				return "mushroom";
			}
		} else {
			// Is neck defined? No.
			if (!isTall() && spineClasses.contains("stubby")) {
				// Is aspect ratio tall? No.
				return "stubby";
			}
		}

		// If we got here, then we are either an undefined neck with a tall aspect
		// ratio,
		// or a defined neck but not a wide head.

		if (isVeryLong(imageScalePhysicalUnitsPerPixel, imageScalePhysicalUnitName)
				&& spineClasses.contains("filopodium")) {
			return "filopodium";
		}

		if (spineClasses.contains("thin")) {
			return "thin";
		}

		return "";
	}

	// endregion

}