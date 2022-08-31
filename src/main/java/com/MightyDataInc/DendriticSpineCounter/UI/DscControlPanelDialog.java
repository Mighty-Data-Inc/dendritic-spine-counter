package com.MightyDataInc.DendriticSpineCounter.UI;

import java.awt.Cursor;
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import ij.Executer;
import ij.ImagePlus;
import ij.IJ;
import ij.gui.Roi;
import ij.measure.Calibration;
import net.imagej.display.OverlayService;

import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.UI.tabpanels.CalibrationPanel;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSegment;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;
import com.MightyDataInc.DendriticSpineCounter.model.SearchPixel;
import com.MightyDataInc.DendriticSpineCounter.model.SearchPixel.PathSide;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
import org.json.simple.parser.JSONParser;

public class DscControlPanelDialog extends JDialog {

	/**
	 * Auto-generated serialVersionUID for serialization purposes.
	 */
	private static final long serialVersionUID = -3504591526118191273L;

	public Dendritic_Spine_Counter ownerPlugin;

	private CalibrationPanel panelCalibration;

	// --------------------------------------
	// Data-bound UI components

	private JTabbedPane tabbedPane;

	private JButton btnActivatePolylineTool;
	private JButton btnTraceCurrentPolyline;
	private JButton btnActivateMultiPointTool;
	private JButton btnDetectSpines;

	private JSlider sliderDetectionSensitivity;

	private JButton btnDeleteBranch;
	private JButton btnRenameBranch;
	private JButton btnNextSegment;
	private JButton btnPrevSegment;
	private JButton btnMakeSegmentWider;
	private JButton btnMakeSegmentNarrower;
	private JButton btnShiftSegmentLeft;
	private JButton btnShiftSegmentRight;
	private JButton btnCopyTableDataToClipboard;

	private JButton btnSaveDataToFile;
	private JButton btnLoadDataFromFile;

	private JCheckBox chkIncludeHeadersInCopyPaste;

	private JTextField textfieldResultTableResearcher;
	private JTextField textfieldResultTableImageDesignation;
	private JTextField textfieldResultTableImageCustomLabel;

	private JLabel lblImageResolution;

	private JList<DendriteSegment> pathListBox;
	private DefaultListModel<DendriteSegment> pathListModel;
	private int pathSegmentIndexSelected;

	private JTable resultsTable;
	private JScrollPane resultsTableHolder;
	private Object[][] resultsTableData = new Object[0][4];
	private String[] resultsTableColumns = { "Dendrite Segment", "Length", "Avg Width", "Spine Count",
			"Spine Density" };

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

