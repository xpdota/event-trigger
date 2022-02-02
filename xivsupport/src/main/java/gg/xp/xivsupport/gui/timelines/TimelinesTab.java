package gg.xp.xivsupport.gui.timelines;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.duties.timelines.RawTimelineEntry;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineManager;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineOverlay;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineProcessor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Map;

@ScanMe
public class TimelinesTab implements PluginTab {
	private final TimelineManager backend;
	private final TimelineOverlay overlay;

	public TimelinesTab(TimelineManager backend, TimelineOverlay overlay) {
		this.backend = backend;
		this.overlay = overlay;
	}

	@Override
	public String getTabName() {
		return "Timelines";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Timelines");
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		{
			JPanel settingsPanel = new JPanel();
			settingsPanel.setLayout(new WrapLayout(FlowLayout.CENTER, 5, 0));

			JCheckBox enableOverlay = new BooleanSettingGui(overlay.getEnabled(), "Enable Overlay").getComponent();
			settingsPanel.add(enableOverlay);
			JCheckBox debugMode = new BooleanSettingGui(backend.getDebugMode(), "Debug Mode").getComponent();
			settingsPanel.add(debugMode);
			JPanel numSetting = new IntSettingSpinner(backend.getRowsToDisplay(), "Max in Overlay").getComponent();
			settingsPanel.add(numSetting);

			panel.add(settingsPanel, c);
		}
		c.gridy++;

		{
			ReadOnlyText text = new ReadOnlyText("This feature is beta and very buggy. For dungeons/24 man raids, or anything else" +
					"with trash, you may need to use /e c:splitpull to force it to reset after each encounter.");
			panel.add(text, c);
		}


		c.gridy++;
		c.weightx = 0;
		c.weighty = 1;
		c.gridwidth = 1;

		// TODO: searching
		CustomTableModel<Map.Entry<Long, String>> timelineChooserModel = CustomTableModel.builder(() -> TimelineManager.getTimelines().entrySet()
						.stream().sorted(Map.Entry.comparingByKey()).toList())
				.addColumn(new CustomColumn<>("Zone", e -> e.getKey(), col -> col.setPreferredWidth(100)))
				.addColumn(new CustomColumn<>("File", e -> e.getValue()))
				.build();

		JTable timelineChooserTable = new JTable(timelineChooserModel);
		timelineChooserModel.configureColumns(timelineChooserTable);
		timelineChooserTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


		JScrollPane chooserScroller = new JScrollPane(timelineChooserTable);
		chooserScroller.setMinimumSize(new Dimension(200, 200));
		panel.add(chooserScroller, c);

		CustomTableModel<RawTimelineEntry> timelineModel = CustomTableModel.builder(() -> {
					Map.Entry<Long, String> selected = timelineChooserModel.getSelectedValue();
					if (selected == null) {
						return Collections.emptyList();
					}
					TimelineProcessor timeline = TimelineManager.getTimeline(selected.getKey());
					if (timeline == null) {
						return Collections.emptyList();
					}
					return timeline.getEntries();
				})
				.addColumn(new CustomColumn<>("Time", e -> e.time()))
				.addColumn(new CustomColumn<>("Name", e -> e.name()))
				.addColumn(new CustomColumn<>("Pattern", e -> e.sync()))
				.addColumn(new CustomColumn<>("Duration", e -> e.duration()))
				.addColumn(new CustomColumn<>("Window Start", e -> e.timelineWindow().start()))
				.addColumn(new CustomColumn<>("Window End", e -> e.timelineWindow().end()))
				.addColumn(new CustomColumn<>("Window Effective", e -> String.format("%s - %s", e.getMinTime(), e.getMaxTime())))
				.addColumn(new CustomColumn<>("Jump", e -> e.jump()))
				.build();

		JTable timelineTable = new JTable(timelineModel);
		timelineModel.configureColumns(timelineTable);
		timelineTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		timelineChooserTable.getSelectionModel().addListSelectionListener(l -> timelineModel.fullRefresh());

		c.gridx++;
		c.weightx = 1;
		JScrollPane scroll = new JScrollPane(timelineTable);
		scroll.setMinimumSize(new Dimension(1, 1));
		panel.add(scroll, c);


		return panel;
	}

	@Override
	public int getSortOrder() {
		return 9;
	}
}
