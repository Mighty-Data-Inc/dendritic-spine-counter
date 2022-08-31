package com.MightyDataInc.DendriticSpineCounter.UI.tabpanels;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;

import com.MightyDataInc.DendriticSpineCounter.Dendritic_Spine_Counter;
import com.MightyDataInc.DendriticSpineCounter.model.DscModel;

public abstract class DscBasePanel extends JPanel {
	private static final long serialVersionUID = 5162415477182496895L;

	protected JTabbedPane tabbedPane;
	protected DscModel model;
	protected Dendritic_Spine_Counter dscPlugin;

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

	public DscBasePanel(JTabbedPane tabbedPane, Dendritic_Spine_Counter dscplugin, DscModel model) {
		this.tabbedPane = tabbedPane;
		this.dscPlugin = dscplugin;
		this.model = model;

		init();
		update();

		ActionListener fnKeepUpdated = new ActionListener() {
			public void actionPerformed(ActionEvent evnt) {
				onTimer();
			}
		};
		Timer timer = new Timer(1000, fnKeepUpdated); // timer is ticking
		timer.setRepeats(true);
		timer.start();
	}

	protected void addNextButton(String labeltext, String pathToIcon) {
		ImageIcon myIcon = new ImageIcon(getClass().getClassLoader().getResource(pathToIcon));

		JButton btnNext = new JButton(labeltext, myIcon);

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
		gridbagConstraints.gridy++;
		gridbagConstraints.gridx = 0;
		gridbagConstraints.gridwidth = GridBagConstraints.REMAINDER;
		gridbagConstraints.gridheight = GridBagConstraints.REMAINDER;
		gridbagConstraints.anchor = GridBagConstraints.PAGE_END;
		gridbagConstraints.weighty = 1.0;
		this.add(btnNext, gridbagConstraints);
	}

	protected void onTimer() {
	}
}
