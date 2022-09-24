package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSegment;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

import ij.Executer;
import ij.IJ;
import ij.gui.Roi;
import ij.measure.Calibration;

public class TraceDendritesPanel extends DscBasePanel {
	private static final long serialVersionUID = 6374646782021205583L;

	private JButton btnActivatePolylineTool;
	private JButton btnTraceCurrentPolyline;

	public TraceDendritesPanel(DscControlPanelDialog controlPanel) {
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
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		gridbagConstraints.gridwidth = 3;
		gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

		{
			JLabel label = new JLabel("<html>" + "Use the Polyline tool to trace a dendrite segment. "
					+ "Your trace doesn't need to follow the dendrite precisely at first. "
					+ "You'll have the chance to refine the trace later." + "</html>");
			this.add(label, gridbagConstraints);

			gridbagConstraints.insets.bottom = 4;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			ImageIcon icon = null;
			PluginInfo<?> polylineInfo = controlPanel.getPlugin().getPolylineTool().getInfo();
			String iconDescription = polylineInfo.getDescription();
			try {
				URL iconURL = polylineInfo.getIconURL();
				icon = new ImageIcon(iconURL, iconDescription);
			} catch (InstantiableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx++;

			btnActivatePolylineTool = new JButton("Activate Polyline Tool to add a new path", icon);
			btnActivatePolylineTool.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					controlPanel.getPlugin().getImageProcessor().getImagePlus().setRoi((Roi) null);

					IJ.setTool("polyline");
					update();

					controlPanel.getPlugin().getImageProcessor().update();
					controlPanel.getPlugin().getImageProcessor().moveToForeground();
				}
			});
			this.add(btnActivatePolylineTool, gridbagConstraints);

			String pathToImage = "images/icons/dsc--trace-dendrite-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnTraceCurrentPolyline = new JButton("Use the active Polyline Path to trace dendrite", myIcon);
			btnTraceCurrentPolyline.setEnabled(false);

			btnTraceCurrentPolyline.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onBtnTrace();
				}
			});
			gridbagConstraints.gridx++;
			this.add(btnTraceCurrentPolyline, gridbagConstraints);

		}

		addNextButton("Next: Find spines", "images/icons/dsc--find-spines-24.png");

		update();
		return this;
	}

	@Override
	public void update() {
	}

	@Override
	protected void onTimer() {
		List<Point2D> pathPoints = getCurrentActivePolylinePathPoints();
		boolean isThereACurrentPath = pathPoints != null;
		btnTraceCurrentPolyline.setEnabled(isThereACurrentPath);
	}

	private List<Point2D> getCurrentActivePolylinePathPoints() {
		List<Point2D> pathPoints = controlPanel.getPlugin().getImageProcessor().getCurrentImagePolylinePathPoints();
		return pathPoints;
	}

	private void onBtnTrace() {
		List<Point2D> pathPoints = getCurrentActivePolylinePathPoints();
		if (pathPoints == null || pathPoints.size() < 2) {
			return;
		}

		DendriteSegment dendritePath = controlPanel.getPlugin().getImageProcessor()
				.traceDendriteWithThicknessEstimation();
		// pathListModel.addElement(dendritePath);
		controlPanel.getPlugin().getImageProcessor().AddPathToDrawOverlay(dendritePath);
		controlPanel.getPlugin().getImageProcessor().update();
	}
}
