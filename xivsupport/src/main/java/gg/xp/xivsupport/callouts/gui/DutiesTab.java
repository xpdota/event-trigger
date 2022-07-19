package gg.xp.xivsupport.callouts.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.DutyType;
import gg.xp.xivdata.data.duties.Expansion;
import gg.xp.xivsupport.callouts.CalloutGroup;
import gg.xp.xivsupport.callouts.DutyTab;
import gg.xp.xivsupport.callouts.ModifiedCalloutRepository;
import gg.xp.xivsupport.gui.extra.PluginTab;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ScanMe
public class DutiesTab implements PluginTab {

	private final ModifiedCalloutRepository backend;

	public DutiesTab(ModifiedCalloutRepository backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Duties";
	}


	private class DutyTabContents {
		final List<CalloutGroup> calls = new ArrayList<>();
		final Map<String, Component> extraTabs = new LinkedHashMap<>();
	}

	@Override
	public Component getTabContents() {
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
//		Map<Expansion, Map<DutyType, List<List<>>>>
//		List<CalloutGroup> allCallouts = backend.getAllCallouts();
//		allCallouts.forEach(group -> {
//			Class<?> callClass = group.getCallClass();
//			DutyTab ann = callClass.getAnnotation(DutyTab.class);
//			ann.value().
//		});
//		CalloutGroup calloutGroup = allCallouts.get(0);
//		calloutGroup
		return tabPane;
	}

	@Override
	public int getSortOrder() {
		return 0;
	}
}
