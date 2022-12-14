package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.CalloutGroup;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.callouts.audio.gui.SoundFileTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CalloutHelper extends JPanel {

	private final List<JCheckBox> showHides = new ArrayList<>();
	private final List<JCheckBox> topLevel = new ArrayList<>();

	public CalloutHelper(List<CalloutGroup> groups, SoundFilesManager soundMgr, SoundFileTab sft) {
//		enableTts.addActionListener(l -> this.repaint());
//		enableOverlay.addActionListener(l -> this.repaint());
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		c.ipadx = 5;
		c.gridy = 3;
		groups.forEach((group) -> {
			List<ModifiedCalloutHandle> callouts = group.getCallouts();
			c.gridx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			JPanel groupControls = new JPanel(new GridBagLayout());
			GridBagConstraints subC = new GridBagConstraints(0, 0, 1, 0, 0, 0, GridBagConstraints.WEST, 0, new Insets(0, 0, 0, 0), 10, 0);
			JCheckBox topLevelCheckbox;
			{
				topLevelCheckbox = new BooleanSettingGui(group.getEnabled(), group.getName()).getComponent();
				topLevel.add(topLevelCheckbox);
				groupControls.add(topLevelCheckbox, subC);
			}
			JCheckBox showHide;
			{
				showHide = new JCheckBox("Show/Hide");
				showHide.setSelected(true);
				subC.gridx++;
				subC.fill = GridBagConstraints.HORIZONTAL;
				subC.ipadx = 10;
				subC.weightx = 1;
				groupControls.add(showHide, subC);
				showHides.add(showHide);
//				collapseAll.addActionListener(l -> showHide.setSelected(false));
//				expandAll.addActionListener(l -> showHide.setSelected(true));
			}
			c.weightx = 1;
			this.add(groupControls, c);
			c.weightx = 0;
			c.gridwidth = 1;
			c.gridx = 2;
			JButton disableButton;
			JButton enableButton;
			{
				JPanel disEnAll = new JPanel(new FlowLayout(FlowLayout.LEFT));
				disableButton = new JButton("Disable All");
				enableButton = new JButton("Enable All");
				disEnAll.add(disableButton);
				disEnAll.add(enableButton);
				showHide.getModel().addChangeListener(l -> {
					disEnAll.setVisible(showHide.isSelected());
				});
				c.gridy++;
				this.add(disEnAll, c);
			}
			List<CalloutSettingGui> csgs = new ArrayList<>();
			callouts.forEach(call -> {
				c.weightx = 0;
				c.gridy++;
				c.gridx = 1;
				this.add(Box.createHorizontalStrut(10), c);
				c.gridx++;
				CalloutSettingGui csg = new CalloutSettingGui(call, soundMgr, sft);
				showHide.getModel().addChangeListener(l -> {
					csg.setVisible(showHide.isSelected());
				});

				csgs.add(csg);

				this.add(csg.getCallCheckbox(), c);

				c.gridx++;
				c.weightx = 1;
				this.add(csg.getTtsPanel(), c);
				c.weightx = 0;
				c.gridx++;
				this.add(Box.createHorizontalStrut(10), c);
				c.gridx++;
				c.weightx = 1;
				this.add(csg.getTextPanel(), c);

				c.gridy++;
				c.gridx = 3;
				this.add(csg.getSoundPanel(), c);
				c.gridx += 2;
				this.add(csg.getColorPickerPanel(), c);

			});
			csgs.forEach(csg -> csg.setEnabledByParent(topLevelCheckbox.isSelected()));
			topLevelCheckbox.getModel().addChangeListener(l -> {
				csgs.forEach(csg -> csg.setEnabledByParent(topLevelCheckbox.isSelected()));
				group.updateChildren();
			});
			disableButton.addActionListener(l -> {
				callouts.forEach(co -> co.getEnable().set(false));
			});
			enableButton.addActionListener(l -> {
				callouts.forEach(co -> co.getEnable().set(true));
			});
			c.gridx++;
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			// Add dummy to pad out the right side
			this.add(Box.createHorizontalStrut(2), c);
			c.gridy++;
		});
		c.weighty = 1;
		this.add(new JPanel(), c);
	}

	public void setAllShowHide(boolean showHide) {
		SwingUtilities.invokeLater(() -> showHides.forEach(sh -> sh.setSelected(showHide)));
	}

	public void setAllEnableDisable(boolean enable) {
		SwingUtilities.invokeLater(() -> topLevel.forEach(sh -> sh.setSelected(enable)));
	}

}
