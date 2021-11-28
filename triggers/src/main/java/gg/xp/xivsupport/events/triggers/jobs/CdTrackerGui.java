package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivdata.jobs.JobType;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class CdTrackerGui implements PluginTab {

	private final CdTracker backend;

	public CdTrackerGui(CdTracker backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Cooldown Tracker";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel("Cooldowns");
		outerPanel.setLayout(new BorderLayout());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new WrapLayout());

		JPanel preTimeBox = new LongSettingGui(backend.getCdTriggerAdvance(), "Time before expiry to call out (milliseconds)").getComponent();
		settingsPanel.add(preTimeBox);
		JCheckBox enableTts = new BooleanSettingGui(backend.getEnableTts(), "Enable TTS").getComponent();
		settingsPanel.add(enableTts);
		JCheckBox enableOverlay = new BooleanSettingGui(backend.getEnableOverlay(), "Enable Overlay").getComponent();
		settingsPanel.add(enableOverlay);

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Map<Cooldown, BooleanSetting> cooldowns = backend.getEnabledCds();
		Map<JobType, List<Cooldown>> byJobType = cooldowns.keySet().stream().filter(cd -> cd.getJobType() != null).collect(Collectors.groupingBy(Cooldown::getJobType));
		Map<Job, List<Cooldown>> byJob = cooldowns.keySet().stream().filter(cd -> cd.getJobType() == null).collect(Collectors.groupingBy(Cooldown::getJob));
		List<JobType> jobTypeKeys = byJobType.keySet().stream().sorted(Comparator.comparing(JobType::getFriendlyName)).collect(Collectors.toList());
		List<Job> jobKeys = byJob.keySet().stream().sorted(Comparator.comparing(Job::getFriendlyName)).collect(Collectors.toList());
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 50;
		c.gridy = 0;
		// TODO: idea for how to do separate TTS/visual plus icons
		// Instead of one checkbox per ability, just have one for TTS, and one for visual, and then
		// have a label with icon and text.
		// Alternatively, have a table with a bunch of checkbox columns
		jobTypeKeys.forEach((job) -> {
			List<Cooldown> cooldownsForJob = byJobType.get(job);
			c.gridwidth = 1;
			c.gridx = 0;
			c.weightx = 0;
			// left filler
			innerPanel.add(new JPanel());
			c.gridx ++;
			JLabel label = new JLabel(job.getFriendlyName());
			innerPanel.add(label, c);
			cooldownsForJob.forEach(dot -> {
				c.gridx++;

				BooleanSetting setting = cooldowns.get(dot);
				JCheckBox checkbox = new BooleanSettingGui(setting, dot.getLabel()).getComponent();
				innerPanel.add(checkbox, c);
			});
			c.gridx++;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			// Add dummy to pad out the right side
			JPanel dummyPanel = new JPanel();
			innerPanel.add(dummyPanel, c);
			c.gridy++;
		});
		jobKeys.forEach((job) -> {
			List<Cooldown> cooldownsForJob = byJob.get(job);
			c.gridwidth = 1;
			c.gridx = 0;
			c.weightx = 0;
			// left filler
			innerPanel.add(new JPanel());
			c.gridx ++;
			JLabel label = new JLabel(job.getFriendlyName());
			innerPanel.add(label, c);
			cooldownsForJob.forEach(dot -> {
				c.gridx++;

				BooleanSetting setting = cooldowns.get(dot);
				JCheckBox checkbox = new BooleanSettingGui(setting, dot.getLabel()).getComponent();
				innerPanel.add(checkbox, c);
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
