package gg.xp.xivsupport.gui.timelines;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.duties.timelines.CustomTimelineEntry;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineCustomizations;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineEntry;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineManager;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineOverlay;
import gg.xp.xivsupport.events.triggers.duties.timelines.TimelineProcessor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.WrapperPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class TimelinesTab extends TitleBorderFullsizePanel implements PluginTab {
	private final TimelineManager backend;
	private final CustomTableModel<Map.Entry<Long, String>> timelineChooserModel;
	private final CustomTableModel<TimelineEntry> timelineModel;
	private Long currentZone;
	private TimelineProcessor currentTimeline;
	private TimelineCustomizations currentCust;

	public TimelinesTab(TimelineManager backend, TimelineOverlay overlay, XivState state) {
		super("Timelines");
		this.backend = backend;
		this.setLayout(new GridBagLayout());
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

			{
				JCheckBox enableOverlay = new BooleanSettingGui(overlay.getEnabled(), "Enable Overlay").getComponent();
				settingsPanel.add(enableOverlay);
			}
			{
				// TODO: just add description to the settings themselves
				JCheckBox debugMode = new BooleanSettingGui(backend.getDebugMode(), "Debug Mode").getComponent();
				debugMode.setToolTipText("Debug mode will cause the last sync to always be displayed, and will cause sync-only entries to be displayed as well.");
				settingsPanel.add(debugMode);
			}
			{
				JCheckBox showPrePull = new BooleanSettingGui(backend.getPrePullSetting(), "Show Pre-Pull").getComponent();
				showPrePull.setToolTipText("Timeline will show prior to there being a valid sync.");
				settingsPanel.add(showPrePull);
			}
			{
				JCheckBox resetOnMapChange = new BooleanSettingGui(backend.getResetOnMapChangeSetting(), "Reset on Map Change").getComponent();
				resetOnMapChange.setToolTipText("Reset on map change - this is NOT a zone change! The timeline will always reset on zone changes.\n\nResetting on a map change is sometimes desirable (e.g. raids with doorbosses, dungeons), but breaks others if they use multiple maps (e.g. O3N) and their post-map-change syncs don't have a big enough window.");
				settingsPanel.add(resetOnMapChange);
			}
			{
				JPanel numSetting = new IntSettingSpinner(backend.getRowsToDisplay(), "Max in Overlay").getComponent();
				settingsPanel.add(numSetting);
			}
			{
				JPanel futureSetting = new IntSettingSpinner(backend.getSecondsFuture(), "Seconds in Future").getComponent();
				settingsPanel.add(futureSetting);
			}
			{
				JPanel pastSetting = new IntSettingSpinner(backend.getSecondsPast(), "Seconds in Past").getComponent();
				settingsPanel.add(pastSetting);
			}

			this.add(settingsPanel, c);
		}
		c.gridy++;

		{
			ReadOnlyText text = new ReadOnlyText("This feature is beta and very buggy. For now, you can only add your own custom entries, but not edit anything coming from the original timeline files.");
			this.add(text, c);
		}


		c.gridy++;
		c.weightx = 0.2;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;

		// TODO: searching
		timelineChooserModel = CustomTableModel.builder(() -> TimelineManager.getTimelines().entrySet()
						.stream().sorted(Map.Entry.comparingByKey()).toList())
				.addColumn(new CustomColumn<>("Zone", Map.Entry::getKey, col -> {
					col.setMinWidth(50);
					col.setMaxWidth(50);
				}))
				.addColumn(new CustomColumn<>("File", Map.Entry::getValue, col -> {
					col.setMinWidth(50);
					col.setMaxWidth(300);
					col.setPreferredWidth(100);
				}))
				.build();

		JTable timelineChooserTable = new JTable(timelineChooserModel);
		timelineChooserModel.configureColumns(timelineChooserTable);
		timelineChooserTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


		JScrollPane chooserScroller = new JScrollPane(timelineChooserTable);
		chooserScroller.setPreferredSize(new Dimension(200, 32768));
		chooserScroller.setMinimumSize(new Dimension(100, 200));
		this.add(chooserScroller, c);

		int numColMinWidth = 50;
		int numColMaxWidth = 200;
		int numColPrefWidth = 50;

		timelineModel = CustomTableModel.builder(() -> {
					TimelineProcessor timeline = currentTimeline;
					if (timeline == null) {
						return Collections.emptyList();
					}
					return timeline.getEntries();
				})
				.addColumn(new CustomColumn<>("Custom", e -> e instanceof CustomTimelineEntry ? "✔" : "", col -> {
					col.setMinWidth(50);
					col.setMaxWidth(50);
				}))
				.addColumn(new CustomColumn<>("Time", TimelineEntry::time, col -> {
					col.setCellEditor(StandardColumns.doubleEditorNonNull((item, value) -> ((CustomTimelineEntry) item).time = value));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Icon", TimelineEntry::icon, col -> {
					col.setCellEditor(StandardColumns.urlEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).icon = value));
					col.setCellRenderer(new ActionAndStatusRenderer());
					col.setMinWidth(32);
					col.setMaxWidth(32);
					col.setPreferredWidth(32);
				}))
				.addColumn(new CustomColumn<>("Name", TimelineEntry::name, col -> {
					col.setCellEditor(StandardColumns.stringEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).name = value));
				}))
				.addColumn(new CustomColumn<>("Pattern", TimelineEntry::sync, col -> {
					col.setCellEditor(StandardColumns.regexEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).sync = value));
				}))
				.addColumn(new CustomColumn<>("Duration", TimelineEntry::duration, col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).duration = value));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Win Start", e -> e.timelineWindow().start(), col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).windowStart = value));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Win End", e -> e.timelineWindow().end(), col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).windowEnd = value));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Win Effective", e -> String.format("%.01f - %.01f", e.getMinTime(), e.getMaxTime()), col -> {
					col.setMinWidth(numColMinWidth * 2);
					col.setMaxWidth(numColMaxWidth * 2);
					col.setPreferredWidth(numColPrefWidth * 2);
				}))
				.addColumn(new CustomColumn<>("Jump", TimelineEntry::jump, col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull((item, value) -> ((CustomTimelineEntry) item).jump = value));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.setItemEquivalence((one, two) -> one == two)
				.build();

		CustomRightClickOption clone = CustomRightClickOption.forRow("Clone", TimelineEntry.class, e -> addNewEntry(new CustomTimelineEntry(
				e.time(),
				e.name(),
				e.sync(),
				e.duration(),
				e.timelineWindow(),
				e.jump(),
				e.icon()
		)));
		CustomRightClickOption delete = CustomRightClickOption.forRow("Delete", CustomTimelineEntry.class, this::deleteEntry);

		JTable timelineTable = new JTable(timelineModel) {
			@Override
			public boolean isCellEditable(int row, int column) {
				TimelineEntry valueForRow = timelineModel.getValueForRow(row);
				if (valueForRow instanceof CustomTimelineEntry) {
					return column != 6;
				}
				return super.isCellEditable(row, column);
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				TimelineEntry selected = timelineModel.getSelectedValue();
				super.editingStopped(e);
				Long currentZone = TimelinesTab.this.currentZone;
				List<CustomTimelineEntry> newCurrentEntries = currentTimeline.getEntries().stream()
						.filter(CustomTimelineEntry.class::isInstance)
						.map(CustomTimelineEntry.class::cast)
						.collect(Collectors.toList());
				currentCust.setEntries(newCurrentEntries);
				backend.commitCustomSettings(currentZone);
				TimelinesTab.this.updateTab();
				if (selected != null) {
					SwingUtilities.invokeLater(() -> timelineModel.setSelectedValue(selected));
					SwingUtilities.invokeLater(() -> scrollRectToVisible(getCellRect(getSelectedRow(), 0, true)));
				}
			}
		};

		timelineModel.configureColumns(timelineTable);
		timelineTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		timelineChooserTable.getSelectionModel().addListSelectionListener(l -> {
			updateTab();
		});

		CustomRightClickOption.configureTable(timelineTable, timelineModel, List.of(clone, delete));

		c.gridx++;
		c.weightx = 1;
		c.gridheight = 1;
		JScrollPane scroll = new JScrollPane(timelineTable);
		scroll.setMinimumSize(new Dimension(1, 1));
		this.add(scroll, c);

		c.gridy++;

		c.gridx = 0;
		c.weighty = 0;
		c.weightx = 0;
		JButton selectCurrentButton = new JButton("Select Current");
		selectCurrentButton.addActionListener(l -> {
			XivZone zone = state.getZone();
			if (zone != null) {
				long zoneId = zone.getId();
				timelineChooserModel.getData().stream().filter(e -> e.getKey() == zoneId).findFirst().ifPresent(value -> {
					timelineChooserModel.setSelectedValue(value);
					timelineChooserModel.scrollToSelectedValue();
				});
			}
		});
		this.add(new WrapperPanel(selectCurrentButton), c);
		c.weighty = 0;
		c.gridx++;

		JButton newButton = new JButton("Add New Timeline Entry") {
			@Override
			public boolean isEnabled() {
				return currentCust != null;
			}
		};
		newButton.addActionListener(l -> {
			@Nullable TimelineEntry selectedValue = timelineModel.getSelectedValue();
			CustomTimelineEntry newEntry = new CustomTimelineEntry();
			if (selectedValue != null) {
				newEntry.time = selectedValue.time();
			}
			addNewEntry(newEntry);
		});
		this.add(new WrapperPanel(newButton), c);
	}

	private void addNewEntry(CustomTimelineEntry newEntry) {
		Long zone = this.currentZone;
		TimelineCustomizations stuff = backend.getCustomSettings(zone);
		List<CustomTimelineEntry> currentEntries = stuff.getEntries();
		List<CustomTimelineEntry> newCurrentEntries = new ArrayList<>(currentEntries);
		newCurrentEntries.add(newEntry);
		stuff.setEntries(newCurrentEntries);
		backend.commitCustomSettings(currentZone);
		updateTab();
		SwingUtilities.invokeLater(() -> {
			timelineModel.setSelectedValue(newEntry);
		});
	}

	private void deleteEntry(CustomTimelineEntry toDelete) {
		Long zone = this.currentZone;
		TimelineCustomizations stuff = backend.getCustomSettings(zone);
		List<CustomTimelineEntry> currentEntries = stuff.getEntries();
		List<CustomTimelineEntry> newCurrentEntries = new ArrayList<>(currentEntries);
		newCurrentEntries.remove(toDelete);
		stuff.setEntries(newCurrentEntries);
		backend.commitCustomSettings(currentZone);
		updateTab();
	}

	@Override
	public String getTabName() {
		return "Timelines";
	}

	@Override
	public Component getTabContents() {
		return this;
	}

	private void updateTab() {
		setCurrentZoneToSelected();
		timelineModel.fullRefresh();
		repaint();
	}

	private void setCurrentZoneToSelected() {
		Long currentZone = getCurrentSelectedZone();
		this.currentZone = currentZone;
		if (currentZone == null) {
			currentTimeline = null;
			currentCust = null;
		}
		else {
			currentTimeline = backend.getTimeline(currentZone);
			currentCust = backend.getCustomSettings(currentZone);
		}
	}

	private @Nullable Long getCurrentSelectedZone() {
		Map.Entry<Long, String> selected = timelineChooserModel.getSelectedValue();
		if (selected == null) {
			return null;
		}
		return selected.getKey();
	}

	@Override
	public int getSortOrder() {
		return 9;
	}
}
