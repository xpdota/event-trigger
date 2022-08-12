package gg.xp.xivsupport.callouts.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.CalloutGroup;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.ModifiedCalloutRepository;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ScanMe
public class CalloutsConfigTab implements PluginTab {


	private final ModifiedCalloutRepository backend;
	private final SoundFilesManager soundMgr;

	public CalloutsConfigTab(ModifiedCalloutRepository backend, SoundFilesManager soundMgr) {
		this.backend = backend;
		this.soundMgr = soundMgr;
	}

	@Override
	public String getTabName() {
		return "Callouts (Legacy)";
	}

	@Override
	public int getSortOrder() {
		return 1;
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel("Callouts");
		outerPanel.setLayout(new BorderLayout());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new WrapLayout());

		JCheckBox enableTts = new BooleanSettingGui(backend.getEnableTts(), "Enable TTS").getComponent();
		settingsPanel.add(enableTts);
		JCheckBox enableOverlay = new BooleanSettingGui(backend.getEnableOverlay(), "Enable Overlay").getComponent();
		settingsPanel.add(enableOverlay);
		JButton expandAll = new JButton("Expand All");
		settingsPanel.add(expandAll);
		JButton collapseAll = new JButton("Collapse All");
		settingsPanel.add(collapseAll);

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

		JPanel innerPanel = new JPanel();
		enableTts.addActionListener(l -> innerPanel.repaint());
		enableOverlay.addActionListener(l -> innerPanel.repaint());
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		List<CalloutGroup> calloutMap = backend.getAllCallouts();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		c.ipadx = 5;
		c.gridy = 3;
		calloutMap.forEach((group) -> {
			List<ModifiedCalloutHandle> callouts = group.getCallouts();
			c.gridx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			JPanel groupControls = new JPanel(new GridBagLayout());
			GridBagConstraints subC = new GridBagConstraints(0, 0, 1, 0, 0, 0, GridBagConstraints.WEST, 0, new Insets(0, 0, 0, 0), 10, 0);
			JCheckBox topLevelCheckbox;
			{
				topLevelCheckbox = new BooleanSettingGui(group.getEnabled(), group.getName()).getComponent();
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
				collapseAll.addActionListener(l -> showHide.setSelected(false));
				expandAll.addActionListener(l -> showHide.setSelected(true));
			}
			c.weightx = 1;
			innerPanel.add(groupControls, c);
			c.weightx = 0;
			c.gridwidth = 1;
			List<CalloutSettingGui> csgs = new ArrayList<>();
			callouts.forEach(call -> {
				c.weightx = 0;
				c.gridy++;
				c.gridx = 1;
				innerPanel.add(Box.createHorizontalStrut(10), c);
				c.gridx++;
				CalloutSettingGui csg = new CalloutSettingGui(call, soundMgr);
				showHide.getModel().addChangeListener(l -> {
					csg.setVisible(showHide.isSelected());
				});

				csgs.add(csg);

				innerPanel.add(csg.getCallCheckbox(), c);

				c.gridx++;
				c.weightx = 1;
				innerPanel.add(csg.getTtsPanel(), c);
				c.weightx = 0;
				c.gridx++;
				innerPanel.add(Box.createHorizontalStrut(10), c);
				c.gridx++;
				c.weightx = 1;
				innerPanel.add(csg.getTextPanel(), c);
				c.gridx++;
				c.weightx = 0;
				innerPanel.add(csg.getColorPicker(), c);
			});
			csgs.forEach(csg -> csg.setEnabledByParent(topLevelCheckbox.isSelected()));
			topLevelCheckbox.addActionListener(l -> {
				csgs.forEach(csg -> csg.setEnabledByParent(topLevelCheckbox.isSelected()));
				group.updateChildren();
			});
			c.gridx++;
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			// Add dummy to pad out the right side
			innerPanel.add(Box.createHorizontalStrut(2), c);
			c.gridy++;
		});
		c.weighty = 1;
		innerPanel.add(new JPanel(), c);
		innerPanel.setPreferredSize(innerPanel.getMinimumSize());
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(scroll);
		return outerPanel;
	}
}
