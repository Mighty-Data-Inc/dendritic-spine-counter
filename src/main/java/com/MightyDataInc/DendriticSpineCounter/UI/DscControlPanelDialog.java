package com.MightyDataInc.DendriticSpineCounter.UI;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
import org.json.simple.parser.JSONParser;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.CalibrationPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.ClassifySpinesPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.DscBasePanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.FindSpinesPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.ReportPanel;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.TraceDendritesPanel;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSegment;
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

	// --------------------------------------
	// Data-bound UI components

	private JTabbedPane tabbedPane;

	private JButton btnSaveDataToFile;
	private JButton btnLoadDataFromFile;

	private String getApplicationVersion(Dendritic_Spine_Counter plugin) {
		if (plugin == null || plugin.pomProjectVersion == null) {
			return "";
		}
		String versionStr = plugin.pomProjectVersion;
		if (versionStr == null) {
			return "";
		}
		return versionStr;
	}

	private String getApplicationVersion() {
		if (this.ownerPlugin == null) {
			return "";
		}
		return getApplicationVersion(this.ownerPlugin);
	}

	private String generateDialogBoxTitle(Dendritic_Spine_Counter plugin) {
		String title = "Dendritic Spine Counter";
		String versionStr = this.getApplicationVersion(plugin);
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

	/**
	 * This last panel lets users place points to mark spines, and generates a
	 * report table.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	private JPanel createFileLoadSavePanel() {
		FileFilter jsonFileFilter = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.getName().toLowerCase().endsWith(".json");
			}

			@Override
			public String getDescription() {
				return "JSON files";
			}
		};

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		gridbagConstraints.gridwidth = 1;
		{
			JLabel label = new JLabel("<html>" + "<p>Dendritic Spine Counter stores dendrite segments along with "
					+ "their associated spines in JSON format.</p>" + "</html>");
			// We want extra space at the bottom of this label.
			gridbagConstraints.insets.bottom = 16;
			panel.add(label, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.insets.bottom = 8;
			gridbagConstraints.gridwidth = 1;
		}

		{
			String pathToImage = "images/icons/file-save-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnSaveDataToFile = new JButton("Save data to file", myIcon);
			panel.add(btnSaveDataToFile, gridbagConstraints);

			btnSaveDataToFile.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setFileFilter(jsonFileFilter);
					int result = fileChooser.showSaveDialog(null);
					if (result != JFileChooser.APPROVE_OPTION) {
						return;
					}

					JSONObject json = new JSONObject();
					json.put("version", getApplicationVersion());

					try {
						json.put("originalimagefile", ownerPlugin.getOriginalImage().getImgPlus().getSource());
					} catch (Exception e1) {
					}

					// json.put("featuresizepixels", getFeatureDetectionWindowSizeInPixels());
//					json.put("researcher", textfieldResultTableResearcher.getText().trim());
//					json.put("imagedesignation", textfieldResultTableImageDesignation.getText().trim());
//					json.put("customlabel", textfieldResultTableImageCustomLabel.getText().trim());

					JSONArray jsonDends = new JSONArray();
//					for (Object dendriteObj : pathListModel.toArray()) {
//						JSONObject jsonDend = ((DendriteSegment) dendriteObj).toJSON();
//						jsonDends.add(jsonDend);
//					}

					json.put("dendrites", jsonDends);

					String filename = fileChooser.getSelectedFile().getAbsolutePath();
					if (!filename.toLowerCase().endsWith(".json")) {
						filename += ".json";
					}
					try {
						FileWriter writer = new FileWriter(filename);
						writer.write(json.toJSONString());
						writer.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});

			gridbagConstraints.gridy++;
		}

		{
			String pathToImage = "images/icons/file-load-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnLoadDataFromFile = new JButton("Load data from file", myIcon);
			panel.add(btnLoadDataFromFile, gridbagConstraints);

			btnLoadDataFromFile.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setFileFilter(jsonFileFilter);
					int result = fileChooser.showOpenDialog(null);
					if (result != JFileChooser.APPROVE_OPTION) {
						return;
					}

					String filename = fileChooser.getSelectedFile().getAbsolutePath();
					JSONObject jsonObj = getJsonObjectFromFile(filename);
					if (jsonObj == null) {
						return;
					}
					loadFromJsonObject(jsonObj);
				}
			});

			gridbagConstraints.gridy++;
		}

		addEmptySpaceFillerLabel(panel, gridbagConstraints);

		return panel;
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

	private void addEmptySpaceFillerLabel(JPanel panel, GridBagConstraints gridbagConstraints) {
		JLabel emptyLabel = new JLabel(" ");
		gridbagConstraints.insets.top = 20;
		gridbagConstraints.insets.bottom = 8;
		gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
		gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
		gridbagConstraints.weighty = 1.0;
		panel.add(emptyLabel, gridbagConstraints);
	}

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

	public void loadFromJsonObject(JSONObject jsonObj) {
		Object v = jsonObj.get("featuresizepixels");

		// this.enumFeatureDetectionWindowSizeUnits =
		// FeatureDetectionWindowSizeUnitsEnum.PIXELS;
		// this.textfieldFeatureDetectionWindowSize.setText(String.format(v.toString()));

//		v = jsonObj.get("researcher");
//		this.textfieldResultTableResearcher.setText(String.format(v.toString()));
//
//		v = jsonObj.get("imagedesignation");
//		this.textfieldResultTableImageDesignation.setText(String.format(v.toString()));
//
//		v = jsonObj.get("customlabel");
//		this.textfieldResultTableImageCustomLabel.setText(String.format(v.toString()));

		JSONArray jsonDends = (JSONArray) jsonObj.get("dendrites");
		List<Point2D> allspines = new ArrayList<Point2D>();
		for (int iDend = 0; iDend < jsonDends.size(); iDend++) {
			JSONObject jsonDend = (JSONObject) jsonDends.get(iDend);
			DendriteSegment dendrite = new DendriteSegment();
			dendrite.fromJSON(jsonDend, ownerPlugin.getImageProcessor().workingImg);

//			this.pathListModel.addElement(dendrite);
//			// ownerPlugin.getImageProcessor().addPathToDrawOverlay(dendrite);

			allspines.addAll(dendrite.spines);
		}

		// Add all the spines as visible ROI points in one big blast.
		// ownerPlugin.getImageProcessor().AddPointRoisAsSpineMarkers(allspines);

//		populateResultsTable();

		update();
		ownerPlugin.getImageProcessor().update();
	}
}
