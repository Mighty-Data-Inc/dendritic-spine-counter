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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteBranch;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;

public class ReportPanel extends DscBasePanel {
	private static final long serialVersionUID = -1468233731883457835L;

	private JTable summaryTable;
	private JScrollPane summaryTableHolder;
	private Object[][] summaryTableData = new Object[0][];
	private String[] summaryTableColumns = {};

	private JTable detailsTable;
	private JScrollPane detailsTableHolder;
	private Object[][] detailsTableData = new Object[0][];
	private String[] detailsTableColumns;

	private JCheckBox chkIncludeSummaryHeadersInCopyPaste;
	private JCheckBox chkIncludeDetailsHeadersInCopyPaste;

	public ReportPanel(DscControlPanelDialog controlPanel) {
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
			gridbagConstraints.gridwidth = 2;
			this.add(new JLabel(
					"<html>Dendrite branch summary: Spine counts and densities on a branch-by-branch basis.<br/>"
							+ "Use the \"Copy to clipboard\" button to copy this data into your computer's clipboard buffer. You "
							+ "can paste it directly into Excel or any other spreadsheet program of your choosing."
							+ "</html>"),
					gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 1;
		}

		{
			this.summaryTableHolder = new JScrollPane();

			summaryTableHolder.setPreferredSize(new Dimension(250, 120));
			summaryTableHolder.setMinimumSize(new Dimension(250, 120));

			gridbagConstraints.gridwidth = 2;
			this.add(summaryTableHolder, gridbagConstraints);

			gridbagConstraints.insets.top = 2;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 1;
		}

		{
			chkIncludeSummaryHeadersInCopyPaste = new JCheckBox("Include headers when copying");
			this.add(chkIncludeSummaryHeadersInCopyPaste, gridbagConstraints);
			chkIncludeSummaryHeadersInCopyPaste.setSelected(true);

			gridbagConstraints.gridx++;

			String pathToImage = "images/icons/copy.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnSummary = new JButton("Copy summary table data to clipboard", myIcon);
			gridbagConstraints.insets.bottom = 2;
			this.add(btnSummary, gridbagConstraints);

			btnSummary.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String[] columns = null;
					if (chkIncludeSummaryHeadersInCopyPaste.isSelected()) {
						columns = summaryTableColumns;
					}
					String s = getTableExport(summaryTableData, columns, "\t");
					copyToClipboard(s);
				}
			});
		}

		{
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 2;

			String pathToImage = "images/icons/file-save-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnExportSummary = new JButton("Export summary table to .csv file", myIcon);
			this.add(btnExportSummary, gridbagConstraints);

			btnExportSummary.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String s = getTableExport(summaryTableData, summaryTableColumns, ",");
					exportFileData("Save summary table to file", s);
				}
			});

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		{
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.insets.top = 16;
			this.add(new JLabel("<html>Dendrite spine details</html>"), gridbagConstraints);

			gridbagConstraints.insets.top = 2;
			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 1;
		}

		{
			this.detailsTableHolder = new JScrollPane();

			detailsTableHolder.setPreferredSize(new Dimension(250, 120));
			detailsTableHolder.setMinimumSize(new Dimension(250, 120));

			gridbagConstraints.gridwidth = 2;
			this.add(detailsTableHolder, gridbagConstraints);

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 1;
		}

		{
			chkIncludeDetailsHeadersInCopyPaste = new JCheckBox("Include headers when copying");
			this.add(chkIncludeDetailsHeadersInCopyPaste, gridbagConstraints);
			chkIncludeDetailsHeadersInCopyPaste.setSelected(true);
			gridbagConstraints.gridx++;

			String pathToImage = "images/icons/copy.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btn = new JButton("Copy details table data to clipboard", myIcon);
			gridbagConstraints.insets.bottom = 2;
			this.add(btn, gridbagConstraints);

			btn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String[] columns = null;
					if (chkIncludeDetailsHeadersInCopyPaste.isSelected()) {
						columns = detailsTableColumns;
					}
					String s = getTableExport(detailsTableData, columns, "\t");
					copyToClipboard(s);
				}
			});
		}

		{
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 2;

			String pathToImage = "images/icons/file-save-24.png";
			ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));

			JButton btnExportSummary = new JButton("Export details table to .csv file", myIcon);
			this.add(btnExportSummary, gridbagConstraints);

			btnExportSummary.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String s = getTableExport(detailsTableData, detailsTableColumns, ",");
					exportFileData("Save details table to file", s);
				}
			});

			gridbagConstraints.gridx = 0;
			gridbagConstraints.gridy++;
		}

		addNextButton("Next: Save/Load", "images/icons/file-save-24.png");

		update();
		return this;
	}

	@Override
	public void onTimer() {
	}

	@Override
	public void update() {
		populateSummaryTable();
		populateDetailsTable();
	}

	@Override
	protected void onPanelEntered() {
		update();
	}

	private String[] generateSummaryTableColumns() {
		List<String> headers = new ArrayList<String>();
		headers.add("Dendrite Branch Label");

		String unitname = "px";
		if (myModel().imageHasValidPhysicalUnitScale()) {
			unitname = myModel().getImageScalePhysicalUnitName();
		}

		headers.add(String.format("Length (%s)", unitname));
		headers.add(String.format("Avg Width (%s)", unitname));

		headers.add(String.format("Total Spine Count (n)"));
		headers.add(String.format("Total Spine Density (n/%s)", unitname));

		headers.add(String.format("Total Spine Count No Filopodia (n)"));
		headers.add(String.format("Total Spine Density No Filopodia (n/%s)", unitname));

		for (String spineClass : myModel().spineClasses) {
			headers.add(String.format("%s Count (n)", spineClass));
			headers.add(String.format("%s Density (n/%s)", spineClass, unitname));
		}

		// https://www.geeksforgeeks.org/arraylist-toarray-method-in-java-with-examples/
		String arr[] = new String[headers.size()];
		arr = headers.toArray(arr);

		return arr;
	}

	private void populateSummaryTable() {
		summaryTableColumns = generateSummaryTableColumns();

		List<DendriteBranch> dendrites = myModel().getDendrites();
		summaryTableData = new Object[dendrites.size()][summaryTableColumns.length];

		for (int iDendrite = 0; iDendrite < dendrites.size(); iDendrite++) {
			DendriteBranch dendrite = dendrites.get(iDendrite);

			String dendriteName = dendrite.name;
			if (dendriteName == null || dendriteName.length() == 0) {
				dendriteName = dendrite.getName();
			}

			Object[] row = summaryTableData[iDendrite];

			int iCol = 0;
			row[iCol++] = dendriteName;

			double dendriteLength = myModel().convertImageScaleFromPixelsToPhysicalUnits(dendrite.getLengthInPixels());

			row[iCol++] = String.format("%.3f", dendriteLength);
			row[iCol++] = String.format("%.3f",
					myModel().convertImageScaleFromPixelsToPhysicalUnits(dendrite.getAverageWidthInPixels()));

			List<DendriteSpine> spinesOfThisDendrite = myModel().getSpinesOfDendrite(dendrite);

			row[iCol++] = spinesOfThisDendrite.size();
			row[iCol++] = String.format("%.8f", spinesOfThisDendrite.size() / dendriteLength);

			List<DendriteSpine> spinesNoFilo = new ArrayList<DendriteSpine>(spinesOfThisDendrite);
			spinesNoFilo.removeIf(new Predicate<DendriteSpine>() {
				@Override
				public boolean test(DendriteSpine arg0) {
					return arg0.getClassification().equals("filopodia");
				}
			});

			row[iCol++] = spinesNoFilo.size();
			row[iCol++] = String.format("%.8f", spinesNoFilo.size() / dendriteLength);

			for (String spineClass : myModel().spineClasses) {
				List<DendriteSpine> spinesOfThisClass = new ArrayList<DendriteSpine>(spinesOfThisDendrite);
				spinesOfThisClass.removeIf(new Predicate<DendriteSpine>() {
					@Override
					public boolean test(DendriteSpine arg0) {
						return arg0.getClassification() != spineClass;
					}
				});
				row[iCol++] = spinesOfThisClass.size();
				row[iCol++] = String.format("%.8f", spinesOfThisClass.size() / dendriteLength);
			}
		}

		this.summaryTable = new JTable(summaryTableData, summaryTableColumns);
		this.summaryTableHolder.setViewportView(summaryTable);
		this.summaryTable.setFillsViewportHeight(true);
	}

	private String[] generateDetailsTableColumns() {
		List<String> headers = new ArrayList<String>();

		headers.add("Spine ID");
		headers.add("Dendrite Branch");

		headers.add("Classification");

		String unitname = "px";
		if (myModel().imageHasValidPhysicalUnitScale()) {
			unitname = myModel().getImageScalePhysicalUnitName();
		}

		headers.add(String.format("Neck Length (%s)", unitname));
		headers.add(String.format("Neck Width (%s)", unitname));
		headers.add(String.format("Head Width (%s)", unitname));

		headers.add("Notes");

		// https://www.geeksforgeeks.org/arraylist-toarray-method-in-java-with-examples/
		String arr[] = new String[headers.size()];
		arr = headers.toArray(arr);

		return arr;
	}

	private void populateDetailsTable() {
		detailsTableColumns = generateDetailsTableColumns();

		List<DendriteSpine> spines = myModel().getSpines();
		detailsTableData = new Object[myModel().getSpines().size()][detailsTableColumns.length];

		for (int iSpine = 0; iSpine < spines.size(); iSpine++) {
			Object[] row = detailsTableData[iSpine];

			DendriteSpine spine = spines.get(iSpine);

			int iCol = 0;
			row[iCol++] = spine.getId();

			String dendriteName = null;
			DendriteBranch dendrite = spine.getNearestDendrite();
			if (dendrite != null) {
				dendriteName = dendrite.getName();
			}

			row[iCol++] = dendriteName;

			row[iCol++] = spine.getClassification();

			row[iCol++] = String.format("%.3f",
					myModel().convertImageScaleFromPixelsToPhysicalUnits(spine.neckLengthInPixels));
			row[iCol++] = String.format("%.3f",
					myModel().convertImageScaleFromPixelsToPhysicalUnits(spine.neckWidthInPixels));
			row[iCol++] = String.format("%.3f",
					myModel().convertImageScaleFromPixelsToPhysicalUnits(spine.headWidthInPixels));

			row[iCol++] = spine.notes;
		}

		this.detailsTable = new JTable(detailsTableData, detailsTableColumns);
		this.detailsTableHolder.setViewportView(detailsTable);
		this.detailsTable.setFillsViewportHeight(true);
	}

	private static String getTableExport(Object[][] tableData, String[] tableColumns, String delimiter) {
		String s = "";

		if (tableColumns != null) {
			for (String columnName : tableColumns) {
				s += columnName + delimiter;
			}

			// Trim the last tab.
			s = s.substring(0, s.length() - 1);
			s += "\n";
		}

		for (Object[] row : tableData) {
			for (Object rowItem : row) {
				s += rowItem.toString() + delimiter;
			}

			s = s.substring(0, s.length() - 1);
			s += "\n";
		}
		return s;
	}

	private String copyToClipboard(String s) {
		// https://stackoverflow.com/questions/6710350/copying-text-to-the-clipboard-using-java
		StringSelection stringSelection = new StringSelection(s);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
		return s;
	}

	private void exportFileData(String sPrompt, String sdata) {
		// https://stackoverflow.com/questions/7211107/how-to-use-filedialog
		FileDialog fd = new FileDialog(controlPanel, sPrompt, FileDialog.SAVE);
		fd.setFile("*.csv");
		fd.setVisible(true);
		String filename = fd.getFile();
		if (filename != null && filename.length() > 0) {
			filename = fd.getDirectory() + fd.getFile();
			if (!filename.contains(".")) {
				filename += ".csv";
			}
			try {
				// https://www.w3schools.com/java/java_files_create.asp
				FileWriter myWriter = new FileWriter(filename);
				myWriter.write(sdata);
				myWriter.close();
			} catch (IOException ex) {
				System.out.println("An error occurred while writing to file: " + filename);
				ex.printStackTrace();
			}
		}
	}

}
