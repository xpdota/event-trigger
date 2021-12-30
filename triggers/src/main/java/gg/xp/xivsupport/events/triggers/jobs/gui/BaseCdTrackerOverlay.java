package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseCdTrackerOverlay extends XivOverlay {
	private static final Logger log = LoggerFactory.getLogger(BaseCdTrackerOverlay.class);

	private final IntSetting numberOfRows;
	private final CustomTableModel<VisualCdInfo> tableModel;
	private final JTable table;
	private volatile Map<CdTrackingKey, AbilityUsedEvent> currentCds = Collections.emptyMap();
	private volatile List<VisualCdInfo> croppedCds = Collections.emptyList();
	private volatile List<BuffApplied> currentBuffs = Collections.emptyList();

	private static final int BAR_WIDTH = 150;

	protected BaseCdTrackerOverlay(String title, String settingKeyBase, PersistenceProvider persistence, IntSetting rowSetting) {
		super(title, settingKeyBase, persistence);
		numberOfRows = rowSetting;
		numberOfRows.addListener(this::repackSize);
		tableModel = CustomTableModel.builder(() -> croppedCds)
				.addColumn(new CustomColumn<>("Icon", c -> c.getEvent().getAbility(), c -> {
					c.setCellRenderer(new ActionAndStatusRenderer(true, false, false));
					c.setMaxWidth(22);
					c.setMinWidth(22);
				}))
				.addColumn(new CustomColumn<>("Bar", Function.identity(),
						c -> {
							c.setCellRenderer(new CdBarRenderer());
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
		getPanel().add(table);
		RefreshLoop<BaseCdTrackerOverlay> refresher = new RefreshLoop<>("CdTracker", this, BaseCdTrackerOverlay::refresh, dt -> Math.max((long) (50 / getScale()), 20));
		repackSize();
		refresher.start();
	}

	private void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows.get()));
		getFrame().revalidate();
		redoScale();
	}

	protected abstract Map<CdTrackingKey, AbilityUsedEvent> getCooldowns();
	protected abstract List<BuffApplied> getBuffs();

	private void getAndSort() {
		Map<CdTrackingKey, AbilityUsedEvent> newCurrentCds = getCooldowns();
		List<BuffApplied> newCurrentBuffs = getBuffs();
		if (!newCurrentCds.equals(currentCds) || !newCurrentBuffs.equals(currentBuffs)) {
			if (newCurrentCds.isEmpty()) {
				currentCds = Collections.emptyMap();
			}
			currentCds = newCurrentCds;
			currentBuffs = newCurrentBuffs;
			// TODO: make limit configurable
			List<VisualCdInfo> out = new ArrayList<>();
			currentCds.forEach((k, abilityUsed) -> {
				Cooldown cd = k.getCooldown();
				BuffApplied buffApplied = newCurrentBuffs.stream()
						.filter(b -> cd.buffIdMatches(b.getBuff().getId()))
						.filter(b -> b.getSource().walkParentChain().equals(abilityUsed.getSource().walkParentChain()))
						.findFirst()
						.orElse(null);
				VisualCdInfo vci = new VisualCdInfo(cd, abilityUsed, buffApplied);
				if (vci.stillValid()) {
					out.add(vci);
				}
			});

			croppedCds = out.stream()
					.limit(numberOfRows.get())
					.collect(Collectors.toList());
		}
		if (croppedCds.stream().noneMatch(VisualCdInfo::stillValid)) {
			croppedCds = Collections.emptyList();
		}
	}


	private void refresh() {
		getAndSort();
		SwingUtilities.invokeLater(tableModel::fullRefresh);
	}
}
