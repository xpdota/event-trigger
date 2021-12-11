package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.triggers.jobs.CdTracker;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.apache.commons.lang3.mutable.MutableBoolean;
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

@ScanMe
public class CdTrackerOverlay extends XivOverlay {

	private static final Logger log = LoggerFactory.getLogger(CdTrackerOverlay.class);

	// TODO make this a setting
	private static final int MAX_CDS = 8;

	private final CdTracker cdTracker;
	private final StatusEffectRepository buffRepo;
	private final CustomTableModel<VisualCdInfo> tableModel;
	private volatile Map<CdTrackingKey, AbilityUsedEvent> currentCds = Collections.emptyMap();
	private volatile List<VisualCdInfo> croppedCds = Collections.emptyList();
	private volatile List<BuffApplied> currentBuffs = Collections.emptyList();

	private static final int BAR_WIDTH = 150;

	public CdTrackerOverlay(PersistenceProvider persistence, CdTracker cdTracker, StatusEffectRepository buffRepo) {
		super("Cd Tracker", "cd-tracker.overlay", persistence);
		this.cdTracker = cdTracker;
		this.buffRepo = buffRepo;
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
		getPanel().setPreferredSize(new Dimension(200, 200));
		JTable table = new JTable(tableModel);
		table.setOpaque(false);
		tableModel.configureColumns(table);
		getPanel().add(table);
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					refresh();
					Thread.sleep(100);
				}
				catch (Throwable e) {
					log.error("Error refreshing dots", e);
				}
			}
		});
		thread.setName("DotRefreshOverlayThread");
		//noinspection CallToThreadStartDuringObjectConstruction
		thread.start();
	}

	private void getAndSort() {
		Map<CdTrackingKey, AbilityUsedEvent> newCurrentCds = cdTracker.getCurrentCooldowns();
		List<BuffApplied> newCurrentBuffs = this.buffRepo.getBuffs();
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
				BuffApplied buffApplied = newCurrentBuffs.stream().filter(b -> cd.buffIdMatches(b.getBuff().getId())).findFirst().orElse(null);
				out.add(new VisualCdInfo(cd, abilityUsed, buffApplied));
			});

			croppedCds = out.stream()
					.limit(MAX_CDS)
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
