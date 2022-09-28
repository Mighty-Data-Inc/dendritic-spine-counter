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

import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;

import ij.IJ;
import ij.gui.PointRoi;
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
					controlPanel.getPlugin().getImageProcessor().setCurrentRoi(null);
					IJ.setTool("multi-point");
					update();
					controlPanel.getPlugin().getImageProcessor().moveToForeground();
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
//					int pixelWindowSize = 5; // getFeatureDetectionWindowSizeInPixels();
//					int SCANSPAN = 2;
//
//					int sensitivitySliderVal = sliderDetectionSensitivity.getValue();
//					double sensitivity = (100.0 - (double) sensitivitySliderVal) / 50.0;
//					sensitivity *= sensitivity;
//					sensitivity *= sensitivity;
//					sensitivity *= 0.25;
//					// The "sensitivity" is actually kinda backwards.
//					// It needs an easing function to mean what the labeling says it means.
//
//					List<Point2D> spines = new ArrayList<Point2D>();
//
//					Object[] paths = pathListModel.toArray();
//					for (Object path : paths) {
//						DendriteSegment dendriteSegment = (DendriteSegment) path;
//
//						for (PathSide side : PathSide.values()) {
//							DendriteSegment sidepath = dendriteSegment.createSidePath(side, pixelWindowSize,
//									pixelWindowSize / 2);
//
//							List<Point2D> spinesHere = sidepath.findSpinesAlongSidepath(pixelWindowSize, SCANSPAN,
//									sensitivity);
//
//							spines.addAll(spinesHere);
//						}
//					}
//					controlPanel.getPlugin().getImageProcessor().AddPointRoisAsSpineMarkers(spines);
				}
			});
			this.add(btnDetectSpines, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>You can adjust the contrast threshold that this plug-in "
					+ "uses for automatic spine detection. <br/>"
					+ "Low contrast threshold may end up mis-identifying noise as spines (Type I error). <br/>"
					+ "High contrast threshold means that some faint spines might get missed (Type II error).<br/><br/>Set constrast threshold:</html>");
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
		if (controlPanel.getPlugin().getCurrentToolName() == "multipoint") {
			this.btnActivateMultiPointTool.setEnabled(false);
		} else {
			this.btnActivateMultiPointTool.setEnabled(true);
		}

		List<Point2D> points = controlPanel.getPlugin().getImageProcessor()
				.getCurrentImagePolylinePathPoints(Roi.POINT);
		if (points == null || points.size() == 0) {
			this.btnLockSpines.setEnabled(false);
		} else {
			this.btnLockSpines.setEnabled(true);
		}

		if (controlPanel.getPlugin().getModel().getSpines().size() > 0) {
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
		if (controlPanel.getPlugin().getCurrentToolName() == "multipoint") {
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
						"Lock Multi-point Selection?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
						new ImageIcon(imgScaled));

				// 0=yes, 1=no
				if (input == 0) {
					this.lockSpines();
				}
			}
		}
	}

	private void lockSpines() {
		List<Point2D> points = controlPanel.getPlugin().getImageProcessor()
				.getCurrentImagePolylinePathPoints(Roi.POINT);
		if (points == null || points.size() == 0) {
			return;
		}

		for (Point2D point : points) {
			DendriteSpine spine = new DendriteSpine(point.getX(), point.getY(),
					controlPanel.getPlugin().getModel().getFeatureWindowSizeInPixels());
			controlPanel.getPlugin().getModel().addSpine(spine);
		}

		controlPanel.getPlugin().getModel().findNearestDendritesForAllSpines();

		controlPanel.getPlugin().getImageProcessor().setCurrentRoi(null);
		controlPanel.getPlugin().getImageProcessor().update();
	}

	private boolean warnMightRemoveClassifications() {
		if (controlPanel.getPlugin().getModel().getSpines().size() == 0) {
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

		List<DendriteSpine> points = controlPanel.getPlugin().getModel().getSpines();

		float[] xPoints = new float[points.size()];
		float[] yPoints = new float[points.size()];

		for (int i = 0; i < points.size(); i++) {
			Point2D searchPixel = points.get(i);
			xPoints[i] = (float) searchPixel.getX();
			yPoints[i] = (float) searchPixel.getY();
		}

		PointRoi roi = new PointRoi(xPoints, yPoints);

		controlPanel.getPlugin().getModel().clearSpines();

		controlPanel.getPlugin().getImageProcessor().setCurrentRoi(roi);
		controlPanel.getPlugin().getImageProcessor().update();
	}
}
