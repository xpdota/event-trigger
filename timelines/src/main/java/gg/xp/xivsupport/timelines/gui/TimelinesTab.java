package gg.xp.xivsupport.timelines.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.WrapperPanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.RenderUtils;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.sys.Threading;
import gg.xp.xivsupport.timelines.CustomTimelineEntry;
import gg.xp.xivsupport.timelines.TimelineCustomizations;
import gg.xp.xivsupport.timelines.TimelineEntry;
import gg.xp.xivsupport.timelines.TimelineInfo;
import gg.xp.xivsupport.timelines.TimelineManager;
import gg.xp.xivsupport.timelines.TimelineOverlay;
import gg.xp.xivsupport.timelines.TimelineProcessor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ScanMe
public class TimelinesTab extends TitleBorderFullsizePanel implements PluginTab {
	private static final Logger log = LoggerFactory.getLogger(TimelinesTab.class);
	private final TimelineManager backend;
	private final ActionTableFactory actionTableFactory;
	private final CustomTableModel<TimelineInfo> timelineChooserModel;
	private final CustomTableModel<TimelineEntry> timelineModel;
	private volatile List<TimelineEntry> timelineEntries;
	private final JTable timelineTable;
	private final ExecutorService exs = Executors.newSingleThreadExecutor(Threading.namedDaemonThreadFactory("TimelinesTab"));
	private Long currentZone;
	private TimelineProcessor currentTimeline;
	private TimelineCustomizations currentCust;

