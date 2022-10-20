package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.UI.DscImageProcessor;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteBranch;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

public class FindSpinesPanel extends DscBasePanel {
	private static final long serialVersionUID = 6374646782021205583L;

	private JButton btnActivateMultiPointTool;

	private JButton btnLockSpines;
	private JButton btnUnlockSpines;

	private JButton btnDetectSpines;

	private JSlider sliderDetectionSensitivity;

	public FindSpinesPanel(DscControlPanelDialog controlPanel) {
		super(controlPanel);
	}

	/**
	 * We go back and forth with the user. They add some polyline ROIs, and we trace
	 * them into paths.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	@Override
	public JPanel init() {
		this.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.bottom = 4;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		{
			gridbagConstraints.gridx = 0;

			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

			JLabel label = new JLabel("<html>Use the Multi-point Tool to mark spines. "
					+ "<ul><li>Click on a spine to mark it with a Multi-point point.</li>"
					+ "<li>Click and drag a spine marker to relocate it.</li>"
					+ "<li>Alt-Click a spine marker to remove it.</li></ul></html>");
			this.add(label, gridbagConstraints);

			// Because we're using the same gridbagConstraints object for subsequent
			// UI elements in this panel, let's set the bottom space value back to
			// our standard.
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;

			ImageIcon icon = null;
			PluginInfo<?> pointToolInfo = controlPanel.getPlugin().getPointTool().getInfo();
			String iconDescription = pointToolInfo.getDescription();
			try {
				URL iconURL = pointToolInfo.getIconURL();
				icon = new ImageIcon(iconURL, iconDescription);
			} catch (InstantiableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			btnActivateMultiPointTool = new JButton("Activate Multi-point Tool to mark spines", icon);
			btnActivateMultiPointTool.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					controlPanel.getPlugin().activateMultiPointTool();
					update();
				}
			});
			this.add(btnActivateMultiPointTool, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			gridbagConstraints.gridx = 1;
			gridbagConstraints.gridy = 0;

			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

			JLabel label = new JLabel("<html>Points marked with the Multi-point Tool "
					+ "are only temporary until you Lock them as spines. Each spine "
					+ "will be automatically associated with its nearest dendrite branch. "
					+ "Press Unlock if you need to add, remove, or edit existing spines "
					+ "(don't forget to Lock them again afterwards!).</html>");
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridy++;

			String pathToImage = "images/icons/lock--24.png";
			ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
			btnLockSpines = new JButton("Lock Multi-point selection as spines", icon);
			btnLockSpines.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					lockSpines();
				}
			});
			this.add(btnLockSpines, gridbagConstraints);

			gridbagConstraints.gridy++;

			pathToImage = "images/icons/unlock--24.png";
			icon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
			btnUnlockSpines = new JButton("Unlock spines to edit as Multi-points", icon);
			btnUnlockSpines.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					unlockSpines();
				}
			});
			this.add(btnUnlockSpines, gridbagConstraints);
		}

		{
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 2;

			gridbagConstraints.insets.top = 16;

			this.add(new JSeparator(), gridbagConstraints);

			gridbagConstraints.insets.top = 4;
			gridbagConstraints.gridy++;

			JLabel label = new JLabel(
					"<html>" + "This plug-in can auto-detect spines that project outward from the edges of "
							+ "your dendrite branches. (You need to define at least one dendrite branch"
							+ "before using this feature.) After spines are automatically detected, you will "
							+ "then have the ability to move, add, or delete them.<br/><br/>"
							+ "NOTE: Using this function will clear your current selection and replace it with "
							+ "the auto-detected features. If you're going to use this feature, you should "
							+ "use it <i>first</i>, and <i>then</i> add more spines manually if needed." + "</html>");
			// We want extra space at the bottom of this label.
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;

			String pathToImage = "images/icons/dsc--find-spines-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnDetectSpines = new JButton("Auto-detect spines", myIcon);
			btnDetectSpines.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int sensitivitySliderVal = sliderDetectionSensitivity.getValue();
					double sensitivity = 1.0 - ((double) sensitivitySliderVal / 100.0);
					autoDetectSpines(sensitivity);
				}
			});
			this.add(btnDetectSpines, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>You can adjust the contrast sensitivity that this plug-in "
					+ "uses for automatic spine detection. <br/>"
					+ "Low sensitivity may end up mis-identifying noise as spines (Type I error). <br/>"
					+ "High sensitivity means that some faint spines might get missed (Type II error).<br/>"
					+ "<br/>Set sensitivity:</html>");
			// We want extra space at the bottom of this label.
			gridbagConstraints.insets.top = 16;
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			sliderDetectionSensitivity = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
			sliderDetectionSensitivity.setMajorTickSpacing(10);
			sliderDetectionSensitivity.setMinorTickSpacing(1);
			sliderDetectionSensitivity.setPaintTicks(true);
			sliderDetectionSensitivity.setPaintLabels(true);

			gridbagConstraints.insets.top = 4;
			this.add(sliderDetectionSensitivity, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		addNextButton("Next: Classify spines", "images/icons/dsc--classify-spines-24.png");

		update();
		return this;
	}

	@Override
	public void onTimer() {
		if (myPlugin().getCurrentToolName().equals("multipoint")) {
			this.btnActivateMultiPointTool.setEnabled(false);
		} else {
			this.btnActivateMultiPointTool.setEnabled(true);
		}

		List<Point2D> points = myImageProcessor().getCurrentImagePolylinePathPoints(Roi.POINT);
		if (points == null || points.size() == 0) {
			this.btnLockSpines.setEnabled(false);
		} else {
			this.btnLockSpines.setEnabled(true);
		}

		if (myModel().getSpines().size() > 0) {
			this.btnUnlockSpines.setEnabled(true);
		} else {
			this.btnUnlockSpines.setEnabled(false);
		}
	}

	@Override
	public void update() {
	}

	@Override
	protected void onPanelExited() {
		if (controlPanel.getPlugin().getCurrentToolName().equals("multipoint")) {
			List<Point2D> points = controlPanel.getPlugin().getImageProcessor()
					.getCurrentImagePolylinePathPoints(Roi.POINT);

			if (points != null && points.size() > 0) {
				String pathToImage = "images/icons/warning-icon.png";
				ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
				Image imgScaled = myIcon.getImage().getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH);

				int input = JOptionPane.showConfirmDialog(null,
						"<html><p>WARNING! You have an active Mulit-point selection that you haven't Locked into spines yet.</p>"
								+ "<p>Do you wish to Lock it now?</p><br/>"
								+ "<p>YES: Lock your current Multi-point selection as your spines.</p>"
								+ "<p>NO: Discard your Multi-point selection.</p>",
						"Lock Multi-point Selection?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
						new ImageIcon(imgScaled));

				// 0=yes, 1=no
				if (input == 0) {
					this.lockSpines();
				}
			}
		}
	}

	private void lockSpines() {
		List<Point2D> points = myImageProcessor().getCurrentImagePolylinePathPoints(Roi.POINT);
		if (points == null || points.size() == 0) {
			return;
		}

		double featureWindowSize = myModel().getFeatureWindowSizeInPixels();

		for (Point2D point : points) {
			DendriteSpine spine = new DendriteSpine(point.getX(), point.getY(), featureWindowSize);
			myModel().addSpine(spine);
		}

		myModel().findNearestDendritesForAllSpines();

		myImageProcessor().setCurrentRoi(null);
		myImageProcessor().update();
	}

	private boolean warnMightRemoveClassifications() {
		DscModel model = controlPanel.getPlugin().getModel();

		if (model.getSpines().size() == 0) {
			// No spines!
			return true;
		}

		if (model.getUnclassifiedSpines().size() == model.getSpines().size()) {
			// No spines are classified, so we can't lose any data.
			return true;
		}

		String pathToImage = "images/icons/warning-icon.png";
		ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
		Image imgScaled = myIcon.getImage().getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH);

		int input = JOptionPane.showConfirmDialog(null,
				"<html><p>WARNING! This action will remove existing spine classifications.</p>"
						+ "<p>Are you sure you want to proceed?</p><br/>",
				"Clear existing spines?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(imgScaled));

		// 0=yes, 1=no
		if (input == 0) {
			return true;
		}

		return false;
	}

	private void unlockSpines() {
		boolean shouldProceed = warnMightRemoveClassifications();
		if (!shouldProceed) {
			return;
		}

		myPlugin().activateMultiPointTool();

		List<DendriteSpine> points = controlPanel.getPlugin().getModel().getSpines();

		float[] xPoints = new float[points.size()];
		float[] yPoints = new float[points.size()];

		for (int i = 0; i < points.size(); i++) {
			Point2D searchPixel = points.get(i);
			xPoints[i] = (float) searchPixel.getX();
			yPoints[i] = (float) searchPixel.getY();
		}

		PointRoi roi = new PointRoi(xPoints, yPoints);

		myModel().clearSpines();

		myImageProcessor().setCurrentRoi(roi);
		myImageProcessor().update();
	}

	private void autoDetectSpines(double contrastThresholdFrac) {
		double contrastThresholdFracEased = contrastThresholdFrac * contrastThresholdFrac / 2;

		DscImageProcessor imageProcessor = controlPanel.getPlugin().getImageProcessor();
		DscModel model = controlPanel.getPlugin().getModel();
		double featureWindowSize = model.getFeatureWindowSizeInPixels();

		model.clearSpines();
		imageProcessor.setCurrentRoi(null);

		double outerSpineCircleRadius = 1.25 * featureWindowSize;
		double innerSpineCircleRadius = .75 * featureWindowSize;

		double distOutside = featureWindowSize / 4;
		double distBetween = distOutside;

		for (DendriteBranch dendrite : controlPanel.getPlugin().getModel().getDendrites()) {
			List<Point2D> spineCandidates = dendrite.getPeripheryPoints(distOutside, distBetween);
			for (Point2D spineCandidate : spineCandidates) {
				PolygonRoi dendriteRoi = (dendrite != null) ? dendrite.getRoi() : null;

				SummaryStatistics statsOutside = imageProcessor.getBrightnessVicinityStats((int) spineCandidate.getX(),
						(int) spineCandidate.getY(), outerSpineCircleRadius, innerSpineCircleRadius, dendriteRoi);
				SummaryStatistics statsInside = imageProcessor.getBrightnessVicinityStats((int) spineCandidate.getX(),
						(int) spineCandidate.getY(), innerSpineCircleRadius, 0, dendriteRoi);

				// We'll use a very simple and dumb ersatz statistical test to see if this spot
				// should be considered to contain a spine.
				// The outside low should still be brighter than the inside high.

				double insideBrightness = statsInside.getMean();
				double outsideBrightness = statsOutside.getMean();
				double spineContrast = (outsideBrightness - insideBrightness);
				if (spineContrast < contrastThresholdFracEased) {
					continue;
				}

				DendriteSpine spine = new DendriteSpine(spineCandidate.getX(), spineCandidate.getY(),
						featureWindowSize);
				spine.contrast = spineContrast;
				model.addSpineWithOverlapImprovement(spine);
			}
		}

		model.findNearestDendritesForAllSpines();

		imageProcessor.update();
		imageProcessor.getDisplay().update();
		controlPanel.getPlugin().getImageProcessor().drawDendriteOverlays();
	}
}
