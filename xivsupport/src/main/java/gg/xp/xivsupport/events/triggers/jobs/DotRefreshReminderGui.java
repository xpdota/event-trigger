package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.data.Job;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
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
//		outerPanel.setLayout(new BorderLayout());
		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Map<DotRefreshReminders.DotBuff, Boolean> dots = backend.getEnabledDots();
		Map<Job, List<DotRefreshReminders.DotBuff>> byJob = dots.keySet().stream().collect(Collectors.groupingBy(DotRefreshReminders.DotBuff::getJob));
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 50;
		c.gridy = 0;
		byJob.forEach((job, dotsForJob) -> {
			c.gridwidth = 1;
			c.gridx = 0;
			c.weightx = 0;
			JLabel label = new JLabel(job.getFriendlyName());
			innerPanel.add(label, c);
			dotsForJob.forEach(dot -> {
				c.gridx ++;
				JCheckBox checkbox = new JCheckBox(dot.getLabel());
				checkbox.setSelected(dots.get(dot));
				checkbox.addItemListener(l -> backend.setDotEnabled(dot, checkbox.isSelected()));
				innerPanel.add(checkbox, c);
			});
			c.gridx ++;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			// Add dummy to pad out the right side
			JPanel dummyPanel = new JPanel();
			innerPanel.add(dummyPanel, c);
			c.gridy ++;
		});
		c.weighty = 1;
		innerPanel.add(new JPanel(), c);
		innerPanel.setPreferredSize(innerPanel.getMinimumSize());
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(new JLabel("Foo bar stuf"));
		outerPanel.add(scroll);
		return outerPanel;
	}
}
