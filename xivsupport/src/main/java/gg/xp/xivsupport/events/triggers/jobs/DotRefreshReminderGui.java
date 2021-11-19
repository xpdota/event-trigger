package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.DotBuff;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.BooleanSetting;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class DotRefreshReminderGui implements PluginTab {

	private final DotRefreshReminders backend;

	public DotRefreshReminderGui(DotRefreshReminders backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Dot Tracker";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel("Dots");
		outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.PAGE_AXIS));

		JPanel preTimeBox = new LongSettingGui(backend.getDotRefreshAdvance(), "Time before expiry to call out (milliseconds)").getComponent();
		outerPanel.add(preTimeBox);

		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Map<DotBuff, BooleanSetting> dots = backend.getEnabledDots();
		Map<Job, List<DotBuff>> byJob = dots.keySet().stream().collect(Collectors.groupingBy(DotBuff::getJob));
		List<Job> jobKeys = byJob.keySet().stream().sorted(Comparator.comparing(Job::getFriendlyName)).collect(Collectors.toList());
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 50;
		c.gridy = 0;
		jobKeys.forEach((job) -> {
			List<DotBuff> dotsForJob = byJob.get(job);
			c.gridwidth = 1;
			c.gridx = 0;
			c.weightx = 0;
			// left filler
			innerPanel.add(new JPanel());
			c.gridx ++;
			JLabel label = new JLabel(job.getFriendlyName());
			innerPanel.add(label, c);
			dotsForJob.forEach(dot -> {
				c.gridx++;

				BooleanSetting setting = dots.get(dot);
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
