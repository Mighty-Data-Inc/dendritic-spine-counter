package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

import ij.Executer;
import ij.measure.Calibration;

public class CalibrationPanel extends DscBasePanel {
	private static final long serialVersionUID = 6374646782021205583L;

	private JTextField textfieldCurrentScaleValue;
	private JTextField textfieldCurrentScaleUnits;

	private JTextField textfieldFeatureWindowPixels;
	private JTextField textfieldFeatureWindowPhysicalUnits;

	private JLabel lblImageUnits;
	private JLabel lblImageResolution;

	public CalibrationPanel(JTabbedPane tabbedPane, Dendritic_Spine_Counter dscplugin, DscModel model) {
		super(tabbedPane, dscplugin, model);
	}

	/**
	 * The user needs to tell us how big a visual feature is. Ultimately we need
	 * this info in pixels, but the user needs to be able to specify it in microns
	 * as well.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	@Override
	public JPanel init() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;
		gridbagConstraints.weighty = 0.0;

		String infoMsg = "<html><div style=\"padding-left: 4em;\">"
				+ "<p>Dendritic Spine Counter needs to know how big of a window to use "
				+ "when scanning this image for visually discernible features. This window's "
				+ "size should be set to the approximate size of an observable dendritic spine, "
				+ "which may depend on factors such as stain quality and image sharpness.</p><br/>"
				+ "<p>Setting this value too high will cause the plugin to fail to find smaller "
				+ "or blurrier spines (Type II errors). Setting it too low will cause the "
				+ "plugin to incorrectly identify spines where none exist (Type I errors). "
				+ "(A low setting may also make the plugin run more slowly.)</p>" + "</div></html>";

		{
			JLabel label = new JLabel("<html>" + infoMsg + "</html>", SwingConstants.RIGHT);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridwidth = 3;
			this.add(label, gridbagConstraints);
		}

		{
			gridbagConstraints.insets.top = 16;
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;

			// First (real) row: Ask them to give us a number.
			// If this number is in pixels, then that's all we need from them at this point.
			JLabel label = new JLabel(
					"<html>" + "Feature detection window size: <br/><i>(minimum 5 pixels)</i>" + "</html>",
					SwingConstants.RIGHT);
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridx++;
			textfieldFeatureWindowPixels = new JTextField(5);
			textfieldFeatureWindowPixels.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onPixelsTyped(false);
				}
			});
			textfieldFeatureWindowPixels.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					onPixelsTyped(true);
				}
			});
			this.add(textfieldFeatureWindowPixels, gridbagConstraints);

			gridbagConstraints.gridx++;
			this.add(new JLabel("pixels"), gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 2;

			gridbagConstraints.gridx = 1;
			textfieldFeatureWindowPhysicalUnits = new JTextField(5);
			textfieldFeatureWindowPhysicalUnits.setEnabled(false);
			textfieldFeatureWindowPhysicalUnits.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					onPhysicalUnitsTyped(false);
				}
			});
			textfieldFeatureWindowPhysicalUnits.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					onPhysicalUnitsTyped(true);
				}
			});
			this.add(textfieldFeatureWindowPhysicalUnits, gridbagConstraints);

			gridbagConstraints.gridx++;
			lblImageUnits = new JLabel();
			this.add(lblImageUnits, gridbagConstraints);
		}

		{
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 16;

			JLabel label = new JLabel("Image scale:", SwingConstants.RIGHT);
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridx++;
			textfieldCurrentScaleValue = new JTextField(5);
			textfieldCurrentScaleValue.setEditable(false);
			this.add(textfieldCurrentScaleValue, gridbagConstraints);

			gridbagConstraints.gridx++;
			textfieldCurrentScaleUnits = new JTextField(5);
			textfieldCurrentScaleUnits.setEditable(false);
			this.add(textfieldCurrentScaleUnits, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 1;
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.insets.top = 2;

			JButton btnOpenSetScale = new JButton("Set Scale...");
			btnOpenSetScale.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dscPlugin.runScaleSettingDialog();
					update();
				}
			});
			this.add(btnOpenSetScale, gridbagConstraints);
		}

		{
			gridbagConstraints.gridwidth = 3;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;

			JLabel label = new JLabel("<html>" + "<hr/><br/>" + "This plugin uses 2D images with a light background. "
					+ "If you wish to analyze 3D stacks, please first convert them "
					+ "to a 2D image (using MinIP, MaxIP, etc.). "
					+ "If your background is dark, you may use the button below to invert the image." + "</html>");
			gridbagConstraints.insets.top = 20;
			gridbagConstraints.insets.bottom = 8;
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;

			gridbagConstraints.insets.top = 4;
			JButton btnInvertImage = new JButton("Invert image brightness levels");
			btnInvertImage.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					dscPlugin.imageProcessor.invertImage();
				}
			});
			this.add(btnInvertImage, gridbagConstraints);
		}

		addNextButton("Next: Trace dendrites", "images/icons/dsc--trace-dendrite-24.png");

		update();
		return this;
	}

	@Override
	public void update() {
		if (this.dscPlugin.imageProcessor != null) {
			Calibration cal = this.dscPlugin.imageProcessor.getDimensions();
			model.setImageScale(cal);
		}

		this.textfieldFeatureWindowPixels.setText(String.format("%f", this.model.getFeatureWindowPixelSize()));

		if (this.model.imageHasValidPhysicalUnitScale()) {
			this.textfieldCurrentScaleValue
					.setText(String.format("%f", this.model.getImageScalePhysicalUnitsPerPixel()));
			this.textfieldCurrentScaleUnits.setText(this.model.getImageScalePhysicalUnitName() + "(s) per pixel");

			this.textfieldFeatureWindowPhysicalUnits.setEnabled(true);
			this.textfieldFeatureWindowPhysicalUnits.setText(String.format("%f",
					this.model.convertImageScalePixelsToPhysicalUnits(this.model.getFeatureWindowPixelSize())));

			this.lblImageUnits.setText(this.model.getImageScalePhysicalUnitName() + "(s)");
		} else {
			String sunk = "(unknown units)";
			this.textfieldCurrentScaleValue.setText("");
			this.textfieldCurrentScaleUnits.setText(sunk);

			this.textfieldFeatureWindowPhysicalUnits.setEnabled(false);
			this.textfieldFeatureWindowPhysicalUnits.setText("");

			this.lblImageUnits.setText(sunk);
		}

	}

	private void onPixelsTyped(boolean setValueOnModel) {
		String t = textfieldFeatureWindowPixels.getText();

		double v = 0;
		try {
			v = Double.parseDouble(t);
		} catch (NumberFormatException ex) {
		}

		if (setValueOnModel) {
			this.model.setFeatureWindowPixelSize(v);
			v = this.model.getFeatureWindowPixelSize();

			textfieldFeatureWindowPixels.setText(String.format("%f", v));
		}

		if (this.model.imageHasValidPhysicalUnitScale()) {
			double y = this.model.convertImageScalePixelsToPhysicalUnits(v);
			textfieldFeatureWindowPhysicalUnits.setText(String.format("%f", y));
		} else {
			textfieldFeatureWindowPhysicalUnits.setText("");
		}
	}

	private void onPhysicalUnitsTyped(boolean setValueOnModel) {
		String t = textfieldFeatureWindowPhysicalUnits.getText();

		double v = 0;
		try {
			v = Double.parseDouble(t);
		} catch (NumberFormatException ex) {
		}

		if (setValueOnModel) {
			this.model.setFeatureWindowPhysicalUnitSize(v);
			v = this.model.getFeatureWindowPhysicalUnitSize();

			textfieldFeatureWindowPhysicalUnits.setText(String.format("%f", v));
		}

		double y = this.model.convertImageScalePhysicalUnitsToPixels(v);
		textfieldFeatureWindowPixels.setText(String.format("%f", y));
	}

	protected void onTimer() {
		Calibration cal = this.dscPlugin.imageProcessor.getDimensions();
		
		double pwDesired = model.getImageScalePhysicalUnitsPerPixel();
		
		if (cal.pixelWidth != pwDesired
				|| cal.getUnit() != model.getImageScalePhysicalUnitName()) {
			model.setImageScale(cal);
			update();
		}
	}
}
