package com.MightyDataInc.DendriticSpineCounter.model.events;

import java.util.EventObject;

import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

public class DscModelChangedEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2154735708645924421L;

	public DscModelChangedEvent(DscModel model) {
		super(model);
	}

	public DscModel getModel() {
		return (DscModel) getSource();
	}
}