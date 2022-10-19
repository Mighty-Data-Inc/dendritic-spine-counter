package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.UI.DscImageProcessor;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

public class ClassifySpinesPanel extends DscBasePanel {
	private static final long serialVersionUID = 810474861208518668L;

	private DendriteSpine currentSpine;

	private BufferedImage imgSpine;
	private Graphics2D gfxSpine;

	private JLabel lblSpineImg;
	private JLabel lblViewportInfo;
	private JLabel lblSpineId;
	private JLabel lblDendriteId;
	private int imgSpineSize;

	private JButton btnPrevSpine;
	private JButton btnNextSpine;
	private JButton btnNextUnclassified;

	private JProgressBar progressBar;

	private String currentTool = "pan";

	private Point2D ptDragStart;
	private Point2D ptDragSpineOrigPosition;

	private JButton btnPan;
	private JButton btnMeasure;

	private JLabel lblMeasurement;
	private double measurement;

	private JRadioButton radioNeckLength;
	private JRadioButton radioNeckWidth;
	private JRadioButton radioHeadWidth;
	private ButtonGroup radioMeasurementButtonGroup;

	private JComboBox<String> comboSpineClass;

	private JTextArea textNotes;

	public ClassifySpinesPanel(DscControlPanelDialog controlPanel) {
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
		this.imgSpineSize = 300;

		ptDragStart = null;
		ptDragSpineOrigPosition = null;

		this.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.bottom = 2;
		gridbagConstraints.insets.left = 4;
		gridbagConstraints.insets.right = 4;

		{
			JPanel leftcol = new JPanel();
			leftcol.setLayout(new BoxLayout(leftcol, BoxLayout.PAGE_AXIS));

			gridbagConstraints.gridx = 0;

			gridbagConstraints.gridwidth = 6;
			gridbagConstraints.gridheight = 20;
			gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;
			this.add(leftcol, gridbagConstraints);

			imgSpine = new BufferedImage(imgSpineSize, imgSpineSize, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < imgSpineSize; y++) {
				for (int x = 0; x < imgSpineSize; x++) {
					imgSpine.setRGB(x, y, Integer.MAX_VALUE);
				}
			}
			ImageIcon imgSpineIcon = new ImageIcon(imgSpine);

			lblSpineImg = new JLabel();
			lblSpineImg.setIcon(imgSpineIcon);
			leftcol.add(lblSpineImg);
			leftcol.setAlignmentX(JLabel.LEFT_ALIGNMENT);

			lblSpineImg.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {
					setImageCursor(null, arg0.isAltDown(), arg0.isControlDown());
				}

				@Override
				public void mouseExited(MouseEvent arg0) {
				}

				@Override
				public void mousePressed(MouseEvent arg0) {
					// Make sure we're drawing or dragging on a clean image.
					renderSpineImage();

					measurement = 0;
					displayCurrentMeasurement();

					ptDragStart = new Point2D.Double(arg0.getX(), arg0.getY());
					ptDragSpineOrigPosition = (currentSpine == null) ? null
							: new Point2D.Double(currentSpine.getX(), currentSpine.getY());
				}

				@Override
				public void mouseReleased(MouseEvent arg0) {
					ptDragStart = null;
					ptDragSpineOrigPosition = null;

					if (currentSpine != null) {
						if (radioNeckLength.isSelected()) {
							currentSpine.neckLengthInPixels = measurementInImagePixels();
							radioNeckWidth.doClick();
						} else if (radioNeckWidth.isSelected()) {
							currentSpine.neckWidthInPixels = measurementInImagePixels();
							radioHeadWidth.doClick();
						} else if (radioHeadWidth.isSelected()) {
							currentSpine.headWidthInPixels = measurementInImagePixels();
							radioNeckLength.doClick();
						}

						autoClassifyCurrentSpine();

						// Clear the drawing.
						renderSpineImage();
					}

					displayMeasurements();
				}
			});
			lblSpineImg.addMouseMotionListener(new MouseMotionListener() {
				@Override
				public void mouseDragged(MouseEvent arg0) {
					String tool = currentTool;
					if (arg0.isAltDown()) {
						tool = "measure";
						lblSpineImg.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
					} else if (arg0.isControlDown()) {
						tool = "pan";
						lblSpineImg.setCursor(new Cursor(Cursor.MOVE_CURSOR));
					}
					onSpineImageDragged(arg0.getX(), arg0.getY(), tool);
					displayCurrentMeasurement();
				}

				@Override
				public void mouseMoved(MouseEvent arg0) {
					setImageCursor(null, arg0.isAltDown(), arg0.isControlDown());
					displayCurrentMeasurement();
				}
			});
			lblSpineImg.addMouseWheelListener(new MouseWheelListener() {
				@Override
				public void mouseWheelMoved(MouseWheelEvent arg0) {
					int wheelRotationDir = arg0.getWheelRotation();
					if (wheelRotationDir < 0) {
						// Zoom in.
						changeSpineSize(0.9);
						update();
					} else if (wheelRotationDir > 0) {
						// Zoom out.
						changeSpineSize(1.1);
						update();
					}
				}
			});

