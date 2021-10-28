package com.MightyDataInc.DendriticSpineCounter;

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
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import com.MightyDataInc.DendriticSpineCounter.SearchPixel.PathSide;

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

	// --------------------------------------
	// Data-bound UI components

	private JTabbedPane tabbedPane;

	private JButton btnActivatePolylineTool;
	private JButton btnTraceCurrentPolyline;
	private JButton btnActivateMultiPointTool;
	private JButton btnCountMarkedSpines;
	private JButton btnDetectSpines;
	
	private JSlider sliderDetectionSensitivity;

	private JButton btnDeleteBranch;
	private JButton btnNextSegment;
	private JButton btnPrevSegment;
	private JButton btnMakeSegmentWider;
	private JButton btnMakeSegmentNarrower;
	private JButton btnShiftSegmentLeft;
	private JButton btnShiftSegmentRight;
	private JButton btnCopyTableDataToClipboard;

	private JButton btnSaveDataToFile;
	private JButton btnLoadDataFromFile;

	private JRadioButton radioFeatureDetectionWindowInPixels;
	private JRadioButton radioFeatureDetectionWindowInImageUnits;
	private JCheckBox chkIncludeHeadersInCopyPaste;

	public JTextField textfieldFeatureDetectionWindowSize;

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

	// ----------------------------------------
	// Data-bound UI-inputted properties.

	public enum FeatureDetectionWindowSizeUnitsEnum {
		PIXELS, IMAGE_UNITS
	};

	public FeatureDetectionWindowSizeUnitsEnum enumFeatureDetectionWindowSizeUnits = 
			FeatureDetectionWindowSizeUnitsEnum.PIXELS;

	public DscControlPanelDialog(Dendritic_Spine_Counter plugin) {
		super((Frame) null, "Dendritic Spine Counter", false);
		ownerPlugin = plugin;

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

			JPanel panel1 = createFeatureDetectionSizeInputPanel();
			tabbedPane.addTab("Set feature size", panel1);

			JPanel panel2 = createPathInputSpecificationPanel();
			tabbedPane.addTab("Trace dendrites", panel2);

			JPanel panel3 = createSpineSelectionPanel();
			tabbedPane.addTab("Mark spines", panel3);

			JPanel panel4 = createReportPanel();
			tabbedPane.addTab("Report results", panel4);
		    tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					// TODO Auto-generated method stub
					if (tabbedPane.getSelectedIndex() != 3) {
						return;
					}
					countSpinesAndBuildTable();
				}
		    });
			JPanel panel5 = createFileLoadSavePanel();
			tabbedPane.addTab("Save/Load", panel5);
		}
		/*
		 * 
		 * controlPanel.add(this.createSpineFinderPanel(), gridbagConstraints);
		 */

		// Make the dialog pretty by setting its icon.
		{
			String pathToImage = "images/icons/dsc--find-spines-32.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
			Image img = myIcon.getImage();
			this.setIconImage(img);
		}

		// Clean up dialog's organization/layout and set its size.
		pack();
		// setPreferredSize(new Dimension(600, getHeight()));
		// setMinimumSize(new Dimension(600, 800));
		setPreferredSize(new Dimension(600, 768));
		pack();
		setVisible(true);
		// setResizable(false);
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
			JLabel label = new JLabel("<html>" 
					+ "<p>This plug-in automatically"
					+ "goes through all of the spines you've currently selected with "
					+ "the Multi-point Tool. It will associate each spine with its "
					+ "nearest dendrite segment, and tabulate statistics about "
					+ "the spine counts and densities for each dendrite segment.</p>" 
					+ "</html>");
			gridbagConstraints.insets.bottom = 8;
			panel.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.bottom = 4;
		}
		
		/*
		{
			String pathToImage = "images/icons/dsc--find-spines-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnCountMarkedSpines = new JButton("Count spines near dendrite segments", myIcon);

			btnCountMarkedSpines.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					countSpinesAndBuildTable();
				}
			});
			panel.add(btnCountMarkedSpines, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}
		*/

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

			DscControlPanelDialog self = this;

			btnLoadDataFromFile.addActionListener(new ActionListener() {			
				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setFileFilter(jsonFileFilter);
					int result = fileChooser.showOpenDialog(null);
					if (result != JFileChooser.APPROVE_OPTION) {
						return;
					}

					String filename = fileChooser.getSelectedFile().getAbsolutePath();
					String filecontents = "";
					try {
						filecontents = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					JSONParser parser = new JSONParser();
					JSONObject jsonObj = null;
					try {
						jsonObj = (JSONObject) parser.parse(filecontents);
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(null, e1.toString());
						return;
					}

					if (jsonObj == null) {
						return;
					}

					Object v = jsonObj.get("featuresizepixels");

					self.enumFeatureDetectionWindowSizeUnits = FeatureDetectionWindowSizeUnitsEnum.PIXELS;
					self.textfieldFeatureDetectionWindowSize.setText(String.format(v.toString()));

					v = jsonObj.get("researcher");
					self.textfieldResultTableResearcher.setText(String.format(v.toString()));

					v = jsonObj.get("imagedesignation");
					self.textfieldResultTableImageDesignation.setText(String.format(v.toString()));

					v = jsonObj.get("customlabel");
					self.textfieldResultTableImageCustomLabel.setText(String.format(v.toString()));

					JSONArray jsonDends = (JSONArray) jsonObj.get("dendrites");
					for (int iDend = 0; iDend < jsonDends.size(); iDend++) {
						JSONObject jsonDend = (JSONObject) jsonDends.get(iDend);
						DendriteSegment dendrite = new DendriteSegment();
						dendrite.fromJSON(jsonDend, ownerPlugin.workingImg);

						self.pathListModel.addElement(dendrite);
						ownerPlugin.AddPathToDrawOverlay(dendrite);
					}

					populateResultsTable();

					updateInputSpecificationButtonEnablements();
					((ImagePlus)(ownerPlugin.getWorkingImagePlus())).updateAndRepaintWindow();
				}
			});

			gridbagConstraints.gridy++;
		}

		addEmptySpaceFillerLabel(panel, gridbagConstraints);		
		
		return panel;
	}

	/**
	 * This last panel lets users place points to mark spines, and generates a
	 * report table.
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

			JLabel label = new JLabel("<html>" + "Use the Multi-point Tool to mark spines. "
					+ "Dendritic Spine Counter "
					+ "will automatically associate each spine with whichever dendrite segment "  
					+ "is closest to it."
					+ "<ul>"
					+ "<li>Click within the image to mark a spine.</li>"
					+ "<li>Click and hold a marked spine (drag it) to relocate it.</li>"
					+ "<li>Alt-Click a marked spine to remove it.</li>"
					+ "</ul>"
					+ "<p>Spines marked with this tool are \"tentative\", i.e. they are "
					+ "considered a Multi-point selection, and can be added, moved, or deleted "
					+ "as you see fit. You will have the chance to tabulate them in the "
					+ "\"Report results\" tab."
					+ "</html>");
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
					updateInputSpecificationButtonEnablements();
					ownerPlugin.setWorkingImageWindowToForeground();
				}
			});
			panel.add(btnActivateMultiPointTool, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			JLabel label = new JLabel("<html>" 
					+ "This plug-in can auto-detect spines that project outward from the edges of " 
					+ "your dendrite segments. You can adjust the contrast sensitivity that this plug-in "
					+ "uses for this task. Low contrast sensitivity means that a spine needs to be "
					+ "significantly darker than its background in order to be recognized, " 
					+ "while high contrast sensitivity may try to mark a feature as a spine even if "
					+ "it is only slightly darker than its background.<br/<br/>"
					+ "After spines are automatically detected, you will then have the ability to move, "
					+ "add, or delete them as you see fit.<br/<br/>"
					+ "NOTE: Using this function will clear your current selection and replace it with "
					+ "the auto-detected features. If you're going to use this feature, you should "
					+ "use it <i>first</i>, and <i>then</i> add more spines manually if needed."
					+ "</html>");
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
					double sensitivity = (100.0 - (double)sensitivitySliderVal) / 50.0;
					sensitivity *= sensitivity;
					sensitivity *= sensitivity;
					sensitivity *= 0.25;
					// The "sensitivity" is actually kinda backwards.
					// It needs an easing function to mean what the labeling says it means.
					
					List<Point2D> spines = new ArrayList<Point2D>();
					
					Object[] paths = pathListModel.toArray();
					for (Object path : paths) {
						DendriteSegment dendriteSegment  = (DendriteSegment)path;
						
						for (PathSide side : PathSide.values()) {
							DendriteSegment sidepath = dendriteSegment.createSidePath(
									side, pixelWindowSize, pixelWindowSize / 2);
							
							List<Point2D> spinesHere =
									sidepath.findSpinesAlongSidepath(pixelWindowSize, SCANSPAN, sensitivity);
							
							spines.addAll(spinesHere);
						}
					}
					ownerPlugin.AddPointRoisAsSpineMarkers(spines);
				}
			});
			panel.add(btnDetectSpines, gridbagConstraints);
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}
		
		{
			JLabel label = new JLabel("<html>"
					+ "Contrast sensitivity (% brightness difference)"
					+ "</html>");
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
			String pathToImage = "images/icons/data-table-results-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnNext = new JButton("Next: Report results", myIcon);

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
			JLabel label = new JLabel(
					"<html>" + "Use the Polyline tool to trace a dendrite segment. "
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
					((ImagePlus)(ownerPlugin.getWorkingImagePlus())).setRoi((Roi)null);
					((ImagePlus)(ownerPlugin.getWorkingImagePlus())).updateAndRepaintWindow();
					
					IJ.setTool("polyline");
					updateInputSpecificationButtonEnablements();
					ownerPlugin.setWorkingImageWindowToForeground();
					((ImagePlus)(ownerPlugin.getWorkingImagePlus())).updateAndRepaintWindow();
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
					DendriteSegment dendritePath = ownerPlugin.traceDendriteWithThicknessEstimation(.8);
					pathListModel.addElement(dendritePath);
					ownerPlugin.AddPathToDrawOverlay(dendritePath);
					((ImagePlus)(ownerPlugin.getWorkingImagePlus())).updateAndRepaintWindow();
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
			listScroller.setPreferredSize(new Dimension(250, 160));
			listScroller.setMinimumSize(new Dimension(250, 160));

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
						ownerPlugin.SelectPath((DendriteSegment) path, false);
					}

					// Now select the one path that's actually selected.
					DendriteSegment path = pathListBox.getSelectedValue();
					ownerPlugin.SelectPath(path, true);
					pathSegmentIndexSelected = 0;
					if (path != null && path.path != null && path.path.size() > 0) {
						ownerPlugin.SetSelectedSegmentCursor(path.path.get(0));
					} else {
						ownerPlugin.SetSelectedSegmentCursor(null);
					}

					updateInputSpecificationButtonEnablements();
					((ImagePlus)(ownerPlugin.getWorkingImagePlus())).updateAndRepaintWindow();					
				}
			});

			panel.add(listScroller, gridbagConstraints);
			gridbagConstraints.gridy++;
		}
		{
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
						ownerPlugin.RemovePathFromDrawOverlay(selectedPath);
						pathSegmentIndexSelected = 0;
						updateSelectedSegment();
						updateInputSpecificationButtonEnablements();
					}
				});

				panel.add(btnDeleteBranch, gridbagConstraints);
				gridbagConstraints.gridy++;
			}

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

			JButton btnNext = new JButton("Next: Mark spines", myIcon);

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

	/**
	 * The user needs to tell us how big a visual feature is. Ultimately we need
	 * this info in pixels, but the user needs to be able to specify it in microns
	 * as well.
	 * 
	 * @return The panel that it created. Add this to whatever master outer panel
	 *         you're building.
	 */
	private JPanel createFeatureDetectionSizeInputPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		// panel.setBorder(BorderFactory.createLineBorder(getForeground()));

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
				+ "<p>Setting this value too high will cause the Finder to fail to find smaller "
				+ "or blurrier spines (Type II errors). Setting it too low will cause the "
				+ "Finder to incorrectly identify spines where none exist (Type I errors). "
				+ "(A low setting may also increase the running time of the feature identification process.)</p>"
				+ "</div></html>";

		{
			// Icon icon = UIManager.getIcon("OptionPane.informationIcon");
			JLabel label = new JLabel("<html>" + infoMsg + "</html>", SwingConstants.RIGHT);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridwidth = 3;
			panel.add(label, gridbagConstraints);

			// Next row!
			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			// First (real) row: Ask them to give us a number.
			// If this number is in pixels, then that's all we need from them at this point.
			JLabel label = new JLabel("<html>" + "Feature detection window size: " + "</html>", SwingConstants.RIGHT);
			panel.add(label, gridbagConstraints);

			{
				textfieldFeatureDetectionWindowSize = new JTextField(5);
	
				textfieldFeatureDetectionWindowSize.setText(String.format("%d", this.ownerPlugin.featureSizePixels));
				textfieldFeatureDetectionWindowSize.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						ownerPlugin.featureSizePixels = getFeatureDetectionWindowSizeInPixels();
					}
				});
				
				JButton btnApply = new JButton("Apply");
				btnApply.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						updateFeatureDetectionSizeInputPanel();						
					}
				});
				
				Box box = Box.createVerticalBox();

				box.add(textfieldFeatureDetectionWindowSize);
				box.add(btnApply);

				gridbagConstraints.gridx++;
				panel.add(box, gridbagConstraints);
			}

			{
				radioFeatureDetectionWindowInPixels = new JRadioButton("pixels");
				radioFeatureDetectionWindowInPixels.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						enumFeatureDetectionWindowSizeUnits = FeatureDetectionWindowSizeUnitsEnum.PIXELS;
						updateFeatureDetectionSizeInputPanel();
					}
				});

				radioFeatureDetectionWindowInImageUnits = new JRadioButton("(image units)");
				radioFeatureDetectionWindowInImageUnits.setEnabled(false);
				radioFeatureDetectionWindowInImageUnits.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						enumFeatureDetectionWindowSizeUnits = FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS;
						updateFeatureDetectionSizeInputPanel();
					}
				});

				ButtonGroup pixelOrMicron = new ButtonGroup();
				pixelOrMicron.add(radioFeatureDetectionWindowInPixels);
				pixelOrMicron.add(radioFeatureDetectionWindowInImageUnits);

				Box box = Box.createVerticalBox();

				box.add(radioFeatureDetectionWindowInPixels);
				box.add(radioFeatureDetectionWindowInImageUnits);

				gridbagConstraints.gridx++;
				panel.add(box, gridbagConstraints);
			}

			// Next row!
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;
		}

		{
			gridbagConstraints.gridwidth = 2;
			this.lblImageResolution = new JLabel();
			panel.add(lblImageResolution, gridbagConstraints);

			gridbagConstraints.gridwidth = 1;
			gridbagConstraints.gridx = 2;
			JButton btnOpenSetScale = new JButton("Set Scale...");
			btnOpenSetScale.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Executer("Set Scale...", 
							((ImagePlus)(ownerPlugin.getWorkingImagePlus())));
					
					// NOTE: The executor spins off on a separate thread.
					// So this update occurs right away, while the user is
					// still entering values.
					enumFeatureDetectionWindowSizeUnits = FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS;
					updateFeatureDetectionSizeInputPanel();					
				}
			});
			panel.add(btnOpenSetScale, gridbagConstraints);

			// Next row!
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;
		}
		
		gridbagConstraints.gridwidth = 3;
		{
			JLabel label = new JLabel("<html>" 
					+ "<hr/><br/>"
					+ "<p>This plug-in assumes that the images being examined were "
					+ "captured with bright-field (BF) microscopy, with stacking "
					+ "presumably performed using minimum intensity projection (MinIP). "
					+ "I.e. it assumes that this image's noteworthy "
					+ "features appear dark upon a light background. If your "
					+ "image was taken with another technique or processed such "
					+ "that the opposite is true, then the working image "
					+ "will need to be inverted."
					+ "</html>");
			gridbagConstraints.insets.top = 20;
			gridbagConstraints.insets.bottom = 8;
			panel.add(label, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}		
		
		{
			gridbagConstraints.insets.top = 4;
			JButton btnInvertImage = new JButton("Invert image brightness levels");
			btnInvertImage.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ownerPlugin.invertImage();
				}
			});
			panel.add(btnInvertImage, gridbagConstraints);

			// Next row!
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 0;
		}

		{
			String pathToImage = "images/icons/dsc--trace-dendrite-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnNext = new JButton("Next: Trace dendrites", myIcon);

			btnNext.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(1);
				}
			});
			gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
			gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
			gridbagConstraints.weighty = 1.0;
			panel.add(btnNext, gridbagConstraints);
		}

		updateFeatureDetectionSizeInputPanel();
		return panel;
	}
	
	public void countSpinesAndBuildTable() {
		List<Point2D> points = ownerPlugin.getPointsFromCurrentPolylineRoiSelection();
		clearSpineAssociations();
		associateSpinesWithDendriteSegments(points);
		populateResultsTable();
		updateInputSpecificationButtonEnablements();
	}

	public void updateSelectedSegment() {
		DendriteSegment selectedBranch = this.pathListBox.getSelectedValue();
		if (selectedBranch == null) {
			this.pathSegmentIndexSelected = 0;
			ownerPlugin.SetSelectedSegmentCursor(null);
			return;
		}

		if (this.pathSegmentIndexSelected < 0) {
			this.pathSegmentIndexSelected = 0;
		}
		if (this.pathSegmentIndexSelected > selectedBranch.path.size() - 1) {
			this.pathSegmentIndexSelected = selectedBranch.path.size() - 1;
		}

		SearchPixel pix = selectedBranch.path.get(pathSegmentIndexSelected);
		ownerPlugin.SetSelectedSegmentCursor(pix);
		this.updateInputSpecificationButtonEnablements();
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

		ownerPlugin.RemovePathFromDrawOverlay(selectedBranch);
		selectedBranch.roi = selectedBranch.getSimilarityVolume();

		ownerPlugin.AddPathToDrawOverlay(selectedBranch);
		selectedBranch.id = oldBranchId;

		ownerPlugin.SelectPath(selectedBranch, true);
		ownerPlugin.SetSelectedSegmentCursor(pix);
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

		ownerPlugin.RemovePathFromDrawOverlay(selectedBranch);
		selectedBranch.roi = selectedBranch.getSimilarityVolume();

		ownerPlugin.AddPathToDrawOverlay(selectedBranch);
		selectedBranch.id = oldBranchId;

		ownerPlugin.SelectPath(selectedBranch, true);

		SearchPixel pix = selectedBranch.path.get(pathSegmentIndexSelected);
		ownerPlugin.SetSelectedSegmentCursor(pix);
	}

	private void setupWindowEventHandlers() {
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				updateInputSpecificationButtonEnablements();
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
				updateInputSpecificationButtonEnablements();
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

	public void updateInputSpecificationButtonEnablements() {
		boolean isCurrentToolPolyline = IJ.getToolName() == "polyline";
		btnActivatePolylineTool.setEnabled(!isCurrentToolPolyline);

		List<Point2D> pathPoints = ownerPlugin.getCurrentImagePolylinePathPoints();
		boolean isThereACurrentPath = pathPoints != null;
		btnTraceCurrentPolyline.setEnabled(isThereACurrentPath);

		DendriteSegment selectedBranch = this.pathListBox.getSelectedValue();
		boolean isThereACurrentSelectedBranch = selectedBranch != null;
		this.btnDeleteBranch.setEnabled(isThereACurrentSelectedBranch);
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
		//btnCountMarkedSpines.setEnabled(areThereAnyDendrites);
		btnDetectSpines.setEnabled(areThereAnyDendrites);

		boolean areThereResults = this.resultsTableData.length > 0;
		btnCopyTableDataToClipboard.setEnabled(areThereResults);

		updateFeatureDetectionSizeInputPanel();
	}

	/**
	 * Update the states of the UI controls in the Feature Detection Size Input
	 * Panel based on the values of our underlying member variables.
	 */
	public void updateFeatureDetectionSizeInputPanel() {
		radioFeatureDetectionWindowInPixels
				.setSelected(enumFeatureDetectionWindowSizeUnits == FeatureDetectionWindowSizeUnitsEnum.PIXELS);
		radioFeatureDetectionWindowInImageUnits
				.setSelected(enumFeatureDetectionWindowSizeUnits == FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS);

		Calibration cal = ownerPlugin.getWorkingImageDimensions();
		boolean isCalibrated = cal != null;
		if (!isCalibrated) {
			radioFeatureDetectionWindowInImageUnits.setText("(image units)");
			radioFeatureDetectionWindowInImageUnits.setEnabled(false);
			lblImageResolution.setText(String.format(
					"<html>" + "Feature window size set to %d pixels<br/>" + "(image scale not set)." + "</html>",
					this.getFeatureDetectionWindowSizeInPixels()));
		} else {
			radioFeatureDetectionWindowInImageUnits.setText(cal.getUnits());
			radioFeatureDetectionWindowInImageUnits.setEnabled(true);

			lblImageResolution.setText(String.format(
					"<html>" + "Feature window size set to %d pixels<br/>" + "(image scale set to %.3f pixels per %s)."
							+ "</html>",
					this.getFeatureDetectionWindowSizeInPixels(), cal.getRawX(1.0), cal.getUnit()));
		}
	}

	public int getFeatureDetectionWindowSizeInPixels() {
		int pixelWindowSize = 5;
		if (enumFeatureDetectionWindowSizeUnits == FeatureDetectionWindowSizeUnitsEnum.PIXELS) {
			try {
				pixelWindowSize = Integer.valueOf(textfieldFeatureDetectionWindowSize.getText());
			} catch (NumberFormatException ex) {
			}
		} else if (enumFeatureDetectionWindowSizeUnits == FeatureDetectionWindowSizeUnitsEnum.IMAGE_UNITS) {
			try {
				double numUnits = Double.valueOf(textfieldFeatureDetectionWindowSize.getText());
				Calibration cal = ownerPlugin.getWorkingImageDimensions();
				if (cal != null) {
					pixelWindowSize = (int) Math.floor(cal.getRawX(numUnits));
				}
			} catch (NumberFormatException ex) {
			}
		}

		if (pixelWindowSize < 3) {
			// If pixel window is too small, then the algo takes forever to run,
			// and produces noisy garbage. As such, we will impose an internal
			// minimum pixel window size.
			pixelWindowSize = 3;
		}

		return pixelWindowSize;
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
	}

	public void populateResultsTable() {
		resultsTableColumns = new String[5];
		resultsTableColumns[0] = "Dendrite Segment";
		resultsTableColumns[3] = "Spine Count";

		Calibration cal = ownerPlugin.getWorkingImageDimensions();
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
			resultRow[0] = dendrite.toString();

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
			double unitsPerSpine = 1.0 / spinesPerUnit;
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
}
