package com.MightyDataInc.DendriticSpineCounter.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
		if (imageScalePhysicalUnitName == null || imageScalePhysicalUnitName.length() == 0
				|| imageScalePhysicalUnitName.equals("null") || imageScalePhysicalUnitName.equals("pixel")
				|| imageScalePhysicalUnitName.equals("pixels") || imageScalePhysicalUnitName.equals("pixel(s)")) {
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

	public void removeDendrite(DendriteBranch dendrite) {
		if (dendrite == null) {
			return;
		}
		if (dendrites.containsKey(dendrite.getId())) {
			dendrites.remove(dendrite.getId());
			this.removeSpinesOfDendrite(dendrite);
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

	public void clearDendrites() {
		dendrites.clear();
		spines.clear();
	}

	public void findNearestDendritesForAllSpines() {
		for (DendriteSpine spine : this.getSpines()) {
			spine.findNearestDendrite(getDendrites());
		}
	}

	// endregion

	// region Manage spines.
	private TreeMap<Integer, DendriteSpine> spines = new TreeMap<Integer, DendriteSpine>();

	public void addSpine(DendriteSpine spine) {
		if (spine == null) {
			return;
		}
		spines.put(spine.getId(), spine);
	}

	public boolean hasSpine(DendriteSpine spine) {
		return this.spines.containsKey(spine.getId());
	}

	public boolean addSpineWithOverlapImprovement(DendriteSpine spine) {
		List<DendriteSpine> overlaps = spine.findAllOverlaps(getSpines());
		if (spine.contrast == 0 && overlaps.size() > 0) {
			return false;
		}
		if (overlaps.size() == 0) {
			this.addSpine(spine);
			return true;
		}

		double maxContrast = 0;
		for (DendriteSpine olspine : overlaps) {
			if (olspine.contrast > maxContrast) {
				maxContrast = olspine.contrast;
			}
		}
		if (maxContrast > spine.contrast) {
			// At least one existing overlapping spine has better contrast than this one.
			return false;
		}
		// This spine has the best contrast. Get rid of all others.
		removeSpines(overlaps);
		this.addSpine(spine);
		return true;
	}

	public void removeSpines(Collection<DendriteSpine> spines) {
		for (DendriteSpine spine : spines) {
			removeSpine(spine);
		}
	}

	public void removeSpine(DendriteSpine spine) {
		if (spine == null) {
			return;
		}
		spines.remove(spine.getId());
	}

	public DendriteSpine getSpine(int id) {
		if (spines.containsKey(id)) {
			return spines.get(id);
		}
		return null;
	}

	public List<DendriteSpine> getSpines() {
		List<DendriteSpine> slist = new ArrayList<DendriteSpine>();
		slist.addAll(spines.values());
		return slist;
	}

	public List<DendriteSpine> getSpinesOfDendrite(DendriteBranch dendrite) {
		List<DendriteSpine> slist = new ArrayList<DendriteSpine>();
		if (dendrite == null) {
			return slist;
		}
		for (DendriteSpine spine : this.getSpines()) {
			if (spine.getNearestDendrite() == dendrite) {
				slist.add(spine);
			}
		}
		return slist;
	}

	public void removeSpinesOfDendrite(DendriteBranch dendrite) {
		for (DendriteSpine spine : this.getSpinesOfDendrite(dendrite)) {
			this.removeSpine(spine);
		}
	}

	public void clearSpines() {
		spines.clear();
	}

	public List<DendriteSpine> getUnclassifiedSpines() {
		List<DendriteSpine> unclassifiedSpines = new ArrayList<DendriteSpine>();
		for (DendriteSpine spine : this.getSpines()) {
			if (!spine.hasClassification()) {
				unclassifiedSpines.add(spine);
			}
		}
		return unclassifiedSpines;
	}

	public DendriteSpine findNextUnclassifiedSpine(DendriteSpine fromSpine) {
		if (getUnclassifiedSpines().size() == 0) {
			// If no unclassified spines exist, just get the next spine.
			return findNextSpine(fromSpine);
		}
		while (true) {
			fromSpine = findNextSpine(fromSpine);
			if (fromSpine == null) {
				return null;
			}
			if (!fromSpine.hasClassification()) {
				return fromSpine;
			}
		}
	}

	public DendriteSpine findNextSpine(DendriteSpine fromSpine) {
		List<DendriteSpine> spines = this.getSpines();
		if (spines.size() == 0) {
			return null;
		}
		if (fromSpine == null) {
			return spines.get(0);
		}

		DendriteSpine prevSpine = null;
		for (DendriteSpine spine : spines) {
			if (prevSpine == fromSpine) {
				return spine;
			}
			prevSpine = spine;
		}
		return spines.get(0);
	}

	public DendriteSpine findPreviousSpine(DendriteSpine fromSpine) {
		List<DendriteSpine> spines = this.getSpines();
		if (spines.size() == 0) {
			return null;
		}
		if (fromSpine == null || fromSpine == spines.get(0)) {
			return spines.get(spines.size() - 1);
		}

		DendriteSpine prevSpine = null;
		for (DendriteSpine spine : spines) {
			if (spine == fromSpine) {
				return prevSpine;
			}
			prevSpine = spine;
		}
		return spines.get(spines.size() - 1);
	}

	// endregion

	// region Spine classes.

	public ArrayList<String> spineClasses = new ArrayList<>(Arrays.asList("stubby", "mushroom", "thin", "filopodia"));

	public void setSpineClasses(String[] newClasses) {
		spineClasses = new ArrayList<String>(Arrays.asList(newClasses));

		for (DendriteSpine spine : spines.values()) {
			if (!spineClasses.contains(spine.getClassification())) {
				spine.setClassification(null);
			}
		}
	}

	// endregion

	// region Saving and loading

	public static JSONObject getJsonObjectFromFile(String filename) {
		String filecontents = "";
		try {
			filecontents = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
		} catch (InvalidPathException e1) {
			JOptionPane.showMessageDialog(null, "Couldn't read this string as a file path: " + filename, "Invalid path",
					JOptionPane.ERROR_MESSAGE);
			return new JSONObject();
		} catch (NoSuchFileException e1) {
			JOptionPane.showMessageDialog(null, "The system could not find any such file: " + filename, "No such file",
					JOptionPane.ERROR_MESSAGE);
			return new JSONObject();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return new JSONObject();
		}

		JSONParser parser = new JSONParser();
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) parser.parse(filecontents);
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(null, e1.toString());
			return new JSONObject();
		}
		return jsonObj;
	}

	public static DscModel loadFromJsonObject(JSONObject jsonModel) {
		DscModel model = new DscModel();

		model.featureWindowSizeInPixels = (double) jsonModel.get("feature_window_size_in_pixels");
		model.imageScalePhysicalUnitName = (String) jsonModel.get("image_scale_physical_unit_name");
		model.imageScalePhysicalUnitsPerPixel = (double) jsonModel.get("image_scale_physical_units_per_pixel");

		JSONArray jsonSpineClasses = (JSONArray) jsonModel.get("spine_classes");
		model.spineClasses.clear();
		for (Object spineClass : jsonSpineClasses) {
			model.spineClasses.add((String) spineClass);
		}

		// Deserialize dendrites.
		JSONArray jsonDendrites = (JSONArray) jsonModel.get("dendrites");
		model.dendrites.clear();
		for (Object jsonDendrite : jsonDendrites) {
			DendriteBranch dendrite = DendriteBranch.loadFromJsonObject((JSONObject) jsonDendrite);
			if (dendrite == null) {
				continue;
			}
			model.dendrites.put(dendrite.getId(), dendrite);
		}

		// TODO: Deserialize spines.
		JSONArray jsonSpines = (JSONArray) jsonModel.get("spines");
		model.spines.clear();
		for (Object jsonSpine : jsonSpines) {
			DendriteSpine spine = DendriteSpine.loadFromJsonObject((JSONObject) jsonSpine);
			if (spine == null) {
				continue;
			}

			// Connect the spine to its nearest dendrite, if it's legit.
			DendriteBranch nearestDendrite = model.dendrites.get(spine.getNearestDendriteId());
			spine.setNearestDendrite(nearestDendrite);

			model.spines.put(spine.getId(), spine);
		}

		return model;
	}

	@SuppressWarnings("unchecked")
	public JSONObject saveToJsonObject() {
		JSONObject jsonModel = new JSONObject();
		jsonModel.put("feature_window_size_in_pixels", featureWindowSizeInPixels);
		jsonModel.put("image_scale_physical_unit_name", imageScalePhysicalUnitName);
		jsonModel.put("image_scale_physical_units_per_pixel", imageScalePhysicalUnitsPerPixel);

		JSONArray jsonSpineClasses = new JSONArray();
		jsonSpineClasses.addAll(this.spineClasses);
		jsonModel.put("spine_classes", jsonSpineClasses);

		JSONArray jsonDendrites = new JSONArray();
		for (DendriteBranch dendrite : this.getDendrites()) {
			jsonDendrites.add(dendrite.saveToJsonObject());
		}
		jsonModel.put("dendrites", jsonDendrites);

		JSONArray jsonSpines = new JSONArray();
		for (DendriteSpine spine : this.getSpines()) {
			jsonSpines.add(spine.saveToJsonObject());
		}
		jsonModel.put("spines", jsonSpines);

		return jsonModel;
	}

	// endregion
}