			// https://stackoverflow.com/questions/8335997/how-can-i-add-a-space-in-between-two-buttons-in-a-boxlayout
			leftcol.add(Box.createRigidArea(new Dimension(0, 8)));

			lblViewportInfo = new JLabel("Viewport info:");
			leftcol.add(lblViewportInfo);

			leftcol.add(Box.createRigidArea(new Dimension(0, 8)));
			leftcol.add(new JLabel("Spine classification:"));

			comboSpineClass = new JComboBox<String>();
			comboSpineClass.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			leftcol.add(comboSpineClass);
			comboSpineClass.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent arg0) {
					if (currentSpine == null) {
						return;
					}
					String selectedItem = (String) comboSpineClass.getSelectedItem();
					if (selectedItem == null || !myModel().spineClasses.contains(selectedItem)) {
						currentSpine.setClassification(null);
					} else {
						currentSpine.setClassification(selectedItem);
					}
					updateClassificationProgressBar();
				}
			});

			leftcol.add(Box.createRigidArea(new Dimension(0, 8)));
			JButton btnEditSpineClasses = new JButton("Edit spine classes");
			leftcol.add(btnEditSpineClasses);
			btnEditSpineClasses.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					editSpineClasses();
				}
			});

			leftcol.add(Box.createRigidArea(new Dimension(0, 16)));
			leftcol.add(new JLabel("Notes about this spine (optional)"));

			textNotes = new JTextArea();
			textNotes.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			textNotes.setLineWrap(true);
			textNotes.setWrapStyleWord(true);
			leftcol.add(textNotes);
			textNotes.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent arg0) {
				}

				@Override
				public void insertUpdate(DocumentEvent arg0) {
					String text = textNotes.getText();
					if (currentSpine != null) {
						currentSpine.notes = text;
					}
				}

				@Override
				public void removeUpdate(DocumentEvent arg0) {
				}
			});
		}

		{
			gridbagConstraints.gridx = 6;
			gridbagConstraints.gridy = 0;
			gridbagConstraints.gridheight = 1;

			progressBar = new JProgressBar();
			this.add(progressBar, gridbagConstraints);
			gridbagConstraints.gridy++;

			lblSpineId = new JLabel();
			this.add(lblSpineId, gridbagConstraints);
			gridbagConstraints.gridy++;

			lblDendriteId = new JLabel();
			gridbagConstraints.insets.top = 0;
			this.add(lblDendriteId, gridbagConstraints);
			gridbagConstraints.gridy++;

			btnPrevSpine = new JButton("← Previous Spine");
			btnPrevSpine.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentSpine = myModel().findPreviousSpine(currentSpine);
					radioNeckLength.doClick();
					update();
				}
			});
			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.insets.top = 8;
			this.add(btnPrevSpine, gridbagConstraints);

			btnNextSpine = new JButton("Next Spine →");
			btnNextSpine.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentSpine = myModel().findNextSpine(currentSpine);
					radioNeckLength.doClick();
					update();
				}
			});
			gridbagConstraints.gridx = 9;
			this.add(btnNextSpine, gridbagConstraints);

			JButton btnDeleteSpine = new JButton("🗑 Delete Spine");
			btnDeleteSpine.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean didDelete = deleteCurrentSpine();
					if (didDelete) {
						btnNextUnclassified.doClick();
					}
				}
			});
			gridbagConstraints.gridy++;
			gridbagConstraints.gridx = 6;
			gridbagConstraints.gridwidth = 2;
			this.add(btnDeleteSpine, gridbagConstraints);

			btnNextUnclassified = new JButton("Next Unclassified Spine →");
			btnNextUnclassified.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentSpine = myModel().findNextUnclassifiedSpine(currentSpine);
					radioNeckLength.doClick();
					update();
				}
			});
			gridbagConstraints.gridx = 9;
			gridbagConstraints.gridwidth = 2;
			this.add(btnNextUnclassified, gridbagConstraints);
		}

		{
			gridbagConstraints.gridx = 6;
			gridbagConstraints.gridwidth = 6;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 16;
			gridbagConstraints.insets.bottom = 8;
			this.add(new JSeparator(), gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 4;
			gridbagConstraints.insets.bottom = 4;
			this.add(new JLabel(
					"<html>Move, resize, or reorient the spine.<br/>(Controls are oriented relative to the spine viewport.)</html>"),
					gridbagConstraints);

			gridbagConstraints.gridy++;

			gridbagConstraints.gridx = 6;
			gridbagConstraints.gridwidth = 2;
			JButton btnRotLeft = new JButton("⭯  Rotate Left");
			btnRotLeft.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (currentSpine == null) {
						return;
					}
					currentSpine.angle -= 0.1;
					if (currentSpine.angle < -Math.PI) {
						currentSpine.angle += 2 * Math.PI;
					}
					update();
				}
			});
			this.add(btnRotLeft, gridbagConstraints);

			gridbagConstraints.gridx = 8;
			btnPan = new JButton("❖ Pan (Ctrl)");
			btnPan.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					currentTool = "pan";
					lblSpineImg.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				}
			});
			btnPan.setModel(new DefaultButtonModel() {
				private static final long serialVersionUID = 3972502560509913497L;

				@Override
				public boolean isPressed() {
					return currentTool.equals("pan");
				}
			});
			this.add(btnPan, gridbagConstraints);

			gridbagConstraints.gridx = 10;
			gridbagConstraints.gridheight = 1;
			JButton btnRotRight = new JButton("⭮  Rotate Right");
			btnRotRight.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (currentSpine == null) {
						return;
					}
					currentSpine.angle += 0.1;
					if (currentSpine.angle > Math.PI) {
						currentSpine.angle -= 2 * Math.PI;
					}
					update();
				}
			});
			this.add(btnRotRight, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 4;

			gridbagConstraints.gridx = 6;
			JButton btnZoomOut = new JButton("🔍- Zoom Out");
			btnZoomOut.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					changeSpineSize(1.1);
					update();
				}
			});
			this.add(btnZoomOut, gridbagConstraints);

			gridbagConstraints.gridx = 10;
			JButton btnZoomIn = new JButton("🔍+  Zoom In");
			btnZoomIn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					changeSpineSize(0.9);
					update();
				}
			});
			this.add(btnZoomIn, gridbagConstraints);
		}

		{
			gridbagConstraints.gridx = 6;
			gridbagConstraints.gridwidth = 6;
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 16;
			gridbagConstraints.insets.bottom = 16;
			this.add(new JSeparator(), gridbagConstraints);

			JLabel lblMeasureInstructions = new JLabel(
					"<html>" + "Click the \"📏 Measure\" button (or hold Alt), and then trace a path "
							+ "on the spine viewport. When you release the mouse button, the length of "
							+ "the traced path will be applied to the selected spine attribute.</html>");
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 4;
			gridbagConstraints.insets.bottom = 4;
			this.add(lblMeasureInstructions, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 2;
			btnMeasure = new JButton("📏 Measure (Alt)");
			btnMeasure.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					currentTool = "measure";
					lblSpineImg.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				}
			});
			btnMeasure.setModel(new DefaultButtonModel() {
				private static final long serialVersionUID = 3055893288090281829L;

				@Override
				public boolean isPressed() {
					return currentTool.equals("measure");
				}
			});
			this.add(btnMeasure, gridbagConstraints);

			lblMeasurement = new JLabel("Measurement:");
			lblMeasurement.setVerticalAlignment(JLabel.CENTER);
			gridbagConstraints.gridx += 2;
			gridbagConstraints.insets.top = 8;
			gridbagConstraints.gridwidth = 4;
			this.add(lblMeasurement, gridbagConstraints);

			radioNeckLength = new JRadioButton("Neck length:");
			gridbagConstraints.gridx = 6;
			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 6;
			this.add(radioNeckLength, gridbagConstraints);

			radioNeckWidth = new JRadioButton("Neck width:");
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 2;
			gridbagConstraints.insets.bottom = 2;
			this.add(radioNeckWidth, gridbagConstraints);

			radioHeadWidth = new JRadioButton("Head width:");
			gridbagConstraints.gridy++;
			this.add(radioHeadWidth, gridbagConstraints);

			radioMeasurementButtonGroup = new ButtonGroup();
			radioMeasurementButtonGroup.add(radioNeckLength);
			radioMeasurementButtonGroup.add(radioNeckWidth);
			radioMeasurementButtonGroup.add(radioHeadWidth);

			radioNeckLength.doClick();
		}

		addNextButton("Next: Generate Report", "images/icons/data-table-results-24.png");

		update();
		return this;
	}

	private void setImageCursor(String tool, boolean isAltDown, boolean isControlDown) {
		if (tool == null || tool.trim().length() == 0) {
			tool = this.currentTool;
		}

		if (isAltDown) {
			tool = "measure";
		} else if (isControlDown) {
			tool = "pan";
		}

		if (tool.equals("pan")) {
			lblSpineImg.setCursor(new Cursor(Cursor.MOVE_CURSOR));
		} else if (tool.equals("measure")) {
			lblSpineImg.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		}
	}

	@Override
	public void onTimer() {
	}

	@Override
	public void update() {
		DscImageProcessor imageProcessor = this.controlPanel.getPlugin().getImageProcessor();

		if (currentSpine == null) {
			lblSpineId.setText("No dendritic spine currently selected.");
			lblDendriteId.setText("");
		} else {
			lblSpineId
					.setText(String.format("Spine #%d: (%.1f,%.1f), width %.1f, orientation %.3f", currentSpine.getId(),
							currentSpine.getX(), currentSpine.getY(), currentSpine.getSize(), currentSpine.angle));

			if (currentSpine.getNearestDendrite() == null) {
				lblDendriteId.setForeground(Color.RED);
				lblDendriteId.setText("Spine is not linked to a dendrite branch.");
			} else {
				lblDendriteId.setForeground(Color.BLACK);
				lblDendriteId.setText("Linked to dendrite branch: " + currentSpine.getNearestDendrite().toString());
			}
		}

		updateClassificationProgressBar();

		renderSpineImage();
		if (currentSpine == null) {
			lblViewportInfo.setText("<html>Viewport info: <i>No spine currently selected.</i></html>");
		} else {
			String positionInfo = String.format("(%.0f, %.0f)∠%.2f°", currentSpine.getX(), currentSpine.getY(),
					Math.toDegrees(currentSpine.angle));

			double viewportWidthPixels = this.getPixelScale() * this.imgSpineSize;
			String zoomInfo = String.format("Width: %.2f pixel", viewportWidthPixels);

			if (myModel().imageHasValidPhysicalUnitScale()) {
				double physUnits = myModel().convertImageScaleFromPixelsToPhysicalUnits(viewportWidthPixels);
				zoomInfo += String.format(" (%.3f %s)", physUnits, myModel().getImageScalePhysicalUnitName());
			}

			lblViewportInfo.setText("<html>Viewport info: " + positionInfo + "<br/>" + zoomInfo + "</html>");
		}

		displayMeasurements();
		displayCurrentMeasurement();

		updateSpineComboBox();

		if (currentSpine == null) {
			this.textNotes.setEnabled(false);
			this.textNotes.setText("");
		} else {
			this.textNotes.setEnabled(true);
			this.textNotes.setText(currentSpine.notes);
		}

		imageProcessor.getDisplay().update();
		imageProcessor.drawDendriteOverlays();
		imageProcessor.drawCurrentSpineIndicator(currentSpine);
	}

	private void updateClassificationProgressBar() {
		int numSpines = myModel().getSpines().size();
		int numSpinesClassified = numSpines - myModel().getUnclassifiedSpines().size();

		progressBar.setStringPainted(true);
		progressBar.setString(String.format("Classified %d of %d spines (%.1f%%)", numSpinesClassified, numSpines,
				(100.0 * numSpinesClassified / numSpines)));
		progressBar.setMaximum(numSpines);
		progressBar.setValue(numSpinesClassified);
	}

	@Override
	protected void onPanelEntered() {
		updateCurrentSpineToAvailable();
		update();
	}

	@Override
	protected void onPanelExited() {
		if (gfxSpine != null) {
			gfxSpine.dispose();
		}
	}

	private double getPixelScale() {
		double pixelScale = 1;
		if (currentSpine != null) {
			pixelScale = (currentSpine.getSize() * 1.25) / imgSpineSize;
		}
		return pixelScale;
	}

	private void changeSpineSize(double mult) {
		if (currentSpine == null) {
			return;
		}
		double newSize = currentSpine.getSize() * mult;

		double featureWindowSize = this.controlPanel.getPlugin().getModel().getFeatureWindowSizeInPixels();

		double sizeMin = featureWindowSize * 0.75f;
		double sizeMax = featureWindowSize * 4f;

		if (newSize < sizeMin) {
			newSize = sizeMin;
		}
		if (newSize > sizeMax) {
			newSize = sizeMax;
		}

		if (newSize < 5) {
			newSize = 5;
		}

		currentSpine.setSize(newSize);
	}

	private void renderSpineImage() {
		DscImageProcessor imageProcessor = controlPanel.getPlugin().getImageProcessor();
		imgSpine = new BufferedImage(imgSpineSize, imgSpineSize, BufferedImage.TYPE_INT_ARGB);

		if (gfxSpine != null) {
			gfxSpine.dispose();
		}
		gfxSpine = imgSpine.createGraphics();
		gfxSpine.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		gfxSpine.setStroke(new BasicStroke(5f));
		gfxSpine.setColor(Color.GREEN);

		double pixelScale = getPixelScale();
		if (currentSpine != null) {
			pixelScale = (currentSpine.getSize() * 1.25) / imgSpineSize;
		}

		for (int y = 0; y < imgSpineSize; y++) {
			for (int x = 0; x < imgSpineSize; x++) {
				if (this.currentSpine == null) {
					// Paint the field white.
					imgSpine.setRGB(x, y, Integer.MAX_VALUE);
					continue;
				}

				// Figure out what pixel coordinates on the source image correspond to each of
				// our xy coords.
				double xRel = (x - imgSpineSize / 2) * pixelScale;
				double yRel = (y - imgSpineSize / 2) * pixelScale;

				double angle = currentSpine.angle;

				double xRelRot = xRel * Math.cos(angle) + yRel * Math.sin(angle);
				double yRelRot = yRel * Math.cos(angle) - xRel * Math.sin(angle);

				double xImg = currentSpine.getX() + xRelRot;
				double yImg = currentSpine.getY() + yRelRot;

				double brightness = imageProcessor.getBrightnessAtFluidPixel(xImg, yImg);
				int v = (int) (brightness * 255);
				int rgb = (255 << 24) | (v << 16) | (v << 8) | v;
				imgSpine.setRGB(x, y, rgb);
			}
		}

		ImageIcon imgSpineIcon = new ImageIcon(imgSpine);
		lblSpineImg.setIcon(imgSpineIcon);
	}

	private void updateCurrentSpineToAvailable() {
		DscModel model = controlPanel.getPlugin().getModel();

		if (currentSpine != null && !model.hasSpine(currentSpine)) {
			// Make sure we're not showing the remnants of a deleted spine.
			currentSpine = null;
		}

		if (currentSpine == null) {
			currentSpine = model.findNextUnclassifiedSpine(null);
		}
		if (currentSpine == null) {
			currentSpine = model.findNextSpine(null);
		}
	}

	private double measurementInImagePixels() {
		double imagePixels = this.measurement * this.getPixelScale();
		return imagePixels;
	}

	private String getMeasurementHtmlString(double measurePixels) {
		String sMeasure = "<i>(none)</i>";
		if (measurePixels == 0) {
			return sMeasure;
		}

		sMeasure = String.format("%.1f px", measurePixels);

		DscModel model = this.controlPanel.getPlugin().getModel();
		if (!model.imageHasValidPhysicalUnitScale()) {
			return sMeasure;
		}

		double physicalUnits = model.convertImageScaleFromPixelsToPhysicalUnits(measurePixels);

		sMeasure += String.format(" (%.3f %s)", physicalUnits,
				this.controlPanel.getPlugin().getModel().getImageScalePhysicalUnitName());

		return sMeasure;
	}

	private void displayCurrentMeasurement() {
		String s = this.getMeasurementHtmlString(measurementInImagePixels());
		this.lblMeasurement.setText("<html>" + s + "</html>");
	}

	private void displayMeasurements() {
		double neckLength = 0;
		double neckWidth = 0;
		double headWidth = 0;

		if (this.currentSpine != null) {
			neckLength = this.currentSpine.neckLengthInPixels;
			neckWidth = this.currentSpine.neckWidthInPixels;
			headWidth = this.currentSpine.headWidthInPixels;
		}

		this.radioNeckLength.setText("<html>Neck length: " + this.getMeasurementHtmlString(neckLength) + "</html>");
		this.radioNeckWidth.setText("<html>Neck width: " + this.getMeasurementHtmlString(neckWidth) + "</html>");
		this.radioHeadWidth.setText("<html>Head width: " + this.getMeasurementHtmlString(headWidth) + "</html>");
	}

	private void onSpineImageDragged(int x, int y, String tool) {
		if (this.currentSpine == null || this.ptDragStart == null || this.ptDragSpineOrigPosition == null) {
			return;
		}
		double dragDiffX = x - this.ptDragStart.getX();
		double dragDiffY = y - this.ptDragStart.getY();

		if (tool == null || tool.trim().length() == 0) {
			tool = this.currentTool;
		}

		if (tool.equals("pan")) {
			double pixelScale = this.getPixelScale();
			double dragImagePositionDeltaX = dragDiffX * pixelScale;
			double dragImagePositionDeltaY = dragDiffY * pixelScale;

			double angle = currentSpine.angle;
			double moveX = dragImagePositionDeltaX * Math.cos(angle) + dragImagePositionDeltaY * Math.sin(angle);
			double moveY = dragImagePositionDeltaY * Math.cos(angle) - dragImagePositionDeltaX * Math.sin(angle);

			double newImgX = this.ptDragSpineOrigPosition.getX() - moveX;
			double newImgY = this.ptDragSpineOrigPosition.getY() - moveY;

			currentSpine.setLocation(newImgX, newImgY);
			update();
		} else if (tool.equals("measure")) {
			if (this.gfxSpine != null) {
				gfxSpine.drawLine((int) this.ptDragStart.getX(), (int) this.ptDragStart.getY(), x, y);
				lblSpineImg.repaint();

				Point2D pt = new Point2D.Double(x, y);

				double dist = pt.distance(this.ptDragStart);
				measurement += dist;
				displayCurrentMeasurement();

				this.ptDragStart = pt;
			}
		}
	}

	private boolean deleteCurrentSpine() {
		if (this.currentSpine == null) {
			return false;
		}

		String pathToImage = "images/icons/warning-icon.png";
		ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToImage));
		Image imgScaled = myIcon.getImage().getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH);

		int input = JOptionPane.showConfirmDialog(null,
				"<html><p>Delete current spine.</p><br/>"
						+ "<p>You've indicated that the current selection is not a spine, "
						+ "and it should be deleted from the list of spines.</p><br/><p>Are you sure?</p><br/>",
				"Delete current spine?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(imgScaled));

		// 0=yes, 1=no
		if (input == 1) {
			return false;
		}

		myModel().removeSpine(currentSpine);
		this.update();

		return true;
	}

	private void updateSpineComboBox() {
		String currentSpineClass = "";
		if (currentSpine != null) {
			// The current spine's class will get cleared when we remove all items,
			// so we need to make sure we remember it and then reapply it.
			currentSpineClass = this.currentSpine.getClassification();
		}

		comboSpineClass.removeAllItems();
		comboSpineClass.addItem("");

		if (currentSpine != null && currentSpineClass != null) {
			currentSpine.setClassification(currentSpineClass);
		}

		for (String spineClass : myModel().spineClasses) {
			comboSpineClass.addItem(spineClass);
		}

		if (this.currentSpine != null && this.currentSpine.hasClassification()) {
			if (!myModel().spineClasses.contains(currentSpineClass)) {
				currentSpineClass = "";
			}
			comboSpineClass.setSelectedItem(currentSpineClass);
		}
	}

	private void editSpineClasses() {
		String commalist = String.join(", ", myModel().spineClasses);

		String s = (String) JOptionPane.showInputDialog(null,
				"List the names of your spine classes, separated by commas.", "Edit spine classes",
				JOptionPane.PLAIN_MESSAGE, null, null, commalist);

		if (s == null || s.length() == 0) {
			// Cancelled.
			return;
		}

		s = s.trim();

		String[] parsedItems = s.split(", ");
		if (parsedItems.length == 0) {
			return;
		}

		String[] parsedItemsTrimmed = new String[parsedItems.length];
		for (int i = 0; i < parsedItems.length; i++) {
			parsedItemsTrimmed[i] = parsedItems[i].trim();
		}

		myModel().setSpineClasses(parsedItemsTrimmed);

		update();
	}

	private String autoClassifyCurrentSpine() {
		if (currentSpine == null) {
			return "";
		}

		String guessedclass = "";

		// Spine descriptions:
		// https://www.frontiersin.org/articles/10.3389/fnsyn.2020.00031/full
		if (currentSpine.neckLengthInPixels <= 2) {
			// "Stubby spines typically do not have a neck."
			guessedclass = "stubby";
		} else if (currentSpine.headWidthInPixels < currentSpine.neckWidthInPixels) {
			// "Filopodia are long, thin dendritic membrane protrusions without a clear
			// head..."
			guessedclass = "filopodia";
		} else if (currentSpine.headWidthInPixels > currentSpine.neckLengthInPixels) {
			// "Mushroom spines have a large head and a small neck..."
			guessedclass = "mushroom";
		} else {
			// "Thin spines have a structure similar to the mushroom spines,
			// but their head is smaller relative to the neck."
			guessedclass = "thin";
		}

		// Make sure that the guessed class is part of this project.
		if (!myModel().spineClasses.contains(guessedclass)) {
			guessedclass = "";
		}

		currentSpine.setClassification(guessedclass);
		this.updateClassificationProgressBar();
		this.updateSpineComboBox();

		return guessedclass;
	}
}
