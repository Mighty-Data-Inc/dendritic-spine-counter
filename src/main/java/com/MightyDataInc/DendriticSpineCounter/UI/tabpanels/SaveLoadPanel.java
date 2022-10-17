package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteBranch;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

/**
 * This last panel lets users place points to mark spines, and generates a
 * report table.
 */
public class SaveLoadPanel extends DscBasePanel {
	private static final long serialVersionUID = -8949215323124689073L;

	private JButton btnSaveDataToFile;
	private JButton btnLoadDataFromFile;

	public SaveLoadPanel(DscControlPanelDialog controlPanel) {
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

		this.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.bottom = 4;
		gridbagConstraints.insets.left = 16;
		gridbagConstraints.insets.right = 16;

		{
			String pathToImage = "images/icons/file-save-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnSaveDataToFile = new JButton("Save data to file", myIcon);
			this.add(btnSaveDataToFile, gridbagConstraints);

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
					json.put("version", myPlugin().getApplicationVersion());

					try {
						json.put("originalimagefile", myPlugin().getOriginalImage().getImgPlus().getSource());
					} catch (Exception e1) {
					}

					JSONObject jsonModel = myModel().saveToJsonObject();
					json.put("model", jsonModel);

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

		}

		{
			String pathToImage = "images/icons/file-load-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			btnLoadDataFromFile = new JButton("Load data from file", myIcon);
			this.add(btnLoadDataFromFile, gridbagConstraints);

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
					JSONObject jsonObj = DscModel.getJsonObjectFromFile(filename);
					if (jsonObj == null) {
						JOptionPane.showMessageDialog(null,
								"<html>No JSON data could be parsed from this file.</html>");
						return;
					}

					String versionNumber = (String) jsonObj.get("version");
					if (versionNumber == null) {
						JOptionPane.showMessageDialog(null,
								"<html>No version information was found <br/>"
										+ "in this file. Are you sure that this file <br/>"
										+ "was saved by this app in the first place?</html>");
						return;
					}

					if (versionNumber != myPlugin().getApplicationVersion()) {
						JOptionPane.showMessageDialog(null, "<html>This JSON file appears to be from a different "
								+ "version of this application. <br/>We're very sorry, but older save files are no longer "
								+ "compatible with new updates. <br/>You'll have to re-tag your images. We normally try "
								+ "to preserve backward compatibility, <br/>but it isn't always possible. Please accept "
								+ "our apologies for the inconvenience.</html>");
						return;
					}

					DscModel newModel = DscModel.loadFromJsonObject(jsonObj);
					if (newModel != null) {
						myPlugin().setModel(newModel);
					}
				}
			});

			gridbagConstraints.gridy++;
		}

		update();
		return this;
	}

	@Override

	public void update() {
	}
}
