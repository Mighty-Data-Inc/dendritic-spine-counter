package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteBranch;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSegment;

import ij.IJ;
import ij.gui.Roi;

public class TraceDendritesPanel extends DscBasePanel {
	private static final long serialVersionUID = 6374646782021205583L;

	private JButton btnActivatePolylineTool;
	private JButton btnTraceCurrentPolyline;

	private JButton btnDeleteBranch;
	private JButton btnRenameBranch;

	private JList<DendriteBranch> pathListBox;
	private DefaultListModel<DendriteBranch> pathListModel;

	private DendriteBranch currentDendrite = null;

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

			btnTraceCurrentPolyline = new JButton("Use the active Polyline selection to trace a new dendrite branch",
					myIcon);
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

		{
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridwidth = 3;
			gridbagConstraints.gridy++;

			JLabel label = new JLabel(
					"<html>" + "<b>Dendrite Branches:</b> Select a branch to modify or delete." + "</html>");
			this.add(label, gridbagConstraints);

			gridbagConstraints.gridy++;

			this.pathListModel = new DefaultListModel<DendriteBranch>();
			pathListBox = new JList<DendriteBranch>(this.pathListModel);
			pathListBox.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

			JScrollPane listScroller = new JScrollPane(pathListBox);
			listScroller.setPreferredSize(new Dimension(250, 140));
			listScroller.setMinimumSize(new Dimension(250, 140));

			// https://stackoverflow.com/questions/13800775/find-selected-item-of-a-jlist-and-display-it-in-real-time
			pathListBox.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent ev) {
					if (ev.getValueIsAdjusting()) {
						// The selection is still changing. One item is being unselected
						// while the other is still being selected.
						return;
					}

					DendriteBranch dendrite = pathListBox.getSelectedValue();
					updateCurrentDendriteSelection(dendrite);
				}
			});

			this.add(listScroller, gridbagConstraints);
			gridbagConstraints.gridy++;
		}

		{
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 1;

			String pathToImage = "images/icons/dsc--delete-dendrite-path-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnDeleteBranch = new JButton("Delete Branch", myIcon);
			btnDeleteBranch.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (currentDendrite == null) {
						return;
					}
					controlPanel.getPlugin().getModel().removeDendrite(currentDendrite);
					pathListModel.removeElement(currentDendrite);
					update();
				}
			});

			this.add(btnDeleteBranch, gridbagConstraints);
			gridbagConstraints.gridx++;

			pathToImage = "images/icons/rename-24.png";
			myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnRenameBranch = new JButton("Rename Branch", myIcon);
			btnRenameBranch.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (currentDendrite == null) {
						return;
					}
					String m = JOptionPane.showInputDialog("Please enter a name for this dendrite branch",
							currentDendrite.name);
					currentDendrite.name = m;

					update();
				}
			});

			this.add(btnRenameBranch, gridbagConstraints);
			gridbagConstraints.gridy++;
		}

		addNextButton("Next: Find spines", "images/icons/dsc--find-spines-24.png");

		update();
		return this;
	}

	@Override
	public void update() {
		pathListBox.updateUI();
	}

	private void updateCurrentDendriteSelection(DendriteBranch dendrite) {
		if (dendrite == null) {
			controlPanel.getPlugin().getImageProcessor().setCurrentRoi(null);
		} else {
			// When you set the current ROI on the image processor, it actually creates
			// a new ROI. If you want the user to be able to edit the ROI and to have those
			// changes stick on the dendrite object, you need to assign the newly created
			// ROI back to the dendrite.
			Roi roiBeingEdited = controlPanel.getPlugin().getImageProcessor().setCurrentRoi(dendrite.getRoi());
			dendrite.setRoi(roiBeingEdited);
		}
		this.currentDendrite = dendrite;
	}

	@Override
	protected void onTimer() {
		List<Point2D> pathPoints = controlPanel.getPlugin().getImageProcessor().getCurrentImagePolylinePathPoints();
		boolean isThereACurrentPath = pathPoints != null;
		btnTraceCurrentPolyline.setEnabled(isThereACurrentPath);

		controlPanel.getPlugin().getImageProcessor().drawDendriteOverlays();
	}

	private void onBtnTrace() {
		List<Point2D> pathPoints = controlPanel.getPlugin().getImageProcessor().getCurrentImagePolylinePathPoints();
		if (pathPoints == null || pathPoints.size() < 2) {
			return;
		}

		DendriteBranch dendrite = controlPanel.getPlugin().getImageProcessor().traceDendriteWithThicknessEstimation();

		controlPanel.getPlugin().getModel().addDendrite(dendrite);
		this.pathListModel.addElement(dendrite);

		this.updateCurrentDendriteSelection(dendrite);
	}
}
