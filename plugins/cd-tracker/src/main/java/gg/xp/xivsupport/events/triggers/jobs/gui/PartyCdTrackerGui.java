package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivsupport.events.state.combatstate.CdTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.BaseCdTrackerGui;
import gg.xp.xivsupport.events.triggers.jobs.gui.PartyCdTrackerOverlay;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.CooldownSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;

import java.util.Map;

@ScanMe
public class PartyCdTrackerGui extends BaseCdTrackerGui {

	private final CdTracker backend;
	private final PartyCdTrackerOverlay overlay;

	public PartyCdTrackerGui(CdTracker backend, PartyCdTrackerOverlay overlay) {
		super(overlay);
		this.backend = backend;
		this.overlay = overlay;
	}

	@Override
	public String getTabName() {
		return "Party Cooldown Tracker";
	}

	@Override
	public int getSortOrder() {
		return 7;
	}

	@Override
	protected LongSetting cdAdvance() {
		return backend.getCdTriggerAdvanceParty();
	}

	@Override
	protected BooleanSetting enableTts() {
		return backend.getEnableTtsParty();
	}

	@Override
	protected BooleanSetting enableOverlay() {
		return overlay.getEnabled();
	}

	@Override
	protected IntSetting overlayMax() {
		return backend.getOverlayMaxParty();
	}

	@Override
	protected Map<Cooldown, CooldownSetting> cds() {
		return backend.getPartyCdSettings();
	}

}
