package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.state.combatstate.CooldownStatus;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.RefreshType;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public abstract class BaseCdTrackerOverlay extends XivOverlay {
	private static final Logger log = LoggerFactory.getLogger(BaseCdTrackerOverlay.class);

	private final IntSetting numberOfRows;
	private final CustomTableModel<VisualCdInfo> tableModel;
	private final JTable table;
	private final SettingsCdTrackerColorProvider colors;
	private final BooleanSetting onlyActive;
	//	private volatile Map<CdTrackingKey, AbilityUsedEvent> currentCds = Collections.emptyMap();
	private volatile List<VisualCdInfo> croppedCds = Collections.emptyList();
//	private volatile List<BuffApplied> currentBuffs = Collections.emptyList();
	private volatile List<CooldownStatus> currentCds;

	protected BaseCdTrackerOverlay(String title, String settingKeyBase, OverlayConfig oc, PersistenceProvider persistence, IntSetting rowSetting) {
		super(title, settingKeyBase, oc, persistence);
		numberOfRows = rowSetting;
		numberOfRows.addListener(this::repackSize);

		CdColorProvider defaults = DefaultCdTrackerColorProvider.INSTANCE;
		onlyActive = new BooleanSetting(persistence, settingKeyBase + ".only-show-active", false);

		colors = SettingsCdTrackerColorProvider.of(persistence, settingKeyBase, defaults);
		BaseCdTrackerTable tableHolder = new BaseCdTrackerTable(() -> croppedCds, colors);
		tableModel = tableHolder.getTableModel();
		table = tableHolder.getTable();
		getPanel().add(table);
		// Logic: try to pick optimal framerate based on bar size, but clamp between 15 and 60 FPS (66 and 16 ms per frame).
		// Upper bound is necessary to avoid performance hits from excessively fast refreshes
		// Lower bound is necessary to avoid the numerical counter not updating quickly enough
		// For the non-clamped amount, we assume 30 seconds
		RefreshLoop<BaseCdTrackerOverlay> refresher = new RefreshLoop<>("CdTracker", this, BaseCdTrackerOverlay::refresh, ct -> ct.calculateScaledFrameTime(200));
		repackSize();
		refresher.start();
	}

	@Override
	protected void repackSize() {
		table.setPreferredSize(new Dimension(table.getPreferredSize().width, table.getRowHeight() * numberOfRows.get()));
		super.repackSize();
	}


	protected abstract List<CooldownStatus> getCds();

	private RefreshType getAndSort() {
		if (!getEnabled().get()) {
			croppedCds = Collections.emptyList();
			return RefreshType.NONE;
		}
		List<CooldownStatus> newCds = getCds();
		if (newCds.equals(currentCds) && !onlyActive.get()) {
			if (newCds.isEmpty()) {
				return RefreshType.NONE;
			}
			if (croppedCds.isEmpty()) {
				return RefreshType.REPAINT;
			}
		}
		else {
			currentCds = newCds;
			croppedCds = newCds.stream()
					.sorted(Comparator.comparing(e -> e.cdKey().getCooldown().sortOrder()))
					.limit(numberOfRows.get())
					.map((Function<? super CooldownStatus, VisualCdInfo>) VisualCdInfoMain::new)
					.filter(VisualCdInfo::stillValid)
					.filter(vci -> {
						if (onlyActive.get()) {
							CdStatus status = vci.getStatus();
							return status == CdStatus.BUFF_ACTIVE || status == CdStatus.BUFF_PREAPP;
						}
						else {
							return true;
						}
					})
					.toList();
		}
		return RefreshType.FULL;
	}


	private void refresh() {
		RefreshType refreshTypeNeeded = getAndSort();
		switch (refreshTypeNeeded) {
			case FULL -> tableModel.fullRefresh();
			case REPAINT -> table.repaint();
		}
	}

	public SettingsCdTrackerColorProvider getColors() {
		return colors;
	}

	public BooleanSetting getOnlyActive() {
		return onlyActive;
	}
}
