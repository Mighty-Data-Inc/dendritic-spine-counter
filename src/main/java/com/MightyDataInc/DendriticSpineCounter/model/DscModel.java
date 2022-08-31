package com.MightyDataInc.DendriticSpineCounter.model;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;

import ij.measure.Calibration;

public class DscModel {
	public Dendritic_Spine_Counter plugin;
	public DscControlPanelDialog dialog;

	// ------------------------------------------------
	// Feature window size and unit scale

	private double featureWindowSizeInPixels = 5;
	private String imageScalePhysicalUnitName;
	private double imageScalePhysicalUnitsPerPixel;

	public double getFeatureWindowPixelSize() {
		return this.featureWindowSizeInPixels;
	}

	public void setFeatureWindowPixelSize(double pixels) {
		if (pixels < 5) {
			pixels = 5;
		}
		this.featureWindowSizeInPixels = pixels;
	}

	public double getFeatureWindowPhysicalUnitSize() {
		double v = this.convertImageScalePixelsToPhysicalUnits(featureWindowSizeInPixels);
		return v;
	}

	public void setFeatureWindowPhysicalUnitSize(double units) {
		double pixels = this.convertImageScalePhysicalUnitsToPixels(units);
		this.setFeatureWindowPixelSize(pixels);
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
		double featureSizePhysicalUnits = getFeatureWindowPhysicalUnitSize();
		this.imageScalePhysicalUnitsPerPixel = unitsperpixel;
		this.setFeatureWindowPhysicalUnitSize(featureSizePhysicalUnits);
	}

	public boolean imageHasValidPhysicalUnitScale() {
		if (imageScalePhysicalUnitName == null || imageScalePhysicalUnitName == ""
				|| imageScalePhysicalUnitName == "null") {
			return false;
		}
		return true;
	}

	public double convertImageScalePixelsToPhysicalUnits(double pixels) {
		if (imageScalePhysicalUnitsPerPixel == 0) {
			return pixels;
		}
		return pixels * imageScalePhysicalUnitsPerPixel;
	}

	public double convertImageScalePhysicalUnitsToPixels(double units) {
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

}
