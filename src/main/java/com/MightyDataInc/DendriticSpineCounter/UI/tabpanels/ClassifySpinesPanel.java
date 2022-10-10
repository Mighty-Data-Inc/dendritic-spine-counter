package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.DefaultButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

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

	private JComboBox dropdownUnits;

	private ArrayList<String> spineClasses = new ArrayList<>(Arrays.asList("stubby", "mushroom", "thin", "filopodia"));

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
		DscModel model = controlPanel.getPlugin().getModel();

		ptDragStart = null;
		ptDragSpineOrigPosition = null;

		this.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.bottom = 2;
		gridbagConstraints.insets.left = 4;
		gridbagConstraints.insets.right = 4;

		{
			gridbagConstraints.gridx = 0;

			gridbagConstraints.gridwidth = 6;
			gridbagConstraints.gridheight = 16;
			gridbagConstraints.fill = GridBagConstraints.HORIZONTAL;

			imgSpine = new BufferedImage(imgSpineSize, imgSpineSize, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < imgSpineSize; y++) {
				for (int x = 0; x < imgSpineSize; x++) {
					imgSpine.setRGB(x, y, Integer.MAX_VALUE);
				}
			}
			ImageIcon imgSpineIcon = new ImageIcon(imgSpine);

			lblSpineImg = new JLabel();
			lblSpineImg.setIcon(imgSpineIcon);
			this.add(lblSpineImg, gridbagConstraints);
			lblSpineImg.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {
					if (currentTool == "pan") {
						lblSpineImg.setCursor(new Cursor(Cursor.MOVE_CURSOR));
					} else if (currentTool == "measure") {
						lblSpineImg.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
					}
				}

				@Override
				public void mouseExited(MouseEvent arg0) {
				}

				@Override
				public void mousePressed(MouseEvent arg0) {
					ptDragStart = new Point2D.Double(arg0.getX(), arg0.getY());
					ptDragSpineOrigPosition = (currentSpine == null) ? null
							: new Point2D.Double(currentSpine.getX(), currentSpine.getY());
				}

				@Override
				public void mouseReleased(MouseEvent arg0) {
					ptDragStart = null;
					ptDragSpineOrigPosition = null;
				}
			});
			lblSpineImg.addMouseMotionListener(new MouseMotionListener() {
				@Override
				public void mouseDragged(MouseEvent arg0) {
					onSpineImageDragged(arg0.getX(), arg0.getY());
				}

				@Override
				public void mouseMoved(MouseEvent arg0) {
				}
			});
			// lblSpineImg.addMouseWheelListener(sdf dsf sdf sd fasfd asdf sadf );

			gridbagConstraints.gridheight = 1;
			gridbagConstraints.gridy = 17;
			lblViewportInfo = new JLabel("Viewport info:");
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
					currentSpine = model.findPreviousSpine(currentSpine);
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
					currentSpine = model.findNextSpine(currentSpine);
					update();
				}
			});
			gridbagConstraints.gridx = 8;
			this.add(btnNextSpine, gridbagConstraints);

			btnNextUnclassified = new JButton("Next Unclassed →");
			btnNextUnclassified.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentSpine = model.findNextUnclassifiedSpine(currentSpine);
					update();
				}
			});
			gridbagConstraints.gridx = 10;
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
					"<html>Move, resize, or reorient the spine.<br/>(All movement controls are oriented relative to the spine viewport.)</html>"),
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
			btnPan = new JButton("❖ Pan");
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
					return currentTool == "pan";
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
					"<html>" + "Click the \"📏 Measure\" button, and then move your mouse cursor "
							+ "onto the viewport and hold down the mouse button to trace a path on "
							+ "the zoomed-in image. When you release the button, the length of the "
							+ "traced path will be copied into your copy-paste buffer, and you can "
							+ "paste it (Control-V or Command-V) into the corresponding measurement field.</html>");
			gridbagConstraints.gridy++;
			gridbagConstraints.insets.top = 4;
			gridbagConstraints.insets.bottom = 4;
			this.add(lblMeasureInstructions, gridbagConstraints);

			gridbagConstraints.gridy++;
			gridbagConstraints.gridwidth = 2;
			btnMeasure = new JButton("📏 Measure");
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
					return currentTool == "measure";
				}
			});
			this.add(btnMeasure, gridbagConstraints);

			JLabel lblUnits = new JLabel("Units: ", SwingConstants.RIGHT);
			gridbagConstraints.gridx += 2;
			this.add(lblUnits, gridbagConstraints);

			dropdownUnits = new JComboBox<String>();
			gridbagConstraints.gridx += 2;
			this.add(dropdownUnits, gridbagConstraints);

		}

		addNextButton("Next: Generate Report", "images/icons/data-table-results-24.png");

		update();
		return this;
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

		DscModel model = controlPanel.getPlugin().getModel();
		int numSpines = model.getSpines().size();
		int numSpinesClassified = numSpines - model.getUnclassifiedSpines().size();

		progressBar.setStringPainted(true);
		progressBar.setString(String.format("Classified %d of %d spines (%.1f%%)", numSpinesClassified, numSpines,
				(100.0 * numSpinesClassified / numSpines)));
		progressBar.setMaximum(numSpines);
		progressBar.setValue(numSpinesClassified);

		renderSpineImage();
		if (currentSpine == null) {
			lblViewportInfo.setText("<html>Viewport info: <i>No spine currently selected.</i></html>");
		} else {
			String positionInfo = String.format("(%.1f, %.1f)∠%.2f°", currentSpine.getX(), currentSpine.getY(),
					Math.toDegrees(currentSpine.angle));
			lblViewportInfo.setText("<html>Viewport info: " + positionInfo + "</html>");
		}

		imageProcessor.getDisplay().update();
		imageProcessor.drawDendriteOverlays();
		imageProcessor.drawCurrentSpineIndicator(currentSpine);

		imageProcessor.moveToForeground();
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

	private void onSpineImageDragged(int x, int y) {
		if (this.currentSpine == null || this.ptDragStart == null || this.ptDragSpineOrigPosition == null) {
			return;
		}
		double dragDiffX = x - this.ptDragStart.getX();
		double dragDiffY = y - this.ptDragStart.getY();

		if (this.currentTool == "pan") {
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
		}
	}
}