	public DscControlPanelDialog(Dendritic_Spine_Counter plugin, DscModel model) {
		super((Frame) null, "Dendritic Spine Counter", false);
		ownerPlugin = plugin;

		model.dialog = this;

		this.setTitle(this.generateDialogBoxTitle());

		this.pathListModel = new DefaultListModel<DendriteSegment>();

		setupWindowEventHandlers();

		JPanel controlPanel = new JPanel();
		add(controlPanel);

		controlPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = standardPanelGridbagConstraints();

		{
			JLabel label = new JLabel("<html>" + "The Dendritic Spine Counter plug-in counts spines along "
					+ "segments of dendrite that you specify.<br/>" + "</html>");
			controlPanel.add(label, gbc);
		}

		{
			tabbedPane = new JTabbedPane();
			gbc.gridx = 0;
			gbc.gridy++;
			gbc.fill = GridBagConstraints.BOTH;
			controlPanel.add(tabbedPane, gbc);

			panelCalibration = new CalibrationPanel(tabbedPane, ownerPlugin, model);
			tabbedPane.addTab("Calibrate size", panelCalibration);

			JPanel panel2 = createPathInputSpecificationPanel();
			tabbedPane.addTab("Trace dendrites", panel2);

			JPanel panel3 = createSpineSelectionPanel();
			tabbedPane.addTab("Find spines", panel3);

			JPanel panel4 = createSpineClassificationPanel();
			tabbedPane.addTab("Classify spines", panel4);

			JPanel panel5 = createReportPanel();
			tabbedPane.addTab("Report results", panel5);

			JPanel panel6 = createFileLoadSavePanel();
			tabbedPane.addTab("Save/Load", panel6);

			// Add a listener to tell when the active pane has been changed.
			// Quickly do whatever work is necessary before the pane appears.
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					// If we're about to view the "Report results" pane,
					// quickly generate the results!
					if (tabbedPane.getSelectedIndex() == 4) {
						countSpinesAndBuildTable();
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
	 * This last panel lets users place points to mark spines, and generates a
	 * report table.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	private JPanel createReportPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		gridbagConstraints.gridwidth = 2;

		{
			JLabel label = new JLabel("<html>" + "<p>This plug-in automatically "
					+ "goes through all of the spines you've currently selected with "
					+ "the Multi-point Tool. It will associate each spine with its "
					+ "nearest dendrite segment, and tabulate statistics about "
					+ "the spine counts and densities for each dendrite segment.</p>" + "</html>");
			gridbagConstraints.insets.bottom = 8;
			panel.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.bottom = 4;
		}

		{
			this.resultsTableHolder = new JScrollPane();

			resultsTableHolder.setPreferredSize(new Dimension(250, 160));
			resultsTableHolder.setMinimumSize(new Dimension(250, 160));

			panel.add(resultsTableHolder, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>" + "<p>You can copy this tab-delimited table data to your clipboard, and "
					+ "paste it directly into spreadsheet software (e.g. Microsoft Excel).</p><br/>"
					+ "<p>If you wish, you may optionally specify additional labels, so that "
					+ "if you make multiple copy-pastes into the same spreadsheet "
					+ "then your data will retain annotations of your choosing. These labels "
					+ "will appear as additional columns in your copy-pasted data, with every "
					+ "row bearing the value you've entered.</p>" + "</html>");
			gridbagConstraints.insets.bottom = 8;
			panel.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.bottom = 4;
			gridbagConstraints.gridwidth = 1;
		}

		{
			JLabel label = new JLabel("<html>Optional column: Image designation label</html>");
			panel.add(label, gridbagConstraints);
			gridbagConstraints.gridx++;

			textfieldResultTableImageDesignation = new JTextField();
			panel.add(textfieldResultTableImageDesignation, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>Optional column: Researcher</html>");
			panel.add(label, gridbagConstraints);
			gridbagConstraints.gridx++;

			textfieldResultTableResearcher = new JTextField();
			panel.add(textfieldResultTableResearcher, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>Optional column: Custom label</html>");
			panel.add(label, gridbagConstraints);
			gridbagConstraints.gridx++;

			textfieldResultTableImageCustomLabel = new JTextField();
			panel.add(textfieldResultTableImageCustomLabel, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			chkIncludeHeadersInCopyPaste = new JCheckBox("Copy with headers");
			panel.add(chkIncludeHeadersInCopyPaste, gridbagConstraints);
			chkIncludeHeadersInCopyPaste.setSelected(true);

			gridbagConstraints.gridx++;

			String pathToImage = "images/icons/copy.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnCopyTableDataToClipboard = new JButton("Copy table data to clipboard", myIcon);
			gridbagConstraints.insets.bottom = 8;
			panel.add(btnCopyTableDataToClipboard, gridbagConstraints);

			btnCopyTableDataToClipboard.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					copyResultsTableToClipboard();
				}
			});

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		addEmptySpaceFillerLabel(panel, gridbagConstraints);

		return panel;
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
						json.put("originalimagefile", ownerPlugin.origDataset.getImgPlus().getSource());
					} catch (Exception e1) {
					}

					json.put("featuresizepixels", getFeatureDetectionWindowSizeInPixels());
					json.put("researcher", textfieldResultTableResearcher.getText().trim());
					json.put("imagedesignation", textfieldResultTableImageDesignation.getText().trim());
					json.put("customlabel", textfieldResultTableImageCustomLabel.getText().trim());

					JSONArray jsonDends = new JSONArray();
					for (Object dendriteObj : pathListModel.toArray()) {
						JSONObject jsonDend = ((DendriteSegment) dendriteObj).toJSON();
						jsonDends.add(jsonDend);
					}

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

	/**
	 * This panel lets users place points to mark spines, and generates a report
	 * table.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	private JPanel createSpineSelectionPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		{
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

			JLabel label = new JLabel(
					"<html>" + "Use the Multi-point Tool to mark spines. " + "Dendritic Spine Counter "
							+ "will automatically associate each spine with whichever dendrite segment "
							+ "is closest to it." + "<ul>" + "<li>Click within the image to mark a spine.</li>"
							+ "<li>Click and hold a marked spine (drag it) to relocate it.</li>"
							+ "<li>Alt-Click a marked spine to remove it.</li>" + "</ul>"
							+ "<p>Spines marked with this tool are \"tentative\", i.e. they are "
							+ "considered a Multi-point selection, and can be added, moved, or deleted "
							+ "as you see fit. You will have the chance to tabulate them in the "
							+ "\"Report results\" tab." + "</html>");
			panel.add(label, gridbagConstraints);

			// Because we're using the same gridbagConstraints object for subsequent
			// UI elements in this panel, let's set the bottom space value back to
			// our standard.
			gridbagConstraints.insets.bottom = 4;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			ImageIcon icon = null;
			PluginInfo<?> pointToolInfo = ownerPlugin.pointTool.getInfo();
			String iconDescription = pointToolInfo.getDescription();
			try {
				URL iconURL = pointToolInfo.getIconURL();
				icon = new ImageIcon(iconURL, iconDescription);
			} catch (InstantiableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			btnActivateMultiPointTool = new JButton("Activate Multi-point Tool to mark spines on the image", icon);
			btnActivateMultiPointTool.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					IJ.setTool("multi-point");
					update();
					ownerPlugin.imageProcessor.moveToForeground();
				}
			});
			panel.add(btnActivateMultiPointTool, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel(
					"<html>" + "This plug-in can auto-detect spines that project outward from the edges of "
							+ "your dendrite segments. You can adjust the contrast sensitivity that this plug-in "
							+ "uses for this task. Low contrast sensitivity means that a spine needs to be "
							+ "significantly darker than its background in order to be recognized, "
							+ "while high contrast sensitivity may try to mark a feature as a spine even if "
							+ "it is only slightly darker than its background.<br/<br/>"
							+ "After spines are automatically detected, you will then have the ability to move, "
							+ "add, or delete them as you see fit.<br/<br/>"
							+ "NOTE: Using this function will clear your current selection and replace it with "
							+ "the auto-detected features. If you're going to use this feature, you should "
							+ "use it <i>first</i>, and <i>then</i> add more spines manually if needed." + "</html>");
			// We want extra space at the bottom of this label.
			panel.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			String pathToImage = "images/icons/dsc--find-spines-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnDetectSpines = new JButton("Automatically detect spines on traced dendrites", myIcon);
			btnDetectSpines.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int pixelWindowSize = getFeatureDetectionWindowSizeInPixels();
					int SCANSPAN = 2;

					int sensitivitySliderVal = sliderDetectionSensitivity.getValue();
					double sensitivity = (100.0 - (double) sensitivitySliderVal) / 50.0;
					sensitivity *= sensitivity;
					sensitivity *= sensitivity;
					sensitivity *= 0.25;
					// The "sensitivity" is actually kinda backwards.
					// It needs an easing function to mean what the labeling says it means.

					List<Point2D> spines = new ArrayList<Point2D>();

					Object[] paths = pathListModel.toArray();
					for (Object path : paths) {
						DendriteSegment dendriteSegment = (DendriteSegment) path;

						for (PathSide side : PathSide.values()) {
							DendriteSegment sidepath = dendriteSegment.createSidePath(side, pixelWindowSize,
									pixelWindowSize / 2);

							List<Point2D> spinesHere = sidepath.findSpinesAlongSidepath(pixelWindowSize, SCANSPAN,
									sensitivity);

							spines.addAll(spinesHere);
						}
					}
					ownerPlugin.imageProcessor.AddPointRoisAsSpineMarkers(spines);
				}
			});
			panel.add(btnDetectSpines, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>" + "Contrast sensitivity (% brightness difference)" + "</html>");
			// We want extra space at the bottom of this label.
			gridbagConstraints.insets.top = 16;
			panel.add(label, gridbagConstraints);

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
			panel.add(sliderDetectionSensitivity, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			String pathToImage = "images/icons/dsc--classify-spines-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnNext = new JButton("Next: Classify Spines", myIcon);

			btnNext.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(3);
				}
			});
			gridbagConstraints.insets.top = 20;
			gridbagConstraints.insets.bottom = 8;
			gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
			gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
			gridbagConstraints.weighty = 1.0;
			panel.add(btnNext, gridbagConstraints);
		}

		return panel;
	}

	/**
	 * This panel lets users place points to mark spines, and generates a report
	 * table.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	private JPanel createSpineClassificationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		{
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

			JLabel label = new JLabel("<html>"
					+ "In this panel you will categorize spines into classes based on their morphologies. "
					+ "In the previous step, you created a Multi-point Tool selection to locate spines on the image. "
					+ "This tool will now help you go through the selected spines one by one, measure "
					+ "their features, and decide the best class to fit each one into. " + "</html>");
			panel.add(label, gridbagConstraints);

			gridbagConstraints.insets.bottom = 4;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			String pathToImage = "images/icons/data-table-results-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnNext = new JButton("Next: Report Results", myIcon);

			btnNext.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(4);
				}
			});
			gridbagConstraints.insets.top = 20;
			gridbagConstraints.insets.bottom = 8;
			gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
			gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
			gridbagConstraints.weighty = 1.0;
			panel.add(btnNext, gridbagConstraints);
		}

		return panel;
	}

	/**
	 * When the dialog first pops up, the user is given the chance to specify how
	 * they will input the path that will eventually be followed to trace the
	 * dendrite. This is done through an introductory text and a couple of buttons.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	private JPanel createPathInputSpecificationPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		// panel.setBorder(BorderFactory.createLineBorder(getForeground()));

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		gridbagConstraints.gridwidth = 2;
		gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

		{
			JLabel label = new JLabel("<html>" + "Use the Polyline tool to trace a dendrite segment. "
					+ "Your trace doesn't need to follow the dendrite precisely at first. "
					+ "You'll have the chance to refine the trace later." + "</html>");
			// We want extra space at the bottom of this label.
			panel.add(label, gridbagConstraints);

			// Because we're using the same gridbagConstraints object for subsequent
			// UI elements in this panel, let's set the bottom space value back to
			// our standard.
			gridbagConstraints.insets.bottom = 4;
			gridbagConstraints.gridy++;
		}

		{
			ImageIcon icon = null;
			PluginInfo<?> polylineInfo = ownerPlugin.polylineTool.getInfo();
			String iconDescription = polylineInfo.getDescription();
			try {
				URL iconURL = polylineInfo.getIconURL();
				icon = new ImageIcon(iconURL, iconDescription);
			} catch (InstantiableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			btnActivatePolylineTool = new JButton("Activate Polyline Tool to add a new path", icon);
			btnActivatePolylineTool.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ownerPlugin.imageProcessor.getImagePlus().setRoi((Roi) null);
					ownerPlugin.imageProcessor.update();

					IJ.setTool("polyline");
					update();
					ownerPlugin.imageProcessor.update();
				}
			});
			panel.add(btnActivatePolylineTool, gridbagConstraints);
			gridbagConstraints.gridy++;
		}

		{
			String pathToImage = "images/icons/dsc--trace-dendrite-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnTraceCurrentPolyline = new JButton("Use existing polyline path to trace dendrite", myIcon);

			btnTraceCurrentPolyline.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DendriteSegment dendritePath = ownerPlugin.imageProcessor.traceDendriteWithThicknessEstimation(.8,
							ownerPlugin.MAX_SEARCH_DISTANCE_IN_PIXEL_WINDOW_SIZE_TIMES, null);
					pathListModel.addElement(dendritePath);
					ownerPlugin.imageProcessor.AddPathToDrawOverlay(dendritePath);
					ownerPlugin.imageProcessor.update();
				}
			});
			panel.add(btnTraceCurrentPolyline, gridbagConstraints);
			gridbagConstraints.gridy++;
		}
		{
			JLabel label = new JLabel(
					"<html>" + "<b>Dendrite Branches:</b> Select a branch to modify or delete." + "</html>");
			panel.add(label, gridbagConstraints);
			gridbagConstraints.gridy++;

			this.pathListBox = new JList<DendriteSegment>(this.pathListModel);
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

					// Unselect all items first.
					Object[] paths = pathListModel.toArray();
					for (Object path : paths) {
						ownerPlugin.imageProcessor.SelectPath((DendriteSegment) path, false);
					}

					// Now select the one path that's actually selected.
					DendriteSegment path = pathListBox.getSelectedValue();
					ownerPlugin.imageProcessor.SelectPath(path, true);
					pathSegmentIndexSelected = 0;
					if (path != null && path.path != null && path.path.size() > 0) {
						ownerPlugin.imageProcessor.SetSelectedSegmentCursor(path.path.get(0),
								ownerPlugin.featureSizePixels);
					} else {
						ownerPlugin.imageProcessor.SetSelectedSegmentCursor(null, 0);
					}

					update();
					ownerPlugin.imageProcessor.update();
				}
			});

			panel.add(listScroller, gridbagConstraints);
			gridbagConstraints.gridy++;
		}
		{

			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			{
				String pathToImage = "images/icons/dsc--delete-dendrite-path-24.png";
				ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnDeleteBranch = new JButton("Delete Branch", myIcon);
				btnDeleteBranch.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						DendriteSegment selectedPath = pathListBox.getSelectedValue();
						if (selectedPath == null) {
							return;
						}
						pathListModel.removeElement(selectedPath);
						ownerPlugin.imageProcessor.RemovePathFromDrawOverlay(selectedPath);
						pathSegmentIndexSelected = 0;
						updateSelectedSegment();

						// Now we delete the spines that were on the deleted segment.
						// Unfortunately, setting the spine selection is kinda an
						// all-or-nothing operation, so we need to gather all
						// spines from all OTHER paths and create a new ROI from them.
						// pathListModel isn't iterable so we'll do this the old fashioned way.
						List<Point2D> spinesRemaining = new ArrayList<Point2D>();
						for (int iDend = 0; iDend < pathListModel.getSize(); iDend++) {
							DendriteSegment dendSegment = pathListModel.get(iDend);
							spinesRemaining.addAll(dendSegment.spines);
						}
						ownerPlugin.imageProcessor.AddPointRoisAsSpineMarkers(spinesRemaining);
						associateSpinesWithDendriteSegments(spinesRemaining);

						update();
						ownerPlugin.imageProcessor.update();
					}
				});

				panel.add(btnDeleteBranch, gridbagConstraints);
				gridbagConstraints.gridx++;
			}

			{
				String pathToImage = "images/icons/rename-24.png";
				ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnRenameBranch = new JButton("Rename Branch", myIcon);
				btnRenameBranch.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						DendriteSegment selectedPath = pathListBox.getSelectedValue();
						if (selectedPath == null) {
							return;
						}
						String m = JOptionPane.showInputDialog("Please enter a name for this dendrite branch",
								selectedPath.name);
						selectedPath.name = m;

						pathListBox.updateUI();
					}
				});

				panel.add(btnRenameBranch, gridbagConstraints);
				gridbagConstraints.gridy++;
			}

			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.gridx = 0;
			{
				JLabel label = new JLabel("<html>" + "To edit the width of the selected dendrite segment at any point, "
						+ "use the controls below to move the highlighted region forward and "
						+ "back along the segment and adjust accordingly." + "</html>");
				panel.add(label, gridbagConstraints);
				gridbagConstraints.gridy++;
			}

			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			{
				String pathToImage = "images/icons/dsc--region-back.png";
				ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnPrevSegment = new JButton("< Region Back", myIcon);
				btnPrevSegment.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						pathSegmentIndexSelected--;
						updateSelectedSegment();
					}
				});

				panel.add(btnPrevSegment, gridbagConstraints);
				gridbagConstraints.gridx++;

				pathToImage = "images/icons/dsc--region-fwd.png";
				myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnNextSegment = new JButton("Region Fwd >", myIcon);
				btnNextSegment.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						pathSegmentIndexSelected++;
						updateSelectedSegment();
					}
				});
				panel.add(btnNextSegment, gridbagConstraints);
				gridbagConstraints.gridx++;
			}
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;
		}

		{
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			{
				String pathToImage = "images/icons/dsc--thinnen-24.png";
				ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnMakeSegmentNarrower = new JButton("- Region Thinner", myIcon);
				btnMakeSegmentNarrower.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						changeSelectedSegmentThickness(-1.0);
					}
				});

				panel.add(btnMakeSegmentNarrower, gridbagConstraints);
				gridbagConstraints.gridx++;

				pathToImage = "images/icons/dsc--widen-24.png";
				myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnMakeSegmentWider = new JButton("Region Thicker +", myIcon);
				btnMakeSegmentWider.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						changeSelectedSegmentThickness(2.0);
					}
				});
				panel.add(btnMakeSegmentWider, gridbagConstraints);
				gridbagConstraints.gridx++;
			}
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;
		}

		{
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			{
				String pathToImage = "images/icons/dsc--region-shift-left.png";
				ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnShiftSegmentLeft = new JButton("Region Shift Left", myIcon);
				btnShiftSegmentLeft.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						shiftSelectedSegmentPosition(-1.0);
					}
				});

				panel.add(btnShiftSegmentLeft, gridbagConstraints);
				gridbagConstraints.gridx++;

				pathToImage = "images/icons/dsc--region-shift-right.png";
				myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

				btnShiftSegmentRight = new JButton("Region Shift Right", myIcon);
				btnShiftSegmentRight.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						shiftSelectedSegmentPosition(1.0);
					}
				});
				panel.add(btnShiftSegmentRight, gridbagConstraints);
				gridbagConstraints.gridx++;
			}
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;
		}

		{
			String pathToImage = "images/icons/dsc--find-spines-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnNext = new JButton("Next: Find spines", myIcon);

			btnNext.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(2);
				}
			});
			gridbagConstraints.insets.top = 20;
			gridbagConstraints.insets.bottom = 8;
			gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
			gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
			gridbagConstraints.weighty = 1.0;
			panel.add(btnNext, gridbagConstraints);
		}

		return panel;
	}

	public void countSpinesAndBuildTable() {
		List<Point2D> points = ownerPlugin.imageProcessor.getPointsFromCurrentPolylineRoiSelection();
		clearSpineAssociations();
		associateSpinesWithDendriteSegments(points);
		populateResultsTable();
		update();
	}

	public void updateSelectedSegment() {
		DendriteSegment selectedBranch = this.pathListBox.getSelectedValue();
		if (selectedBranch == null) {
			this.pathSegmentIndexSelected = 0;
			ownerPlugin.imageProcessor.SetSelectedSegmentCursor(null, 0);
			return;
		}

		if (this.pathSegmentIndexSelected < 0) {
			this.pathSegmentIndexSelected = 0;
		}
		if (this.pathSegmentIndexSelected > selectedBranch.path.size() - 1) {
			this.pathSegmentIndexSelected = selectedBranch.path.size() - 1;
		}

		SearchPixel pix = selectedBranch.path.get(pathSegmentIndexSelected);
		ownerPlugin.imageProcessor.SetSelectedSegmentCursor(pix, ownerPlugin.featureSizePixels);
		this.update();
	}

	public void changeSelectedSegmentThickness(double changeAmt) {
		changeAmt *= this.getFeatureDetectionWindowSizeInPixels() / 2;

		DendriteSegment selectedBranch = this.pathListBox.getSelectedValue();
		if (selectedBranch == null) {
			return;
		}

		SearchPixel pix = selectedBranch.path.get(pathSegmentIndexSelected);
		if (pix == null) {
			return;
		}

		// Left side is negative.
		pix.similarityBoundaryDistanceLeft -= changeAmt;
		pix.similarityBoundaryDistanceLeft = Math.min(-1.0, pix.similarityBoundaryDistanceLeft);

		pix.similarityBoundaryDistanceRight += changeAmt;
		pix.similarityBoundaryDistanceRight = Math.max(1.0, pix.similarityBoundaryDistanceRight);

		selectedBranch.smoothSimilarityBoundaryDistances(.5);

		int oldBranchId = selectedBranch.id;

		ownerPlugin.imageProcessor.RemovePathFromDrawOverlay(selectedBranch);
		selectedBranch.roi = selectedBranch.getSimilarityVolume();

		ownerPlugin.imageProcessor.AddPathToDrawOverlay(selectedBranch);
		selectedBranch.id = oldBranchId;

		ownerPlugin.imageProcessor.SelectPath(selectedBranch, true);
		ownerPlugin.imageProcessor.SetSelectedSegmentCursor(pix, ownerPlugin.featureSizePixels);
	}

	public void shiftSelectedSegmentPosition(double changeAmt) {
		DendriteSegment selectedBranch = this.pathListBox.getSelectedValue();
		if (selectedBranch == null) {
			return;
		}

		changeAmt *= this.getFeatureDetectionWindowSizeInPixels() / 2;

		// Create some smothing/easing behavior.
		int INDEX_SPAN = 3;

		for (int iSpan = -INDEX_SPAN; iSpan <= INDEX_SPAN; iSpan++) {
			int iPix = pathSegmentIndexSelected + iSpan;
			if (iPix < 0 || iPix >= selectedBranch.path.size()) {
				continue;
			}

			double shiftDist = changeAmt * (double) (INDEX_SPAN - Math.abs(iSpan)) / (double) INDEX_SPAN;

			SearchPixel pix = selectedBranch.path.get(iPix);
			if (pix == null) {
				return;
			}

			pix.x += (int) shiftDist * pix.unitOrthogonal.getX();
			pix.y += (int) shiftDist * pix.unitOrthogonal.getY();
		}

		int oldBranchId = selectedBranch.id;

		ownerPlugin.imageProcessor.RemovePathFromDrawOverlay(selectedBranch);
		selectedBranch.roi = selectedBranch.getSimilarityVolume();

		ownerPlugin.imageProcessor.AddPathToDrawOverlay(selectedBranch);
		selectedBranch.id = oldBranchId;

		ownerPlugin.imageProcessor.SelectPath(selectedBranch, true);

		SearchPixel pix = selectedBranch.path.get(pathSegmentIndexSelected);
		ownerPlugin.imageProcessor.SetSelectedSegmentCursor(pix, ownerPlugin.featureSizePixels);
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
		boolean isCurrentToolPolyline = IJ.getToolName() == "polyline";
		btnActivatePolylineTool.setEnabled(!isCurrentToolPolyline);

		List<Point2D> pathPoints = ownerPlugin.imageProcessor.getCurrentImagePolylinePathPoints(null);
		boolean isThereACurrentPath = pathPoints != null;
		btnTraceCurrentPolyline.setEnabled(isThereACurrentPath);

		DendriteSegment selectedBranch = this.pathListBox.getSelectedValue();
		boolean isThereACurrentSelectedBranch = selectedBranch != null;
		this.btnDeleteBranch.setEnabled(isThereACurrentSelectedBranch);
		this.btnRenameBranch.setEnabled(isThereACurrentSelectedBranch);
		this.btnPrevSegment.setEnabled(isThereACurrentSelectedBranch && this.pathSegmentIndexSelected > 0);
		this.btnNextSegment.setEnabled(
				isThereACurrentSelectedBranch && this.pathSegmentIndexSelected < selectedBranch.path.size() - 1);
		this.btnMakeSegmentNarrower.setEnabled(isThereACurrentSelectedBranch);
		this.btnMakeSegmentWider.setEnabled(isThereACurrentSelectedBranch);
		this.btnShiftSegmentLeft.setEnabled(isThereACurrentSelectedBranch);
		this.btnShiftSegmentRight.setEnabled(isThereACurrentSelectedBranch);

		boolean isCurrentToolMultiPoint = IJ.getToolName() == "multipoint";
		this.btnActivateMultiPointTool.setEnabled(!isCurrentToolMultiPoint);

		boolean areThereAnyDendrites = !this.pathListModel.isEmpty();
		// btnCountMarkedSpines.setEnabled(areThereAnyDendrites);
		btnDetectSpines.setEnabled(areThereAnyDendrites);

		boolean areThereResults = this.resultsTableData.length > 0;
		btnCopyTableDataToClipboard.setEnabled(areThereResults);

		this.panelCalibration.update();
	}

	/**
	 * Update the states of the UI controls in the Feature Detection Size Input
	 * Panel based on the values of our underlying member variables.
	 */
	public void updateFeatureDetectionSizeInputPanel() {
		/*
		 * radioFeatureDetectionWindowInPixels
		 * .setSelected(enumFeatureDetectionWindowSizeUnits ==
		 * FeatureDetectionWindowSizeUnitsEnum.PIXELS);
		 * radioFeatureDetectionWindowInImageUnits
		 * .setSelected(enumFeatureDetectionWindowSizeUnits ==
		 * FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS);
		 * 
		 * Calibration cal = (ownerPlugin.imageProcessor != null) ?
		 * ownerPlugin.imageProcessor.getDimensions() : null; boolean isCalibrated = cal
		 * != null; if (!isCalibrated) {
		 * radioFeatureDetectionWindowInImageUnits.setText("(image units)");
		 * radioFeatureDetectionWindowInImageUnits.setEnabled(false);
		 * lblImageResolution.setText(String.format( "<html>" +
		 * "Feature window size set to %d pixels<br/>" + "(image scale not set)." +
		 * "</html>", this.getFeatureDetectionWindowSizeInPixels())); } else {
		 * radioFeatureDetectionWindowInImageUnits.setText(cal.getUnits());
		 * radioFeatureDetectionWindowInImageUnits.setEnabled(true);
		 * 
		 * lblImageResolution.setText(String.format( "<html>" +
		 * "Feature window size set to %d pixels<br/>" +
		 * "(image scale set to %.3f pixels per %s)." + "</html>",
		 * this.getFeatureDetectionWindowSizeInPixels(), cal.getRawX(1.0),
		 * cal.getUnit())); }
		 */
	}

	public int getFeatureDetectionWindowSizeInPixels() {
		/*
		 * int pixelWindowSize = 5; if (enumFeatureDetectionWindowSizeUnits ==
		 * FeatureDetectionWindowSizeUnitsEnum.PIXELS) { try { pixelWindowSize =
		 * Integer.valueOf(textfieldFeatureDetectionWindowSize.getText()); } catch
		 * (NumberFormatException ex) { } } else if (enumFeatureDetectionWindowSizeUnits
		 * == FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS) { try { double numUnits =
		 * Double.valueOf(textfieldFeatureDetectionWindowSize.getText()); Calibration
		 * cal = ownerPlugin.imageProcessor.getDimensions(); if (cal != null) {
		 * pixelWindowSize = (int) Math.floor(cal.getRawX(numUnits)); } } catch
		 * (NumberFormatException ex) { } }
		 * 
		 * if (pixelWindowSize < 3) { // If pixel window is too small, then the algo
		 * takes forever to run, // and produces noisy garbage. As such, we will impose
		 * an internal // minimum pixel window size. pixelWindowSize = 3; }
		 */

		return 0;
	}

	public void clearSpineAssociations() {
		Object[] segments = this.pathListModel.toArray();
		for (Object segment : segments) {
			DendriteSegment dendrite = (DendriteSegment) segment;
			dendrite.spines.clear();
		}
	}

	public void associateSpinesWithDendriteSegments(List<Point2D> spineMarks) {
		Object[] segments = this.pathListModel.toArray();
		this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

		for (Point2D spineMark : spineMarks) {
			DendriteSegment closestPath = null;
			double closestPathDist = Double.MAX_VALUE;

			for (Object segment : segments) {
				DendriteSegment dendrite = (DendriteSegment) segment;
				SearchPixel nearestPixelInDendrite = dendrite.findNearestPixel(spineMark.getX(), spineMark.getY());

				double thisDist = Math.hypot(nearestPixelInDendrite.x - spineMark.getX(),
						nearestPixelInDendrite.y - spineMark.getY());

				if (closestPath == null || thisDist < closestPathDist) {
					closestPathDist = thisDist;
					closestPath = dendrite;
				}
			}
			closestPath.spines.add(spineMark);
		}
		this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void populateResultsTable() {
		resultsTableColumns = new String[5];
		resultsTableColumns[0] = "Dendrite Segment";
		resultsTableColumns[3] = "Spine Count";

		Calibration cal = null; //ownerPlugin.imageProcessor.getDimensions();
		if (cal == null) {
			resultsTableColumns[1] = "Length (pixels)";
			resultsTableColumns[2] = "Avg. Width (pixels)";
			resultsTableColumns[4] = "Spine Density (count/pixel)";
		} else {
			resultsTableColumns[1] = "Length (" + cal.getUnits() + ")";
			resultsTableColumns[2] = "Avg. Width (" + cal.getUnits() + ")";
			resultsTableColumns[4] = "Spine Density (count/" + cal.getUnit() + ")";
		}

		Object[] segments = this.pathListModel.toArray();
		this.resultsTableData = new Object[segments.length][resultsTableColumns.length];
		for (int iSegment = 0; iSegment < segments.length; iSegment++) {
			DendriteSegment dendrite = (DendriteSegment) (segments[iSegment]);

			Object[] resultRow = resultsTableData[iSegment];
			resultRow[0] = dendrite.getName();

			double dendriteLength = dendrite.minimumSeparation * dendrite.path.size();
			if (cal != null) {
				dendriteLength = cal.getX(dendriteLength);
			}

			double dendriteWidth = dendrite.MeanPixelWidth();
			if (cal != null) {
				dendriteWidth = cal.getX(dendriteWidth);
			}

			resultRow[1] = String.format("%.2f", dendriteLength);
			resultRow[2] = String.format("%.2f", dendriteWidth);

			resultRow[3] = String.format("%d", dendrite.spines.size());

			double spinesPerUnit = ((double) dendrite.spines.size()) / dendriteLength;
			resultRow[4] = String.format("%.5f", spinesPerUnit);
		}

		this.resultsTable = new JTable(resultsTableData, resultsTableColumns);
		this.resultsTableHolder.setViewportView(this.resultsTable);
		this.resultsTable.setFillsViewportHeight(true);
	}

	public void copyResultsTableToClipboard() {
		String s = "";

		String strImageDes = this.textfieldResultTableImageDesignation.getText().trim();
		String strResearcher = this.textfieldResultTableResearcher.getText().trim();
		String strCustomLbl = this.textfieldResultTableImageCustomLabel.getText().trim();

		if (this.chkIncludeHeadersInCopyPaste.isSelected()) {
			for (String columnName : this.resultsTableColumns) {
				s += columnName + "\t";
			}

			if (!strImageDes.isEmpty()) {
				s += "Image Designator\t";
			}
			if (!strResearcher.isEmpty()) {
				s += "Researcher\t";
			}
			if (!strCustomLbl.isEmpty()) {
				s += "Custom Label\t";
			}

			// Trim the last tab.
			s = s.substring(0, s.length() - 1);
			s += "\n";
		}

		for (Object[] row : this.resultsTableData) {
			for (Object rowItem : row) {
				s += rowItem.toString() + "\t";
			}

			if (!strImageDes.isEmpty()) {
				s += strImageDes + "\t";
			}
			if (!strResearcher.isEmpty()) {
				s += strResearcher + "\t";
			}
			if (!strCustomLbl.isEmpty()) {
				s += strCustomLbl + "\t";
			}

			s = s.substring(0, s.length() - 1);
			s += "\n";
		}

		// https://stackoverflow.com/questions/6710350/copying-text-to-the-clipboard-using-java
		StringSelection stringSelection = new StringSelection(s);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
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

		v = jsonObj.get("researcher");
		this.textfieldResultTableResearcher.setText(String.format(v.toString()));

		v = jsonObj.get("imagedesignation");
		this.textfieldResultTableImageDesignation.setText(String.format(v.toString()));

		v = jsonObj.get("customlabel");
		this.textfieldResultTableImageCustomLabel.setText(String.format(v.toString()));

		JSONArray jsonDends = (JSONArray) jsonObj.get("dendrites");
		List<Point2D> allspines = new ArrayList<Point2D>();
		for (int iDend = 0; iDend < jsonDends.size(); iDend++) {
			JSONObject jsonDend = (JSONObject) jsonDends.get(iDend);
			DendriteSegment dendrite = new DendriteSegment();
			dendrite.fromJSON(jsonDend, ownerPlugin.imageProcessor.workingImg);

			this.pathListModel.addElement(dendrite);
			ownerPlugin.imageProcessor.AddPathToDrawOverlay(dendrite);

			allspines.addAll(dendrite.spines);
		}

		// Add all the spines as visible ROI points in one big blast.
		ownerPlugin.imageProcessor.AddPointRoisAsSpineMarkers(allspines);

		populateResultsTable();

		update();
		ownerPlugin.imageProcessor.update();
	}
}
