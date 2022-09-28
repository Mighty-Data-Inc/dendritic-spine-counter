package com.MightyDataInc.DendriticSpineCounter.model;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

import ij.gui.OvalRoi;

public class DendriteSpine extends Point2D {
	private static int nextId = 1;

	private int id = 0;

	public int getId() {
		return id;
	}

	private double x = 0;
	private double y = 0;

	private double featureWindowSize = 5;

	public double getSize() {
		return this.featureWindowSize;
	}

	public void setSize(double size) {
		this.featureWindowSize = size;
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
		roi.setStrokeWidth(1.5);
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

	private DendriteBranch nearestDendrite = null;

	public DendriteBranch getNearestDendrite() {
		return nearestDendrite;
	}

	public int getNearestDendriteId() {
		if (nearestDendrite == null) {
			return -1;
		}
		return nearestDendrite.getId();
	}

	public void setNearestDendrite(DendriteBranch dendrite) {
		this.nearestDendrite = dendrite;
	}

	public DendriteBranch findNearestDendrite(Collection<DendriteBranch> dendrites) {
		double winnerDist = java.lang.Double.MAX_VALUE;
		DendriteBranch winnerDendrite = null;

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
				}
			}
		}

		this.setNearestDendrite(winnerDendrite);
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

}