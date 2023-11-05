package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.CalloutGroup;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.callouts.audio.gui.SoundFileTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CalloutHelper extends JPanel implements Scrollable {

	private final List<JCheckBox> showHides = new ArrayList<>();
	private final List<JCheckBox> topLevel = new ArrayList<>();
	private final List<CalloutGroup> groups;

	public CalloutHelper(List<CalloutGroup> groups, SoundFilesManager soundMgr, SoundFileTab sft) {
		this(groups, soundMgr, sft, List.of());
	}

	public CalloutHelper(List<CalloutGroup> groups, SoundFilesManager soundMgr, SoundFileTab sft, PicoContainer container) {
		this(groups, soundMgr, sft, container.getComponents(ExtraCalloutAction.class));
	}
	public CalloutHelper(List<CalloutGroup> groups, SoundFilesManager soundMgr, SoundFileTab sft, List<ExtraCalloutAction> extras) {
//		enableTts.addActionListener(l -> this.repaint());
//		enableOverlay.addActionListener(l -> this.repaint());
		this.setLayout(new GridBagLayout());
		this.groups = new ArrayList<>(groups);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		c.ipadx = 5;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		JPanel settingsPanel = new JPanel();
		JButton expandAll = new JButton("Expand All");
		expandAll.addActionListener(l -> setAllShowHide(true));
		settingsPanel.add(expandAll);

		JButton collapseAll = new JButton("Collapse All");
		collapseAll.addActionListener(l -> setAllShowHide(false));
		settingsPanel.add(collapseAll);

		JButton enableAll = new JButton("Enable All Groups");
		enableAll.addActionListener(l -> setAllEnableDisable(true));
		settingsPanel.add(enableAll);

		JButton disableAll = new JButton("Disable All Groups");
		disableAll.addActionListener(l -> setAllEnableDisable(false));
		settingsPanel.add(disableAll);

		JButton resetAll = new JButton("Reset Enabled/Disabled Status");
		resetAll.addActionListener(l -> askThenReset());
		settingsPanel.add(resetAll);


		add(settingsPanel, c);
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;


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
			List<CalloutSettingGui> csgs = new ArrayList<>();
			callouts.forEach(call -> {
				c.weightx = 0;
				c.gridy++;
				c.gridx = 1;
				this.add(Box.createHorizontalStrut(10), c);
				c.gridx++;
				CalloutSettingGui csg = new CalloutSettingGui(call, soundMgr, sft, extras);
				showHide.getModel().addChangeListener(l -> {
					csg.setVisible(showHide.isSelected());
				});

				csgs.add(csg);

				c.gridwidth = GridBagConstraints.REMAINDER;

				JCheckBox callCheckbox = csg.getCallCheckbox();
				Component extendedDescription = csg.getExtendedDescription();
				this.add(callCheckbox, c);

				if (extendedDescription != null) {
					c.gridy++;
					c.gridx += 2;
					this.add(extendedDescription, c);
					c.gridx -= 2;
				}

				c.gridwidth = 1;
				c.gridy++;
				c.gridx++;
//				this.add(new JLabel("Foo"), c);
				this.add(Box.createHorizontalStrut(50), c);
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
				c.gridx = 4;
				this.add(csg.getSoundPanel(), c);
				c.gridx += 2;
				this.add(csg.getColorPickerAndActionsPanel(), c);

			});
			csgs.forEach(csg -> csg.setEnabledByParent(topLevelCheckbox.isSelected()));
			topLevelCheckbox.getModel().addChangeListener(l -> {
				csgs.forEach(csg -> csg.setEnabledByParent(topLevelCheckbox.isSelected()));
				group.updateChildren();
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

	private void askThenReset() {
		SwingUtilities.invokeLater(() -> {
			int result = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), "Are you sure you want to reset all enabled/disabled settings on this page? This cannot be reverted!", "Reset?", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				groups.forEach(CalloutGroup::resetAllBooleans);
			}
			SwingUtilities.invokeLater(() -> {
				updateUI();
				repaint();
			});
		});
	}

	public void setAllShowHide(boolean showHide) {
		SwingUtilities.invokeLater(() -> showHides.forEach(sh -> sh.setSelected(showHide)));
	}

	public void setAllEnableDisable(boolean enable) {
		SwingUtilities.invokeLater(() -> topLevel.forEach(sh -> sh.setSelected(enable)));
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 20;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (int) (getVisibleRect().height / 2.5);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
