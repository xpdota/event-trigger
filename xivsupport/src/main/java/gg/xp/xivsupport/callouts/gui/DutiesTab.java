package gg.xp.xivsupport.callouts.gui;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.Duty;
import gg.xp.xivdata.data.duties.DutyType;
import gg.xp.xivdata.data.duties.Expansion;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutGroup;
import gg.xp.xivsupport.callouts.ModifiedCalloutRepository;
import gg.xp.xivsupport.callouts.audio.SoundFilesManager;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tabs.FixedWidthVerticalTabPane;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ScanMe
public class DutiesTab implements PluginTab {

	private final ModifiedCalloutRepository backend;
	private final PicoContainer container;
	private final SoundFilesManager soundMgr;
	private final EventMaster master;

	public DutiesTab(ModifiedCalloutRepository backend, PicoContainer container, SoundFilesManager soundMgr, EventMaster master) {
		this.backend = backend;
		this.container = container;
		this.soundMgr = soundMgr;
		this.master = master;
	}

	@Override
	public String getTabName() {
		return "Duties";
	}


	private static final class DutyTabContents {
		final Duty duty;
		final List<CalloutGroup> calls = new ArrayList<>();
		final List<DutyPluginTab> extraTabs = new ArrayList<>();

		private DutyTabContents(Duty duty) {
			this.duty = duty;
		}
	}

	@Override
	public Component getTabContents() {
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
		Map<Expansion, Map<DutyType, Map<Duty, DutyTabContents>>> contents = new LinkedHashMap<>();
		DutyTabContents nonSpecific = new DutyTabContents(KnownDuty.None);
		List<CalloutGroup> allCallouts = new ArrayList<>(backend.getAllCallouts());
		allCallouts.sort(Comparator.comparing(CalloutGroup::getDuty));
		allCallouts.forEach(group -> {
			KnownDuty duty = group.getDuty();
			if (duty == KnownDuty.None) {
				nonSpecific.calls.add(group);
				return;
			}
			DutyTabContents tabContents = contents.computeIfAbsent(duty.getExpac(), d -> new LinkedHashMap<>())
					.computeIfAbsent(duty.getType(), d -> new LinkedHashMap<>())
					.computeIfAbsent(duty, DutyTabContents::new);
			tabContents.calls.add(group);
		});

		container.getComponents(DutyPluginTab.class).forEach(tab -> {
			KnownDuty duty = tab.getDuty();
			DutyTabContents tabContents = contents.computeIfAbsent(duty.getExpac(), d -> new LinkedHashMap<>())
					.computeIfAbsent(duty.getType(), d -> new LinkedHashMap<>())
					.computeIfAbsent(duty, DutyTabContents::new);
			tabContents.extraTabs.add(tab);

		});


		nonSpecific.extraTabs.add(new DutyPluginTab() {
			@Override
			public String getTabName() {
				return "Test Callouts";
			}

			@Override
			public Component getTabContents() {
				TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Test Callouts");
				outer.setLayout(new BorderLayout());
				JButton testButton = new JButton("Test");
				testButton.addActionListener(l -> {
					// TODO: not a good way of doing this
					master.pushEvent(new DebugCommand("testcall"));
				});
				JPanel panel = new JPanel();
				panel.add(testButton);
				outer.add(panel, BorderLayout.NORTH);
				return outer;
			}

			@Override
			public KnownDuty getDuty() {
				return null;
			}
		});

		tabPane.add("General", makeDutyComponent(nonSpecific));

		// TODO: as this gets bigger, might be worth looking into some kind of lazy
		// loading of tabs.
		contents.forEach((expac, types) -> {
			JTabbedPane expacTab = new FixedWidthVerticalTabPane(80);
			types.forEach((type, duties) -> {
				JTabbedPane typeTab = new FixedWidthVerticalTabPane(100);
				duties.forEach((duty, dutyContent) -> {
					typeTab.add(duty.getName(), makeDutyComponent(dutyContent));
				});
				expacTab.add(type.getName(), typeTab);
			});
			tabPane.add(expac.getName(), expacTab);
		});

		return tabPane;
	}

	private Component makeDutyComponent(DutyTabContents dutyContent) {
		JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);

		{
			JPanel panel = new JPanel(new BorderLayout());
			CalloutHelper ch = new CalloutHelper(dutyContent.calls, soundMgr);
			JScrollPane scroller = new JScrollPane(ch);
			scroller.getVerticalScrollBar().setUnitIncrement(20);
			scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			panel.add(scroller, BorderLayout.CENTER);

			JPanel settingsPanel = new JPanel();
			settingsPanel.setLayout(new WrapLayout());

			JCheckBox enableTts = new BooleanSettingGui(backend.getEnableTts(), "Enable TTS (Global)").getComponent();
			settingsPanel.add(enableTts);
			JCheckBox enableOverlay = new BooleanSettingGui(backend.getEnableOverlay(), "Enable Overlay (Global)").getComponent();
			settingsPanel.add(enableOverlay);

			JButton expandAll = new JButton("Expand All");
			expandAll.addActionListener(l -> ch.setAllShowHide(true));
			settingsPanel.add(expandAll);

			JButton collapseAll = new JButton("Collapse All");
			collapseAll.addActionListener(l -> ch.setAllShowHide(false));
			settingsPanel.add(collapseAll);

			JButton enableAll = new JButton("Enable All");
			enableAll.addActionListener(l -> ch.setAllEnableDisable(true));
			settingsPanel.add(enableAll);

			JButton disableAll = new JButton("Disable All");
			disableAll.addActionListener(l -> ch.setAllEnableDisable(false));
			settingsPanel.add(disableAll);


			panel.add(settingsPanel, BorderLayout.NORTH);

			tabPane.add("Callouts", panel);
		}

		{
			for (DutyPluginTab extraTab : dutyContent.extraTabs) {
				tabPane.add(extraTab.getTabName(), extraTab.getTabContents());
			}
		}

		return tabPane;
	}

	@Override
	public int getSortOrder() {
		return 0;
	}
}