	public TimelinesTab(TimelineManager backend, TimelineOverlay overlay, XivState state, ActionTableFactory actionTableFactory, TimelineBarColorProviderImpl tbcp) {
		super("Timelines");
		// TODO: searching
		this.backend = backend;
		this.actionTableFactory = actionTableFactory;

		int numColMinWidth = 50;
		int numColMaxWidth = 200;
		int numColPrefWidth = 50;

		int timeColMinWidth = 80;
		int timeColMaxWidth = 200;
		int timeColPrefWidth = 80;
		timelineChooserModel = CustomTableModel.builder(() -> TimelineManager.getTimelines().values()
						.stream().sorted(Comparator.comparing(TimelineInfo::zoneId)).toList())
				.addColumn(new CustomColumn<>("Zone", TimelineInfo::zoneId, col -> {
					col.setMinWidth(50);
					col.setMaxWidth(50);
				}))
				.addColumn(new CustomColumn<>("File", TimelineInfo::filename, col -> {
					col.setMinWidth(50);
					col.setMaxWidth(300);
					col.setPreferredWidth(100);
				}))
				.build();

		timelineModel = CustomTableModel.builder(() -> {
					TimelineProcessor timeline = currentTimeline;
					if (timeline == null) {
						return Collections.emptyList();
					}
					return timeline.getRawEntries();
				})
				.addColumn(new CustomColumn<>("En", TimelineEntry::enabled, col -> {
					col.setCellRenderer(StandardColumns.checkboxRenderer);
					col.setCellEditor(new StandardColumns.CustomCheckboxEditor<>(safeEditTimelineEntry(true, (entry, value) -> {
						entry.enabled = value;
					})));
					col.setMinWidth(22);
					col.setMaxWidth(22);
				}))
				.addColumn(new CustomColumn<>("Type", Function.identity(), col -> {
					col.setCellEditor(NoCellEditor.INSTANCE);
					col.setMinWidth(40);
					col.setMaxWidth(40);
					col.setCellRenderer(new TimelineEntryTypeRenderer());
				}))
				.addColumn(new CustomColumn<>("Time", TimelineEntry::time, col -> {
					col.setCellRenderer(new DoubleTimeRenderer());
					col.setCellEditor(StandardColumns.doubleEditorNonNull(safeEditTimelineEntry(false, (item, value) -> item.time = value)));
					col.setMinWidth(timeColMinWidth);
					col.setMaxWidth(timeColMaxWidth);
					col.setPreferredWidth(timeColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Icon", TimelineEntry::icon, col -> {
					col.setCellEditor(StandardColumns.urlEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.icon = value)));
					col.setCellRenderer(new ActionAndStatusRenderer());
					col.setMinWidth(32);
					col.setMaxWidth(32);
					col.setPreferredWidth(32);
				}))
				.addColumn(new CustomColumn<>("Text/Name", TimelineEntry::name, col -> {
					col.setCellEditor(StandardColumns.stringEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.name = value)));
				}))
				.addColumn(new CustomColumn<>("Pattern", TimelineEntry::sync, col -> {
					col.setCellEditor(StandardColumns.regexEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.sync = value), Pattern.CASE_INSENSITIVE));
				}))
				.addColumn(new CustomColumn<>("Duration", TimelineEntry::duration, col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.duration = value)));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Win Start", e -> e.timelineWindow().start(), col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.windowStart = value)));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Win End", e -> e.timelineWindow().end(), col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.windowEnd = value)));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Win Effective", e -> String.format("%.01f - %.01f", e.getMinTime(), e.getMaxTime()), col -> {
					col.setCellEditor(NoCellEditor.INSTANCE);
					col.setMinWidth(numColMinWidth * 2);
					col.setMaxWidth(numColMaxWidth * 2);
					col.setPreferredWidth(numColPrefWidth * 2);
				}))
				.addColumn(new CustomColumn<>("Jump", TimelineEntry::jump, col -> {
					col.setCellRenderer(new DoubleTimeRenderer());
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> item.jump = value)));
					col.setMinWidth(timeColMinWidth);
					col.setMaxWidth(timeColMaxWidth);
					col.setPreferredWidth(timeColPrefWidth);
				}))
				.addColumn(new CustomColumn<>("Trig", TimelineEntry::callout, col -> {
					col.setCellRenderer(StandardColumns.checkboxRenderer);
					col.setCellEditor(new StandardColumns.CustomCheckboxEditor<>(safeEditTimelineEntry(true, (entry, value) -> {
						entry.callout = value;
						stopEditing();
						commitSettings();
					})));
					col.setMinWidth(30);
					col.setMaxWidth(30);
				}))
				.addColumn(new CustomColumn<>("Pre", timelineEntry -> {
					double pre = timelineEntry.calloutPreTime();
					boolean isTrigger = timelineEntry.callout();
					// Hide column if not a trigger
					if (!isTrigger && pre == 0) {
						return "";
					}
					return pre;
				}, col -> {
					col.setCellEditor(StandardColumns.doubleEditorEmptyToNull(safeEditTimelineEntry(false, (item, value) -> {
						if (value == null) {
							item.calloutPreTime = 0.0;
						}
						else {
							// Automatically set as trigger if we set a pre time
							item.calloutPreTime = value;
							item.callout = true;
						}
					})));
					col.setMinWidth(numColMinWidth);
					col.setMaxWidth(numColMaxWidth);
					col.setPreferredWidth(numColPrefWidth);
				}))
				.setItemEquivalence((one, two) -> one == two)
				.build();

		timelineTable = new JTable(timelineModel) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return !(getCellEditor(row, column) instanceof NoCellEditor);
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				super.editingStopped(e);
				commitEdit();
			}

			@Override
			public void editingCanceled(ChangeEvent e) {
				super.editingCanceled(e);
				cancelEdit();
			}
		};


		SwingUtilities.invokeLater(() -> {

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
				JPanel colorsPanel = new JPanel();
				colorsPanel.setLayout(new WrapLayout(FlowLayout.CENTER, 5, 0));

				colorsPanel.add(new ColorSettingGui(tbcp.getActiveSetting(), "Active Color", () -> true).getComponent());
				colorsPanel.add(new ColorSettingGui(tbcp.getUpcomingSetting(), "Upcoming Color", () -> true).getComponent());
				colorsPanel.add(new ColorSettingGui(tbcp.getExpiredSetting(), "Expired Color", () -> true).getComponent());
				colorsPanel.add(new ColorSettingGui(tbcp.getFontSetting(), "Font Color", () -> true).getComponent());

				this.add(colorsPanel, c);
			}
//		c.gridy++;
//
//		{
//			ReadOnlyText text = new ReadOnlyText("This feature is beta and very buggy. For now, you can only add your own custom entries, but not edit anything coming from the original timeline files.");
//			this.add(text, c);
//		}


			c.gridy++;
			c.weightx = 0.2;
			c.weighty = 1;
			c.gridwidth = 1;
			c.gridheight = 1;


			JTable timelineChooserTable = new JTable(timelineChooserModel);
			timelineChooserModel.configureColumns(timelineChooserTable);
			timelineChooserTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


			JScrollPane chooserScroller = new JScrollPane(timelineChooserTable);
			chooserScroller.setPreferredSize(new Dimension(200, 32768));
			chooserScroller.setMinimumSize(new Dimension(100, 200));
			this.add(chooserScroller, c);


			CustomRightClickOption clone = CustomRightClickOption.forRow("Clone", TimelineEntry.class, e -> addNewEntry(new CustomTimelineEntry(
					e.time(),
					e.name() + " copy",
					e.sync(),
					e.duration(),
					e.timelineWindow(),
					e.jump(),
					e.icon(),
					null,
					false,
					e.callout(),
					e.calloutPreTime()
			)));
			CustomRightClickOption delete = CustomRightClickOption.forRow("Delete/Revert", CustomTimelineEntry.class, this::deleteEntry);

			CustomRightClickOption chooseAbilityIcon = CustomRightClickOption.forRow("Use Ability Icon", TimelineEntry.class, this::chooseActionIcon);
			CustomRightClickOption chooseStatusIcon = CustomRightClickOption.forRow("Use Status Icon", TimelineEntry.class, this::chooseStatusInfo);

			timelineModel.configureColumns(timelineTable);
			timelineTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			timelineChooserTable.getSelectionModel().addListSelectionListener(l -> {
				timelineModel.setSelectedValue(null);
				stopEditing();
				updateTab();
			});

			RightClickOptionRepo.of(clone, delete, chooseAbilityIcon, chooseStatusIcon).configureTable(timelineTable, timelineModel);

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
					timelineChooserModel.getData().stream().filter(e -> e.zoneId() == zoneId).findFirst().ifPresent(value -> {
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
			JButton resetButton = new JButton("Delete All Customizations") {
				@Override
				public boolean isEnabled() {
					return currentCust != null && !currentCust.getEntries().isEmpty();
				}
			};
			// TODO: confirmation
			resetButton.addActionListener(l -> {
				int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete all customizations for the currently selected timeline?", "Confirm", JOptionPane.YES_NO_OPTION);
				if (confirmation == 0) {
					resetAll();
				}
			});
			JPanel buttonPanel = new JPanel(new WrapLayout());
			buttonPanel.add(newButton);
			buttonPanel.add(resetButton);

			this.add(buttonPanel, c);
		});
	}

	private void commitSettings() {
		TimelineEntry selected = timelineModel.getSelectedValue();
		Long currentZone = TimelinesTab.this.currentZone;
		List<CustomTimelineEntry> newCurrentEntries = currentTimeline.getRawEntries().stream()
				.filter(CustomTimelineEntry.class::isInstance)
				.map(CustomTimelineEntry.class::cast)
				.collect(Collectors.toList());
		currentCust.setEntries(newCurrentEntries);
		backend.commitCustomSettings(currentZone);
		TimelinesTab.this.updateTab();
		if (selected != null) {
			SwingUtilities.invokeLater(() -> timelineModel.setSelectedValue(selected));
			SwingUtilities.invokeLater(() -> scrollRectToVisible(timelineTable.getCellRect(timelineTable.getSelectedRow(), 0, true)));
		}

	}

	private void stopEditing() {
		TableCellEditor editor = timelineTable.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}

	private void chooseActionIcon(TimelineEntry te) {
		stopEditing();
		actionTableFactory.showChooser(SwingUtilities.getWindowAncestor(this), action -> this.<ActionInfo>safeEditTimelineEntry(false, (ce, actionInfo) -> {
					ActionIcon icon = actionInfo.getIcon();
					ce.icon = icon == null ? null : icon.getIconUrl();
				}).accept(te, action)
		);
		commitEdit();
		updateTab();
	}

	private void chooseStatusInfo(TimelineEntry te) {
		stopEditing();
		StatusTable.showChooser(SwingUtilities.getWindowAncestor(this), status -> this.<StatusEffectInfo>safeEditTimelineEntry(false, (ce, statusInfo) -> {
					StatusEffectIcon icon = statusInfo.getIcon(0);
					ce.icon = icon == null ? null : icon.getIconUrl();
				}).accept(te, status)
		);
		commitEdit();
		updateTab();
	}

	private static final Runnable NOTHING = () -> {
	};
	private volatile Runnable pendingEdit = NOTHING;

	/**
	 * Wraps the given editing function to make it create an override first if needed
	 *
	 * @param editFunc BiConsumer, first argument is the CustomTimelineEntry, second is the value from the editor
	 * @param <X>      The type for the cell
	 * @return The lambda
	 */
	private <X> BiConsumer<Object, X> safeEditTimelineEntry(boolean stopEditing, BiConsumer<CustomTimelineEntry, X> editFunc) {
		return (item, value) -> {
			pendingEdit = () -> {
				if (item instanceof CustomTimelineEntry custom) {
					editFunc.accept(custom, value);
				}
				else {
					CustomTimelineEntry custom = CustomTimelineEntry.overrideFor((TimelineEntry) item);
					editFunc.accept(custom, value);
					addNewEntry(custom);
				}
				commitSettings();
			};
			if (stopEditing) {
				stopEditing();
			}
		};
	}

	private void commitEdit() {
		log.info("Committing edit");
		pendingEdit.run();
		pendingEdit = NOTHING;
		updateTab();
	}

	private void cancelEdit() {
		log.info("Cancelling edit");
		pendingEdit = NOTHING;
		updateTab();
	}


	private void addNewEntry(CustomTimelineEntry newEntry) {
		stopEditing();
		Long zone = this.currentZone;
		TimelineCustomizations stuff = backend.getCustomSettings(zone);
		List<CustomTimelineEntry> currentEntries = stuff.getEntries();
		List<CustomTimelineEntry> newCurrentEntries = new ArrayList<>(currentEntries);
		newCurrentEntries.add(newEntry);
		stuff.setEntries(newCurrentEntries);
		backend.commitCustomSettings(currentZone);
		updateTab();
		SwingUtilities.invokeLater(() -> timelineModel.setSelectedValue(newEntry));
	}

	private void deleteEntry(CustomTimelineEntry toDelete) {
		stopEditing();
		Long zone = this.currentZone;
		TimelineCustomizations stuff = backend.getCustomSettings(zone);
		List<CustomTimelineEntry> currentEntries = stuff.getEntries();
		List<CustomTimelineEntry> newCurrentEntries = new ArrayList<>(currentEntries);
		newCurrentEntries.remove(toDelete);
		stuff.setEntries(newCurrentEntries);
		backend.commitCustomSettings(currentZone);
		updateTab();
	}

	private void resetAll() {
		stopEditing();
		Long zone = this.currentZone;
		TimelineCustomizations stuff = backend.getCustomSettings(zone);
		stuff.setEntries(Collections.emptyList());
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
		timelineModel.fullRefreshSync();
		repaint();
	}

	private void setCurrentZoneToSelected() {
		Long currentZone = getCurrentSelectedZone();
		this.currentZone = currentZone;
		if (currentZone == null) {
			currentTimeline = null;
			currentCust = null;
			timelineEntries = Collections.emptyList();
		}
		else {
			currentTimeline = backend.getTimeline(currentZone);
			currentCust = backend.getCustomSettings(currentZone);
			timelineEntries = new ArrayList<>(currentTimeline.getRawEntries());
		}
	}

	private @Nullable Long getCurrentSelectedZone() {
		TimelineInfo selected = timelineChooserModel.getSelectedValue();
		if (selected == null) {
			return null;
		}
		return selected.zoneId();
	}

	@Override
	public int getSortOrder() {
		return 9;
	}

	private static class TimelineEntryTypeRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			String text;
			String tooltip;
			if (value instanceof CustomTimelineEntry custom) {
				if (custom.replaces() == null) {
					text = "C";
					tooltip = "Custom user-added entry";
				}
				else {
					text = "O";
					tooltip = "Override of builtin timeline entry";
				}
			}
			else if (value instanceof TimelineEntry) {
				text = "B";
				tooltip = "Builtin timeline entry";
			}
			else {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
			Component comp = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
			RenderUtils.setTooltip(comp, tooltip);
			return comp;
		}
	}

	private static class DoubleTimeRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof Double time) {
				return super.getTableCellRendererComponent(table, formatTimeDouble(time), isSelected, hasFocus, row, column);
			}
			else {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	}

	private static final DecimalFormat truncateTrailingZeroes = new DecimalFormat("00.###");

	private static String formatTimeDouble(double time) {
		int minutes = (int) (time / 60);
		double seconds = (time % 60);
		return String.format("%s (%s:%s)", time, minutes, truncateTrailingZeroes.format(seconds));
	}
}
