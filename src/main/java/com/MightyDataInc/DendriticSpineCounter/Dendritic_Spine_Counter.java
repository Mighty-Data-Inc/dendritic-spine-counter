package com.MightyDataInc.DendriticSpineCounter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.json.simple.JSONObject;
import org.scijava.command.Command;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.display.event.input.InputEvent;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.tool.Tool;
import org.scijava.tool.ToolService;
import org.scijava.ui.UIService;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.UI.DscImageProcessor;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

import ij.Executer;
import ij.IJ;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import io.scif.SCIFIO;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;

@Plugin(type = Command.class, menuPath = "Plugins>Dendritic Spine Counter")
public class Dendritic_Spine_Counter implements PlugIn, SciJavaPlugin, Command {
	public String pomProjectVersion = "";

	private static final ImageJ imageJLegacy = new ImageJ();

	@Parameter
	ImageDisplayService imageDisplayService;

	@Parameter
	UIService uiService;

	@Parameter
	SCIFIO scifioService;

	@Parameter
	ToolService toolService;

	@Parameter
	DisplayService displayService;

	@Parameter
	OverlayService overlayService;

	private DscModel model;

	private DscControlPanelDialog controlPanelDlg;
	private DscImageProcessor imageProcessor;

	private Dataset origDataset;

	private Tool polylineTool;
	private Tool pointTool;

	public Tool getPolylineTool() {
		return this.polylineTool;
	}

	public Tool getPointTool() {
		return this.pointTool;
	}

	public String getCurrentToolName() {
		return IJ.getToolName();
	}

	public DscModel getModel() {
		return this.model;
	}

	public void setModel(DscModel model) {
		this.model = model;
		this.imageProcessor.setCurrentRoi(null);
		this.imageProcessor.drawDendriteOverlays();

		this.imageProcessor.update();
		this.imageProcessor.getDisplay().update();
	}

	public DscControlPanelDialog getControlPanel() {
		return this.controlPanelDlg;
	}

	public DscImageProcessor getImageProcessor() {
		return this.imageProcessor;
	}

	public Dataset getOriginalImage() {
		return this.origDataset;
	}

	// The IDE environment injects services with a "legacy" model, whereas
	// Fiji uses a "new" model. But it's inconsistent in both cases. It's maddening.
	void makeServicesWorkWithBothIDEAndFiji() {
		if (this.imageDisplayService == null) {
			this.imageDisplayService = imageJLegacy.imageDisplay();
		}
		if (this.uiService == null) {
			this.uiService = imageJLegacy.ui();
		}
		if (this.scifioService == null) {
			this.scifioService = imageJLegacy.scifio();
		}
		if (this.toolService == null) {
			this.toolService = imageJLegacy.tool();
		}
		if (this.displayService == null) {
			this.displayService = imageJLegacy.display();
		}
		if (this.overlayService == null) {
			this.overlayService = imageJLegacy.overlay();
		}
	}

	@Override
	public void run(String arg) {
		System.out.println(
				String.format("Method PlugIn.run was passed String argument: \"%s\". Argument was ignored.", arg));
		this.run();
	}

