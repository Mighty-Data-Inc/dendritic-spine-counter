package com.MightyDataInc.DendriticSpineCounter.model;

import java.util.HashSet;
import java.util.Set;

public class SpineClassifier {
	public Set<String> spineClasses;
	
	public SpineClassifier() {
		this.spineClasses = new HashSet<String>();
		this.spineClasses.add("stubby");
		this.spineClasses.add("mushroom");
		this.spineClasses.add("thin");
		this.spineClasses.add("filopodia");
	}
}
