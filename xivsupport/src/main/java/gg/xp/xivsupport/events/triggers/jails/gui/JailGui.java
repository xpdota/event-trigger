package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.triggers.jails.JailSolver;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ScanMe
public class JailGui implements PluginTab {
	private static final Logger log = LoggerFactory.getLogger(JailGui.class);

	private final JailSolver jails;

	public JailGui(JailSolver jails) {

		this.jails = jails;
	}

	@Override
	public String getTabName() {
		return "Titan Gaols";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Jails");
		panel.add(new BooleanSettingGui(jails.getEnableTts(), "Enable Personal Callout").getComponent());
		panel.add(new BooleanSettingGui(jails.getEnableAutomark(), "Enable Automarks").getComponent());
		List<Job> items = Arrays.stream(Job.values()).filter(Job::isCombatJob).collect(Collectors.toList());
		JList<Job> jobList = new RearrangeableList<>(items, l -> log.info("Changed jail prio: {}", l.stream().map(Enum::name).collect(Collectors.joining(", "))));
		panel.add(jobList);
		return panel;
	}


}
