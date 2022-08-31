package com.MightyDataInc.DendriticSpineCounter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
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

	public DscModel model;

	// TODO: Don't expose dlg. Expose model, and make model ping dlg.
	// Maybe leave image processor exposed.
	public DscControlPanelDialog controlPanelDlg;
	public DscImageProcessor imageProcessor;

	public Dataset origDataset;

	public Tool polylineTool;
	public Tool pointTool;

	// This parameter determines how "different" a spine's brightness
	// can be from the brightness of the dendrite that the spine comes off of.
	// If it's set to zero, then the spine would have to be exactly the same
	// brightness (or darkness) as the central line of the dendrite. In terms
	// of units, it represents the number of standard deviations of difference
	// in pixel value samplings that the spine can have and still be considered
	// part of the same dendritic structure.
	public double FEATURE_SENSITIVITY_ALLOWANCE = 4.0;

	// How far out from the polyline to search for the thickness of the dendrite.
	// Expressed as a multiple of how many pixel window sizes to search out to.
	public double MAX_SEARCH_DISTANCE_IN_PIXEL_WINDOW_SIZE_TIMES = 5.0;

	// How big the features to scan for are, in pixels.
	public int featureSizePixels = 5;

	// The ID value to assign to the next path that gets added to our overlay.
	// Incremented every time we add a path. Never decremented.
	// TODO: MOve this into the model.
	public int nextPathId = 1;

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
				jsonObj = DscControlPanelDialog.getJsonObjectFromFile(fileJsonPath);

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

		model = new DscModel();

		controlPanelDlg = new DscControlPanelDialog(this, model);

		imageProcessor = new DscImageProcessor(this);
		imageProcessor.createWorkingImage(origDataset, this.displayService);

		Calibration cal = imageProcessor.getDimensions();
		model.setImageScale(cal);
		controlPanelDlg.update();

		if (jsonObj != null) {
			controlPanelDlg.loadFromJsonObject(jsonObj);
		}
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
		this.controlPanelDlg.update();
	}

	public void activatePolylineTool() {
		polylineTool.activate();
		toolService.setActiveTool(polylineTool);
		IJ.setTool("polyline");
	}

	public void runScaleSettingDialog() {
		this.displayService.setActiveDisplay(this.imageProcessor.getDisplay());
		Executer executer = new Executer("Set Scale...");
		executer.run();

		//updateUI();
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