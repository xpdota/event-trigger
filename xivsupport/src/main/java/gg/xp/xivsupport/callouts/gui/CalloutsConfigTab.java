package gg.xp.xivsupport.callouts.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.DotBuff;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.callouts.ModifiedCalloutRepository;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class CalloutsConfigTab implements PluginTab {


	private final ModifiedCalloutRepository backend;

	public CalloutsConfigTab(ModifiedCalloutRepository backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Callouts";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel("Callouts");
		outerPanel.setLayout(new BorderLayout());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new WrapLayout());

//		JPanel preTimeBox = new LongSettingGui(backend.getDotRefreshAdvance(), "Time before expiry to call out (milliseconds)").getComponent();
//		settingsPanel.add(preTimeBox);
//		JCheckBox enableTts = new BooleanSettingGui(backend.getEnableTts(), "Enable TTS").getComponent();
//		settingsPanel.add(enableTts);
//		JCheckBox enableOverlay = new BooleanSettingGui(backend.getEnableOverlay(), "Enable Overlay").getComponent();
//		settingsPanel.add(enableOverlay);

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Map<String, List<ModifiedCalloutHandle>> calloutMap = backend.getAllCallouts();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		c.ipadx = 20;
		c.gridy = 0;
		calloutMap.forEach((desc, callouts) -> {
			c.gridx = 0;
			c.weightx = 0;
			// left filler
			c.gridwidth = 1;
			innerPanel.add(new JPanel(), c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			JLabel label = new JLabel(desc);
			c.gridx ++;
			c.weightx = 1;
			innerPanel.add(label, c);
			c.weightx = 0;
			c.gridwidth = 1;
			callouts.forEach(call -> {
				c.gridy ++;
				c.gridx = 1;
				innerPanel.add(new JPanel(), c);
				c.gridx ++;
				StringSetting ttsSetting = call.getTtsSetting();
				StringSetting textSetting = call.getTextSetting();

				innerPanel.add(new JLabel(call.getDescription()), c);

				c.gridx ++;
				innerPanel.add(new StringSettingGui(ttsSetting, "TTS").getComponent(), c);
				c.gridx ++;
				innerPanel.add(new StringSettingGui(textSetting, "Text").getComponent(), c);
				c.gridwidth = GridBagConstraints.REMAINDER;
				innerPanel.add(new JPanel(), c);
				c.gridwidth = 1;
			});
			c.gridx++;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			// Add dummy to pad out the right side
			JPanel dummyPanel = new JPanel();
			innerPanel.add(dummyPanel, c);
			c.gridy++;
		});
		c.weighty = 1;
		innerPanel.add(new JPanel(), c);
		innerPanel.setPreferredSize(innerPanel.getMinimumSize());
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(scroll);
		return outerPanel;
	}
}
