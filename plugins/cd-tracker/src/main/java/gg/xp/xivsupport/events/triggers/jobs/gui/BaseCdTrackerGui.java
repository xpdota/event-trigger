package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivdata.data.Job;
import gg.xp.xivdata.data.JobType;
import gg.xp.xivsupport.cdsupport.CustomCooldownsUpdated;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.CooldownSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class BaseCdTrackerGui implements PluginTab {

	private final BaseCdTrackerOverlay overlay;
	private CustomTableModel<CdInfo> model;

	protected BaseCdTrackerGui(BaseCdTrackerOverlay overlay) {
		this.overlay = overlay;
	}

	protected abstract LongSetting cdAdvance();

	protected abstract BooleanSetting enableTts();

	protected abstract BooleanSetting enableOverlay();

	protected abstract IntSetting overlayMax();

	protected abstract Map<ExtendedCooldownDescriptor, CooldownSetting> cds();

	// TODO: here's how I can do settings in a reasonable way:
	// Have a gui with 5 or so checkboxes (all on/off, this, DPS, healer, tank)
	// Let each callout be selectable (including ctrl/shift)
	// Then cascade changes to multiple callouts


	// New idea: list them in a table, also have icons for job and ability
	private record CdInfo(ExtendedCooldownDescriptor cd, CooldownSetting setting) {

	}

	List<CdInfo> getCds() {
		Map<ExtendedCooldownDescriptor, CooldownSetting> cooldowns = cds();
		// TODO: idea for how to do separate TTS/visual plus icons
		// Instead of one checkbox per ability, just have one for TTS, and one for visual, and then
		// have a label with icon and text.
		// Alternatively, have a table with a bunch of checkbox columns

		List<CdInfo> sortedCds = new ArrayList<>(cooldowns.entrySet().stream().map(entry -> new CdInfo(entry.getKey(), entry.getValue())).toList());
		sortedCds.sort(Comparator.comparing(cdi -> {
			ExtendedCooldownDescriptor cd = cdi.cd;
			Job job = cd.getJob();
			// Sort job categories first
			if (job == null) {
				JobType jobType = cd.getJobType();
				if (jobType == null) {
					// Put custom user-added CDs first
					return -2;
				}
				// Then categories
				return jobType.ordinal() + 5000;
			}
			// Then jobs
			return job.defaultPartySortOrder() + 10000;
		}));
		return sortedCds;
	}

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

		settingsPanel.add(Box.createHorizontalStrut(32_000));
		SettingsCdTrackerColorProvider colors = overlay.getColors();
		settingsPanel.add(new ColorSettingGui(colors.getActiveSetting(), "Active Color", enableOverlaySetting::get).getComponent());
		settingsPanel.add(new ColorSettingGui(colors.getReadySetting(), "Ready Color", enableOverlaySetting::get).getComponent());
		settingsPanel.add(new ColorSettingGui(colors.getOnCdSetting(), "On CD Color", enableOverlaySetting::get).getComponent());

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

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
		model = CustomTableModel.builder(this::getCds)
				.addColumn(StandardColumns.booleanSettingColumn("Overlay", cd -> cd.setting.getOverlay(), 50, enableOverlaySetting))
				.addColumn(StandardColumns.booleanSettingColumn("TTS (Ready)", cd -> cd.setting.getTtsReady(), 80, enableTtsSetting))
				.addColumn(StandardColumns.booleanSettingColumn("TTS (On Use)", cd -> cd.setting.getTtsOnUse(), 80, enableTtsSetting))
				.addColumn(new CustomColumn<>("Job", cdi -> {
					ExtendedCooldownDescriptor cd = cdi.cd;
					if (cd.getJob() != null) {
						return cd.getJob();
					}
					else {
						JobType jobType = cd.getJobType();
						if (jobType != null) {
							return jobType.getFriendlyName();
						}
						else {
							return "?";
						}
					}
				},
						col -> {
							col.setCellRenderer(new JobRenderer());
							col.setMinWidth(100);
							col.setMaxWidth(100);
							col.setCellEditor(new NoCellEditor());
						}))
				.addColumn(new CustomColumn<>("Skill",
						cdi -> new XivAbility(cdi.cd.getPrimaryAbilityId(), cdi.cd.getLabel()), c -> {
					c.setCellRenderer(new ActionAndStatusRenderer());
					c.setCellEditor(new NoCellEditor());
				}))
				.addColumn(new CustomColumn<>("Cooldown", cdi -> {
					ExtendedCooldownDescriptor cd = cdi.cd;
					int maxCharges = cd.getMaxCharges();
					if (maxCharges > 1) {
						return String.format("%s (%d charges)", cd.getCooldown(), maxCharges);
					}
					return cd.getCooldown();
				}, c -> c.setCellEditor(new NoCellEditor())))
//				.addColumn(new CustomColumn<>("Cooldown (from CSV)", cd -> {
//					ActionInfo actionInfo = ActionLibrary.forId(cd.getPrimaryAbilityId());
//					if (actionInfo == null) {
//						return "null";
//					}
//					double cdAi = actionInfo.getCd();
//					int maxChargesAi = actionInfo.maxCharges();
//
//					if (maxChargesAi == cd.getMaxCharges() && cd.getCooldown() == cdAi) {
//						return "";
//					}
//					if (maxChargesAi > 1) {
//						return String.format("%s (%d charges)", cdAi, maxChargesAi);
//					}
//					return cdAi;
//				}))
//				.addColumn(new CustomColumn<>("Max Charges", cooldown -> {
//					int maxCharges = cooldown.getMaxCharges();
//					return maxCharges > 1 ? maxCharges : null;
//				}, 100))
//				.addColumn(new CustomColumn<>("Raw class/job/category", cd -> {
//					ActionInfo actionInfo = ActionLibrary.forId(cd.getPrimaryAbilityId());
//					return actionInfo == null ? null : actionInfo.categoryRaw();
//				}))
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

	@HandleEvents(order = 100)
	public void cooldownsUpdated(EventContext context, CustomCooldownsUpdated event) {
		if (model != null) {
			model.signalNewData();
		}
	}
}
