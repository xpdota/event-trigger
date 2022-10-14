package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.models.XivAbility;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BaseCdTrackerTable {
	private final CustomTableModel<VisualCdInfo> tableModel;
	private final JTable table;
	private static final int BAR_WIDTH = 150;

	public BaseCdTrackerTable(Supplier<List<? extends VisualCdInfo>> supplier) {
		this(supplier, DefaultCdTrackerColorProvider.INSTANCE);
	}

	public BaseCdTrackerTable(Supplier<List<? extends VisualCdInfo>> supplier, CdColorProvider colors) {
		tableModel = CustomTableModel.builder(supplier)
				.addColumn(new CustomColumn<>("Icon", c -> {
					AbilityUsedEvent ability = c.getEvent();
					// If there was no ability (e.g. CD has not fired yet), try to use
					// the icon of the cooldown's primary ability.
					if (ability == null) {
						return ActionLibrary.iconForId(c.getPrimaryAbilityId());
					}
					else {
						// If there was an ability use, try to use the icon from that.
						ActionIcon icon = ActionLibrary.iconForId(ability.getAbility().getId());
						// If the above failed to get us a usable icon, our last resort is to see if the
						if (icon == null) {
							return ability.getEffectsOfType(StatusAppliedEffect.class)
									.stream()
									.map(sae -> StatusEffectLibrary.iconForId(sae.getStatus().getId(), sae.getStacks()))
									.filter(Objects::nonNull)
									.findFirst()
									.orElse(null);
						}
						return icon;
					}
				}, c -> {
					c.setCellRenderer(new ActionAndStatusRenderer(true, false, false));
					c.setMaxWidth(22);
					c.setMinWidth(22);
				}))
				.addColumn(new CustomColumn<>("Bar", Function.identity(),
						c -> {
							c.setCellRenderer(new CdBarRenderer(colors));
							c.setMaxWidth(BAR_WIDTH);
							c.setMinWidth(BAR_WIDTH);
						}))
				.build();
		table = new JTable(tableModel);
		table.setOpaque(false);
		table.setFocusable(false);
		table.setRowSelectionAllowed(false);
		table.setCellSelectionEnabled(false);
		tableModel.configureColumns(table);
	}

	public CustomTableModel<VisualCdInfo> getTableModel() {
		return tableModel;
	}

	public JTable getTable() {
		return table;
	}
}
