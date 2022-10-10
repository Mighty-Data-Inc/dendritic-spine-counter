package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

import com.MightyDataInc.DendriticSpineCounter.UI.DscControlPanelDialog;

public abstract class DscBasePanel extends JPanel {
	private static final long serialVersionUID = 5162415477182496895L;

	private boolean isActive = false;

	protected DscControlPanelDialog controlPanel;

	protected GridBagConstraints standardPanelGridbagConstraints() {
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

	public abstract JPanel init();

	public abstract void update();

	public DscControlPanelDialog getControlPanel() {
		return this.controlPanel;
	}

	public DscBasePanel(DscControlPanelDialog controlPanel) {
		this.controlPanel = controlPanel;

		init();
		update();

		ActionListener fnKeepUpdated = new ActionListener() {
			public void actionPerformed(ActionEvent evnt) {
				if (isActive) {
					onTimer();
				}
			}
		};
		Timer timer = new Timer(1000, fnKeepUpdated); // timer is ticking
		timer.setRepeats(true);
		timer.start();
	}

	protected void addNextButton(String labeltext, String pathToIcon) {
		JTabbedPane tabbedPane = this.getControlPanel().getTabbedPane();

		ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToIcon));

		JButton btnNext = new JButton(labeltext + " >>", myIcon);

		btnNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int iCurrentTab = tabbedPane.getSelectedIndex();
				if (iCurrentTab >= tabbedPane.getTabCount() - 1) {
					return;
				}
				tabbedPane.setSelectedIndex(iCurrentTab + 1);
			}
		});

		GridBagConstraints gridbagConstraints = standardPanelGridbagConstraints();
		gridbagConstraints.gridy = 10;
		gridbagConstraints.gridx = 0;
		gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
		gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
		gridbagConstraints.weighty = 1.0;
		this.add(btnNext, gridbagConstraints);
	}

	public boolean IsActive() {
		return this.isActive;
	}

	public void enterPanel() {
		if (this.isActive) {
			return;
		}
		this.isActive = true;
		this.onPanelEntered();
	}

	public void exitPanel() {
		if (!this.isActive) {
			return;
		}
		this.isActive = false;
		this.onPanelExited();
	}

	protected void onPanelEntered() {
	}

	protected void onPanelExited() {
	}

	protected void onTimer() {
	}
}
