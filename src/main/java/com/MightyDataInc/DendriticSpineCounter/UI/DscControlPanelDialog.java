package com.MightyDataInc.DendriticSpineCounter.UI;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.CalibrationPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.ClassifySpinesPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.DscBasePanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.FindSpinesPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.ReportPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.SaveLoadPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.TraceDendritesPanel;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

public class DscControlPanelDialog extends JDialog {

	/**
	 * Auto-generated serialVersionUID for serialization purposes.
	 */
	private static final long serialVersionUID = -3504591526118191273L;

	private Dendritic_Spine_Counter ownerPlugin;

	private CalibrationPanel panelCalibration;
	private TraceDendritesPanel panelTraceDendrites;
	private FindSpinesPanel panelFindSpines;
	private ClassifySpinesPanel panelClassifySpines;
	private ReportPanel panelReport;
	private SaveLoadPanel panelSaveLoad;

	// --------------------------------------
	// Data-bound UI components

	private JTabbedPane tabbedPane;

	private String generateDialogBoxTitle(Dendritic_Spine_Counter plugin) {
		String title = "Dendritic Spine Counter";
		String versionStr = plugin.getApplicationVersion();
		if (!versionStr.isEmpty()) {
			title += " " + versionStr;
		}
		return title;
	}

	private String generateDialogBoxTitle() {
		return this.generateDialogBoxTitle(ownerPlugin);
	}

	public Dendritic_Spine_Counter getPlugin() {
		return this.ownerPlugin;
	}

	public JTabbedPane getTabbedPane() {
		return this.tabbedPane;
	}

	public DscControlPanelDialog(Dendritic_Spine_Counter plugin, DscModel model) {
		super((Frame) null, "Dendritic Spine Counter", false);
		ownerPlugin = plugin;

		this.setTitle(this.generateDialogBoxTitle());

		setupWindowEventHandlers();

		JPanel controlPanel = new JPanel();
		add(controlPanel);

		controlPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = standardPanelGridbagConstraints();

		{
			JLabel label = new JLabel("<html>The Dendritic Spine Counter plug-in "
					+ "helps you find, count, and classify spines on images of " + "dendritic segments.</html>");
			controlPanel.add(label, gbc);
		}

		{
			tabbedPane = new JTabbedPane();
			gbc.gridx = 0;
			gbc.gridy++;
			gbc.fill = GridBagConstraints.BOTH;
			controlPanel.add(tabbedPane, gbc);

			panelCalibration = new CalibrationPanel(this);
			tabbedPane.addTab("Calibrate size", panelCalibration);
			panelCalibration.enterPanel();

			panelTraceDendrites = new TraceDendritesPanel(this);
			tabbedPane.addTab("Trace dendrites", panelTraceDendrites);

			panelFindSpines = new FindSpinesPanel(this);
			tabbedPane.addTab("Find spines", panelFindSpines);

			panelClassifySpines = new ClassifySpinesPanel(this);
			tabbedPane.addTab("Classify spines", panelClassifySpines);

			panelReport = new ReportPanel(this);
			tabbedPane.addTab("Report results", panelReport);

			panelSaveLoad = new SaveLoadPanel(this);
			tabbedPane.addTab("Save/Load", panelSaveLoad);

			// Add a listener to tell when the active pane has been changed.
			// Quickly do whatever work is necessary before the pane appears.
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					for (int iPanel = 0; iPanel < tabbedPane.getComponentCount(); iPanel++) {
						Component panelComponent = tabbedPane.getComponentAt(iPanel);
						DscBasePanel dscPanel = null;

						try {
							dscPanel = (DscBasePanel) panelComponent;
						} catch (ClassCastException ex) {
						}

						if (dscPanel != null) {
							if (iPanel == tabbedPane.getSelectedIndex()) {
								dscPanel.enterPanel();
							} else {
								dscPanel.exitPanel();
							}
						}

					}
				}
			});
		}

		// Make the dialog pretty by setting its icon.
		{
			String pathToImage = "images/icons/dsc--find-spines-32.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
			Image img = myIcon.getImage();
			this.setIconImage(img);
		}

		// Clean up dialog's organization/layout and set its size.
		pack();
		setPreferredSize(new Dimension(800, 768));
		pack();
		setVisible(true);
	}

	/**
	 * We tend to use the same gridbag constraints for a couple of our panels, so
	 * we'll just put them into a function instead of copy-pasting everywhere.
	 * 
	 * @return Our standard initial gridbagConstraints object.
	 */
	private GridBagConstraints standardPanelGridbagConstraints() {
		GridBagConstraints gridbagConstraints = new GridBagConstraints();
		gridbagConstraints.anchor = GridBagConstraints.NORTH;
		gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridbagConstraints.weightx = 1;
		gridbagConstraints.weighty = 0;
		gridbagConstraints.gridx = GridBagConstraints.RELATIVE;
		gridbagConstraints.gridy = 0;
		gridbagConstraints.insets.bottom = 4;
		gridbagConstraints.insets.left = 4;
		gridbagConstraints.insets.right = 4;
		return gridbagConstraints;
	}

	private void setupWindowEventHandlers() {
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				update();
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
			}
		});

		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent arg0) {
			}

			@Override
			public void mouseMoved(MouseEvent arg0) {
			}
		});

		addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				update();
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}
		});

	}

	public void update() {
	}
}
