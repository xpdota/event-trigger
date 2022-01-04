package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.CooldownSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

public abstract class BaseCdTrackerGui implements PluginTab {

	protected abstract LongSetting cdAdvance();

	protected abstract BooleanSetting enableTts();

	protected abstract BooleanSetting enableOverlay();

	protected abstract IntSetting overlayMax();

	protected abstract Map<Cooldown, CooldownSetting> cds();

	// TODO: here's how I can do settings in a reasonable way:
	// Have a gui with 5 or so checkboxes (all on/off, this, DPS, healer, tank)
	// Let each callout be selectable (including ctrl/shift)
	// Then cascade changes to multiple callouts


	// New idea: list them in a table, also have icons for job and ability

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel(getTabName());
		outerPanel.setLayout(new BorderLayout());

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new WrapLayout());

		JPanel preTimeBox = new LongSettingGui(cdAdvance(), "Time before expiry to call out (milliseconds)").getComponent();
		settingsPanel.add(preTimeBox);
		BooleanSetting enableTtsSetting = enableTts();
		JCheckBox enableTts = new BooleanSettingGui(enableTtsSetting, "Enable TTS").getComponent();
		settingsPanel.add(enableTts);
		BooleanSetting enableOverlaySetting = enableOverlay();
		JCheckBox enableOverlay = new BooleanSettingGui(enableOverlaySetting, "Enable Overlay").getComponent();
		settingsPanel.add(enableOverlay);
		JPanel numSetting = new IntSettingSpinner(overlayMax(), "Max in Overlay").getComponent();
		settingsPanel.add(numSetting);
		// TODO: bug here - doesn't cancel editing, so current cell enabled/disabled is stuck

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

		Map<Cooldown, CooldownSetting> cooldowns = cds();
		// TODO: idea for how to do separate TTS/visual plus icons
		// Instead of one checkbox per ability, just have one for TTS, and one for visual, and then
		// have a label with icon and text.
		// Alternatively, have a table with a bunch of checkbox columns

		List<Cooldown> sortedCds = new ArrayList<>(cds().keySet());
		sortedCds.sort(Comparator.comparing(cd -> {
			Job job = cd.getJob();
			// Sort job categories first
			if (job == null) {
				return cd.getJobType().ordinal();
			}
			return job.defaultPartySortOrder() + 10000;
		}));
		JTable table = new JTable() {
			@Override
			public boolean isCellEditable(int row, int column) {
				// TODO: make this more official
				if (getCellEditor(row, column) instanceof NoCellEditor) {
					return false;
				}
				return true;
			}
		};
		CustomTableModel<Cooldown> model = CustomTableModel.builder(() -> sortedCds)
				.addColumn(StandardColumns.booleanSettingColumn("Overlay", cd -> cooldowns.get(cd).getOverlay(), 50, enableOverlaySetting))
				.addColumn(StandardColumns.booleanSettingColumn("TTS", cd -> cooldowns.get(cd).getTts(), 50, enableTtsSetting))
				.addColumn(new CustomColumn<>("Job", cd -> {
					if (cd.getJob() != null) {
						return cd.getJob();
					}
					else {
						return cd.getJobType().getFriendlyName();
					}
				},
						col -> {
							col.setCellRenderer(new JobRenderer());
							col.setMinWidth(100);
							col.setMaxWidth(100);
						}))
				.addColumn(new CustomColumn<>("Cooldown", Cooldown::getLabel))
				.build();

		table.setModel(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		model.configureColumns(table);

		enableTtsSetting.addListener(() -> {
			TableCellEditor cellEditor = table.getCellEditor();
			if (cellEditor != null) {
				cellEditor.stopCellEditing();
			}
		});
		enableOverlaySetting.addListener(() -> {
			TableCellEditor cellEditor = table.getCellEditor();
			if (cellEditor != null) {
				cellEditor.stopCellEditing();
			}
		});
		enableTtsSetting.addListener(table::repaint);
		enableOverlaySetting.addListener(table::repaint);

		JScrollPane scroll = new JScrollPane(table);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(scroll);
		return outerPanel;
	}
}
