package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;
import com.MightyDataInc.DendriticSpineCounter.UI.DscImageProcessor;
import com.MightyDataInc.DendriticSpineCounter.model.DendriteSpine;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

public class ClassifySpinesPanel extends DscBasePanel {
	private static final long serialVersionUID = 810474861208518668L;

	private DendriteSpine currentSpine;

	private BufferedImage imgSpine;
	private JLabel lblSpineImg;
	private JLabel lblSpineId;
	private int imgSpineSize;

	private JButton btnPrevSpine;
	private JButton btnNextSpine;
	private JButton btnNextUnclassified;

	private JProgressBar progressBar;

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

		this.setLayout(new GridBagLayout());

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.insets.top = 8;
		gridbagConstraints.insets.bottom = 4;
		gridbagConstraints.insets.left = 4;
		gridbagConstraints.insets.right = 4;

		{
			gridbagConstraints.gridx = 0;

			gridbagConstraints.gridwidth = 2;
			gridbagConstraints.gridheight = 6;
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

			gridbagConstraints.gridx = 2;
			gridbagConstraints.gridheight = 1;

			progressBar = new JProgressBar();
			this.add(progressBar, gridbagConstraints);
			gridbagConstraints.gridy++;

			lblSpineId = new JLabel();
			this.add(lblSpineId, gridbagConstraints);
			gridbagConstraints.gridy++;

			btnPrevSpine = new JButton("← Previous Spine");
			btnPrevSpine.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentSpine = model.findPreviousSpine(currentSpine);
					update();
				}
			});
			gridbagConstraints.gridx = 2;
			gridbagConstraints.gridwidth = 1;
			this.add(btnPrevSpine, gridbagConstraints);

			btnNextSpine = new JButton("Next Spine →");
			btnNextSpine.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					currentSpine = model.findNextSpine(currentSpine);
					update();
				}
			});
			gridbagConstraints.gridx = 3;
			this.add(btnNextSpine, gridbagConstraints);

			gridbagConstraints.gridy++;

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
		if (currentSpine == null) {
			lblSpineId.setText("No current dendritic spine selected.");
		} else {
			lblSpineId.setText(String.format("Dendritic Spine #%d: (%.1f,%.1f), width %.1f, orientation %.3f",
					currentSpine.getId(), currentSpine.getX(), currentSpine.getY(), currentSpine.getSize(),
					currentSpine.angle));
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
	}

	@Override
	protected void onPanelEntered() {
		updateCurrentSpineToAvailable();
		update();
	}

	@Override
	protected void onPanelExited() {
	}

	private void renderSpineImage() {
		DscImageProcessor imageProcessor = controlPanel.getPlugin().getImageProcessor();
		imgSpine = new BufferedImage(imgSpineSize, imgSpineSize, BufferedImage.TYPE_INT_ARGB);

		double pixelScale = 1;
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
				// NOTE: We invert the y coordinate because we want the dendrite body to be on
				// the bottom of the display and the spine to stick up.
				double xRel = (x - imgSpineSize / 2) * pixelScale;
				double yRel = -(y - imgSpineSize / 2) * pixelScale;

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
}
