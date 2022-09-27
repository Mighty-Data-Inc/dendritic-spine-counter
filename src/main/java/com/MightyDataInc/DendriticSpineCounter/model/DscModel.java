package com.MightyDataInc.DendriticSpineCounter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.MightyDataInc.DendriticSpineCounter.model.events.DscModelChangedEventListener;

import ij.measure.Calibration;

public class DscModel {
	// region Event handling

	private ArrayList<DscModelChangedEventListener> listenerList = new ArrayList<DscModelChangedEventListener>();

	public void addFooListener(DscModelChangedEventListener l) {
		listenerList.add(l);
	}

	public void removeFooListener(DscModelChangedEventListener l) {
		listenerList.remove(l);
	}

	// region Feature window size and unit scale

	private double featureWindowSizeInPixels = 25;
	private String imageScalePhysicalUnitName;
	private double imageScalePhysicalUnitsPerPixel;

	public double getFeatureWindowSizeInPixels() {
		return this.featureWindowSizeInPixels;
	}

	public boolean setFeatureWindowSizeInPixels(double pixels) {
		if (this.featureWindowSizeInPixels == pixels) {
			return false;
		}
		if (pixels < 5) {
			pixels = 5;
		}
		this.featureWindowSizeInPixels = pixels;
		return true;
	}

	public double getFeatureWindowSizeInPhysicalUnits() {
		double v = this.convertImageScaleFromPixelsToPhysicalUnits(featureWindowSizeInPixels);
		return v;
	}

	public void setFeatureWindowSizeInPhysicalUnits(double units) {
		double pixels = this.convertImageScaleFromPhysicalUnitsToPixels(units);
		this.setFeatureWindowSizeInPixels(pixels);
	}

	public String getImageScalePhysicalUnitName() {
		return this.imageScalePhysicalUnitName;
	}

	public void setImageScalePhysicalUnitName(String unitname) {
		this.imageScalePhysicalUnitName = unitname;
	}

	public double getImageScalePhysicalUnitsPerPixel() {
		return this.imageScalePhysicalUnitsPerPixel;
	}

	public void setImageScalePhysicalUnitsPerPixel(double unitsperpixel) {
		if (!this.imageHasValidPhysicalUnitScale() || this.imageScalePhysicalUnitsPerPixel == 0) {
			this.imageScalePhysicalUnitsPerPixel = unitsperpixel;
			return;
		}

		// Through the transformation, keep the size of the feature window the same.
		double featureSizePhysicalUnits = getFeatureWindowSizeInPhysicalUnits();
		this.imageScalePhysicalUnitsPerPixel = unitsperpixel;
		this.setFeatureWindowSizeInPhysicalUnits(featureSizePhysicalUnits);
	}

	public boolean imageHasValidPhysicalUnitScale() {
		if (imageScalePhysicalUnitName == null || imageScalePhysicalUnitName == ""
				|| imageScalePhysicalUnitName == "null" || imageScalePhysicalUnitName == "pixel"
				|| imageScalePhysicalUnitName == "pixels" || imageScalePhysicalUnitName == "pixel(s)") {
			return false;
		}
		return true;
	}

	public double convertImageScaleFromPixelsToPhysicalUnits(double pixels) {
		if (imageScalePhysicalUnitsPerPixel == 0) {
			return pixels;
		}
		return pixels * imageScalePhysicalUnitsPerPixel;
	}

	public double convertImageScaleFromPhysicalUnitsToPixels(double units) {
		if (imageScalePhysicalUnitsPerPixel == 0) {
			return units;
		}
		return units / imageScalePhysicalUnitsPerPixel;
	}

	public void setImageScale(Calibration cal) {
		if (cal == null) {
			this.setImageScalePhysicalUnitName("");
			this.setImageScalePhysicalUnitsPerPixel(0);
			return;
		}
		this.setImageScalePhysicalUnitName(cal.getUnit());
		this.setImageScalePhysicalUnitsPerPixel(cal.pixelWidth);
	}

	// endregion

	// region Manage dendrites.
	private TreeMap<Integer, DendriteBranch> dendrites = new TreeMap<Integer, DendriteBranch>();

	public void addDendrite(DendriteBranch dendrite) {
		if (dendrites.containsKey(dendrite.getId())) {
			throw new IllegalArgumentException(
					String.format("Dendrite branch collection already has element with id %d", dendrite.getId()));
		}
		dendrites.put(dendrite.getId(), dendrite);
	}

	public void removeDendrite(DendriteBranch dendriteBranch) {
		if (dendriteBranch == null) {
			return;
		}
		if (dendrites.containsKey(dendriteBranch.getId())) {
			dendrites.remove(dendriteBranch.getId());
		}
	}

	public DendriteBranch getDendrite(int id) {
		if (dendrites.containsKey(id)) {
			return dendrites.get(id);
		}
		return null;
	}

	public List<DendriteBranch> getDendrites() {
		List<DendriteBranch> dlist = new ArrayList<DendriteBranch>();
		dlist.addAll(dendrites.values());
		return dlist;
	}

	// endregion
}