	@Override
	public void run() {
		try {
			final Properties properties = new Properties();
			InputStream propertiesStream = this.getClass().getClassLoader().getResourceAsStream("project.properties");
			properties.load(propertiesStream);
			this.pomProjectVersion = properties.getProperty("version");
		} catch (Exception e1) {
		}

		makeServicesWorkWithBothIDEAndFiji();

		JSONObject jsonObj = null;

		this.origDataset = imageDisplayService.getActiveDataset();
		if (this.origDataset == null) {
			final File file = uiService.chooseFile(null, "open");
			String filePathFromUserSelection = file.getPath();

			String datasetFilePath = filePathFromUserSelection;
			if (filePathFromUserSelection.toLowerCase().endsWith(".json")) {
				String fileJsonPath = filePathFromUserSelection;
				jsonObj = DscModel.getJsonObjectFromFile(fileJsonPath);

				datasetFilePath = "";
				if (jsonObj.containsKey("originalimagefile")) {
					datasetFilePath = jsonObj.get("originalimagefile").toString().trim();
				}
				if (datasetFilePath == null || datasetFilePath.isEmpty()) {
					JOptionPane.showMessageDialog(null,
							"This JSON file doesn't contain a record of which image file it corresponds to.",
							"Missing original file image record", JOptionPane.ERROR_MESSAGE);
					IJ.noImage();
					return;
				}
			}

			try {
				origDataset = scifioService.datasetIO().open(datasetFilePath);
			} catch (InvalidPathException e1) {
				JOptionPane.showMessageDialog(null, "Couldn't read this string as a file path: " + datasetFilePath,
						"Invalid path", JOptionPane.ERROR_MESSAGE);
			} catch (NoSuchFileException e1) {
				JOptionPane.showMessageDialog(null, "The system could not find any such file: " + datasetFilePath,
						"No such file", JOptionPane.ERROR_MESSAGE);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (origDataset == null) {
				IJ.noImage();
				return;
			}
			// show the image
			this.uiService.show(origDataset);
		}

		// Grab a reference to some tools, so that our control panel
		// can make use of them.
		polylineTool = this.toolService.getTool("Polyline");
		pointTool = this.toolService.getTool("Point");

		imageProcessor = new DscImageProcessor(this);
		imageProcessor.createWorkingImage(origDataset, this.displayService);

		if (jsonObj != null) {
			model = DscModel.loadFromJsonObject(jsonObj);
		}
		if (model == null) {
			model = new DscModel();
			Calibration cal = imageProcessor.getDimensions();
			model.setImageScale(cal);
		}

		controlPanelDlg = new DscControlPanelDialog(this, model);
		controlPanelDlg.update();
	}

	public Display<?> getOriginalDatasetDisplay() {
		List<Display<?>> origDatasetDisplays = this.displayService.getDisplays(this.origDataset);
		return origDatasetDisplays.get(0);
	}

	@EventHandler
	void onEvent(InputEvent ev) {
		if (ev.getDisplay() != this.imageProcessor.getDisplay()) {
			// This event wasn't on the display we care about.
			return;
		}
		// TODO: Do something with this event. It has x, y, and modifier info.
		System.out.println(String.format("Event: %s at (%d, %d)", ev.toString(), ev.getX(), ev.getY()));
		this.controlPanelDlg.update();
	}

	public void activatePolylineTool() {
		polylineTool.activate();
		toolService.setActiveTool(polylineTool);
		IJ.setTool("polyline");
	}

	public void activateMultiPointTool() {
		getImageProcessor().setCurrentRoi(null);
		IJ.setTool("multi-point");
		getImageProcessor().moveToForeground();
	}

	public void runScaleSettingDialog() {
		this.displayService.setActiveDisplay(this.imageProcessor.getDisplay());
		Executer executer = new Executer("Set Scale...");
		executer.run();
	}

	public String getApplicationVersion() {
		String versionStr = this.pomProjectVersion;
		if (versionStr == null) {
			return "";
		}
		return versionStr;
	}

	public void updateUI() {
		if (this.imageProcessor != null) {
			this.imageProcessor.update();
		}
		if (this.controlPanelDlg != null) {
			this.controlPanelDlg.update();
		}
	}

	public static void main(final String... args) throws Exception {
		// create the ImageJ application context with all available services
		imageJLegacy.ui().showUI();

		/*
		 * Dataset dataset = null; try { dataset =
		 * ij.scifio().datasetIO().open("C:\\\\Users\\mvol\\Desktop\\testpubz-MinIP.jpg"
		 * ); } catch(Exception ex) { }
		 * 
		 * if (dataset == null) { final File file = ij.ui().chooseFile(null, "open");
		 * String filePathFromUserSelection = file.getPath(); dataset =
		 * ij.scifio().datasetIO().open(filePathFromUserSelection); }
		 * 
		 * // show the image ij.ui().show(dataset);
		 */

		// invoke the plugin
		// ij.command().run(Dendritic_Spine_Counter.class, true);
		// Dendritic_Spine_Counter thePlugin = new Dendritic_Spine_Counter();
		// thePlugin.run( null );

		ij.IJ.runPlugIn("com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter", "unnecessaryArg");
	}

}