package gg.xp.xivsupport.custompartyoverlay.cdtracker;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.cdsupport.CustomCooldownsUpdated;
import gg.xp.xivsupport.events.triggers.jobs.gui.BaseCdTrackerGui;
import gg.xp.xivsupport.events.triggers.jobs.gui.SettingsCdTrackerColorProvider;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.gui.tables.renderers.JobRenderer;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CustomPartyCdTrackerGui extends JPanel {

	private final CustomPartyCdTrackerConfig config;
	private CustomTableModel<CdInfo> model;

	public CustomPartyCdTrackerGui(CustomPartyCdTrackerConfig config) {
		this.config = config;
		setLayout(new BorderLayout());
		JPanel topSettingsPanel = new JPanel();
		topSettingsPanel.setLayout(new WrapLayout());

		topSettingsPanel.add(new BooleanSettingGui(config.getRightToLeft(), "Right to Left", true).getComponent());
		topSettingsPanel.add(new IntSettingSpinner(config.getSpacing(), "Spacing Between Items").getComponent());
		topSettingsPanel.add(new IntSettingSpinner(config.getBorderWidth(), "Border Thickness").getComponent());
		topSettingsPanel.add(new IntSettingSpinner(config.getBorderRoundness(), "Roundness").getComponent());

		topSettingsPanel.add(Box.createHorizontalStrut(32_000));
		SettingsCdTrackerColorProvider colors = config.getColors();
		topSettingsPanel.add(new ColorSettingGui(colors.getActiveSetting(), "Active Color", () -> true).getComponent());
		topSettingsPanel.add(new ColorSettingGui(colors.getReadySetting(), "Ready Color", () -> true).getComponent());
		topSettingsPanel.add(new ColorSettingGui(colors.getOnCdSetting(), "On CD Color", () -> true).getComponent());
		topSettingsPanel.add(new ColorSettingGui(colors.getPreappSetting(), "Preapp Color", () -> true).getComponent());
//		topSettingsPanel.add(new ColorSettingGui(colors.getFontSetting(), "Font Color", () -> true).getComponent());

		add(topSettingsPanel, BorderLayout.PAGE_START);

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
				.addColumn(StandardColumns.booleanSettingColumn("Enable", cd -> cd.setting.getEnable(), 50, null))
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


		JScrollPane scroll = new JScrollPane(table);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scroll, BorderLayout.CENTER);
		JPanel bottomBettingsPanel = new JPanel();
		bottomBettingsPanel.setLayout(new WrapLayout());
		bottomBettingsPanel.add(new BooleanSettingGui(config.getScOnlyRez(), "Hide Swiftcast on Non-Rez Casters", true).getComponent());
		add(bottomBettingsPanel, BorderLayout.SOUTH);
		config.addListener(model::signalNewData);
	}

	private record CdInfo(ExtendedCooldownDescriptor cd, CustomPartyCdSetting setting) {

	}

	List<CdInfo> getCds() {
		Map<ExtendedCooldownDescriptor, CustomPartyCdSetting> cooldowns = config.getSettings();
		// TODO: idea for how to do separate TTS/visual plus icons
		// Instead of one checkbox per ability, just have one for TTS, and one for visual, and then
		// have a label with icon and text.
		// Alternatively, have a table with a bunch of checkbox columns

		List<CdInfo> sortedCds = new ArrayList<>(cooldowns.entrySet().stream().map(entry -> new CdInfo(entry.getKey(), entry.getValue())).toList());
		sortedCds.sort(Comparator.<CdInfo, Integer>comparing(cdi -> {
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
		}).thenComparing(cdi -> cdi.cd().getLabel()));
		return sortedCds;
	}

	@HandleEvents(order = 100)
	public void cooldownsUpdated(EventContext context, CustomCooldownsUpdated event) {
		if (model != null) {
			model.signalNewData();
		}
	}
}
