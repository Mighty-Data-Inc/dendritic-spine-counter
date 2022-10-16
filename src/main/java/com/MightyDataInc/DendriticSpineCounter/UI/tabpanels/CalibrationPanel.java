package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.UI.DscImageProcessor;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

import ij.measure.Calibration;

public class CalibrationPanel extends DscBasePanel {
	private static final long serialVersionUID = 6374646782021205583L;

	private JTextField textfieldCurrentScaleValue;
	private JTextField textfieldCurrentScaleUnits;

	private JTextField textfieldFeatureWindowPixels;
	private JTextField textfieldFeatureWindowPhysicalUnits;

	private JLabel lblImageUnits;

	public CalibrationPanel(DscControlPanelDialog controlPanel) {
		super(controlPanel);
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

		String infoMsg = "<div style=\"padding-left: 4em;\">"
				+ "<p><strong>Dendritic Spine Counter needs to know how big your spines are on this image.</strong></p><br/>"
				+ "<p>In the center of your Working Image window, you should see a <strong>small blue selection circle</strong>, "
				+ "which you should be able to move around and resize. "
				+ "(If you don't see this circle or if you can't move it around, click "
				+ "\"Re-center calibration circle\" to put it in the center of your Working Image window.)</p>"
				+ "<ul><li>Pick a roughly average-sized dendritic spine anywhere on your image.</li>"
				+ "<li>Pan and zoom so that this spine is prominent in your window.</li>"
				+ "<li>Move the calibration circle onto this spine. "
				+ "(If the circle isn't in your viewport, click the \"Re-center\" button.)</li>"
				+ "<li>Resize the calibration circle to match the size of the spine. Try to keep the circle circular. (Try holding the SHIFT key while resizing.)</li>"
				+ "</ul><p>When you're done, click \"Next\" at the bottom of this panel.</p>";

		{
			JLabel label = new JLabel("<html>" + infoMsg + "</html>", SwingConstants.RIGHT);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridwidth = 3;
			this.add(label, gridbagConstraints);
		}

		{
			gridbagConstraints.gridy++;
			int gridy = gridbagConstraints.gridy;

			gridbagConstraints.insets.top = 8;
			gridbagConstraints.insets.bottom = 2;
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridheight = 8;

			String pathToImage = "images/guides/ring-around-a-dendritic-spine.jpg";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
			JLabel label = new JLabel(myIcon);
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridy += 4;
			gridbagConstraints.gridheight = 1;
			label = new JLabel("<html><i>Example of how to place the calibration circle</i></html>",
					SwingConstants.CENTER);
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridy++;
			JButton btnRecenterCalib = new JButton("Re-center calibration circle");
			btnRecenterCalib.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					controlPanel.getPlugin().getImageProcessor().showHideFeatureSizeSelectorRoi(true);
				}
			});
			this.add(btnRecenterCalib, gridbagConstraints);

			gridbagConstraints.gridy = gridy;
		}

		{
			gridbagConstraints.insets.top = 2;
			gridbagConstraints.insets.bottom = 2;
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.gridheight = 1;
			gridbagConstraints.gridx = 1;

			// First (real) row: Ask them to give us a number.
			// If this number is in pixels, then that's all we need from them at this point.
			JLabel label = new JLabel("<html>Spine detection window size <i>(minimum 5 pixels)</i></html>");
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 1;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 1;
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
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.gridx = 1;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 16;

			JLabel label = new JLabel("Image scale:");
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 1;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.insets.top = 2;
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
					getControlPanel().getPlugin().runScaleSettingDialog();
					update();
				}
			});
			this.add(btnOpenSetScale, gridbagConstraints);
		}

		{
			gridbagConstraints.gridwidth = 3;
			gridbagConstraints.gridheight = 1;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 20;
			gridbagConstraints.insets.bottom = 8;
			this.add(new JSeparator(), gridbagConstraints);
		}

		{
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridheight = 2;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;

			String pathToImage = "images/guides/invert-bw.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
			JLabel imglabel = new JLabel(myIcon);
			this.add(imglabel, gridbagConstraints);

			gridbagConstraints.gridx = 1;
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.gridheight = 1;
			JLabel label = new JLabel("<html>This plugin uses 2D images with a light background. "
					+ "If you wish to analyze 3D stacks, please first convert them "
					+ "to a 2D image (using MinIP, MaxIP, etc.). "
					+ "If your background is dark, you may use the button below to invert the image." + "</html>");
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 1;

			gridbagConstraints.insets.top = 4;
			gridbagConstraints.insets.bottom = 20;
			JButton btnInvertImage = new JButton("Invert image brightness levels");
			btnInvertImage.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getControlPanel().getPlugin().getImageProcessor().invertImage();
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
		DscModel model = getControlPanel().getPlugin().getModel();
		DscImageProcessor imageProcessor = getControlPanel().getPlugin().getImageProcessor();

		if (imageProcessor != null) {
			Calibration cal = imageProcessor.getDimensions();
			model.setImageScale(cal);
		}

		this.textfieldFeatureWindowPixels.setText(String.format("%f", model.getFeatureWindowSizeInPixels()));

		if (model.imageHasValidPhysicalUnitScale()) {
			this.textfieldCurrentScaleValue.setText(String.format("%f", model.getImageScalePhysicalUnitsPerPixel()));
			this.textfieldCurrentScaleUnits.setText(model.getImageScalePhysicalUnitName() + "(s) per pixel");

			this.textfieldFeatureWindowPhysicalUnits.setEnabled(true);
			this.textfieldFeatureWindowPhysicalUnits.setText(String.format("%f",
					model.convertImageScaleFromPixelsToPhysicalUnits(model.getFeatureWindowSizeInPixels())));

			this.lblImageUnits.setText(model.getImageScalePhysicalUnitName() + "(s)");
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
		DscModel model = getControlPanel().getPlugin().getModel();
		String t = textfieldFeatureWindowPixels.getText();

		double v = 0;
		try {
			v = Double.parseDouble(t);
		} catch (NumberFormatException ex) {
		}

		if (setValueOnModel) {
			model.setFeatureWindowSizeInPixels(v);
			v = model.getFeatureWindowSizeInPixels();

			textfieldFeatureWindowPixels.setText(String.format("%f", v));

			getControlPanel().getPlugin().getImageProcessor().showHideFeatureSizeSelectorRoi(true);
		}

		if (model.imageHasValidPhysicalUnitScale()) {
			double y = model.convertImageScaleFromPixelsToPhysicalUnits(v);
			textfieldFeatureWindowPhysicalUnits.setText(String.format("%f", y));
		} else {
			textfieldFeatureWindowPhysicalUnits.setText("");
		}
	}

	private void onPhysicalUnitsTyped(boolean setValueOnModel) {
		DscModel model = getControlPanel().getPlugin().getModel();
		String t = textfieldFeatureWindowPhysicalUnits.getText();

		double v = 0;
		try {
			v = Double.parseDouble(t);
		} catch (NumberFormatException ex) {
		}

		if (setValueOnModel) {
			model.setFeatureWindowSizeInPhysicalUnits(v);
			v = model.getFeatureWindowSizeInPhysicalUnits();

			textfieldFeatureWindowPhysicalUnits.setText(String.format("%f", v));

			getControlPanel().getPlugin().getImageProcessor().showHideFeatureSizeSelectorRoi(true);
		}

		double y = model.convertImageScaleFromPhysicalUnitsToPixels(v);
		textfieldFeatureWindowPixels.setText(String.format("%f", y));
	}

	private void getFeatureSizeFromSelector() {
		DscImageProcessor imageProcessor = getControlPanel().getPlugin().getImageProcessor();
		double pixels = imageProcessor.getFeatureSizeSelectorRoiSizeInPixels();
		if (pixels == 0) {
			return;
		}

		DscModel model = getControlPanel().getPlugin().getModel();
		boolean didFeatureSizeChange = model.setFeatureWindowSizeInPixels(pixels);
		if (didFeatureSizeChange) {
			this.update();
		}
	}

	private void showFeatureSizeSelector() {
		DscImageProcessor imageProcessor = getControlPanel().getPlugin().getImageProcessor();
		imageProcessor.showHideFeatureSizeSelectorRoi(true);
	}
	
	@Override
	protected void onTimer() {
		showFeatureSizeSelector();

		getFeatureSizeFromSelector();

		DscModel model = getControlPanel().getPlugin().getModel();

		Calibration cal = getControlPanel().getPlugin().getImageProcessor().getDimensions();
		if (cal == null) {
			return;
		}

		double pwDesired = model.getImageScalePhysicalUnitsPerPixel();

		if (cal.pixelWidth != pwDesired || cal.getUnit() != model.getImageScalePhysicalUnitName()) {
			model.setImageScale(cal);
			update();
		}
	}

	@Override
	protected void onPanelEntered() {
		this.showFeatureSizeSelector();
	}

	@Override
	protected void onPanelExited() {
		getFeatureSizeFromSelector();
		getControlPanel().getPlugin().getImageProcessor().showHideFeatureSizeSelectorRoi(false);
	}

}
