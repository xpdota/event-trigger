package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivsupport.events.state.combatstate.CdTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.BaseCdTrackerGui;
import gg.xp.xivsupport.events.triggers.jobs.gui.CdTrackerOverlay;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.CooldownSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;

import java.util.Map;

@ScanMe
public class CdTrackerGui extends BaseCdTrackerGui {

	private final CdTracker backend;
	private final CdTrackerOverlay overlay;

	public CdTrackerGui(CdTracker backend, CdTrackerOverlay overlay) {
		super(overlay);
		this.backend = backend;
		this.overlay = overlay;
	}

	@Override
	public String getTabName() {
		return "Cooldown Tracker";
	}

	@Override
	public int getSortOrder() {
		return 5;
	}

	// TODO: here's how I can do settings in a reasonable way:
	// Have a gui with 5 or so checkboxes (all on/off, this, DPS, healer, tank)
	// Let each callout be selectable (including ctrl/shift)
	// Then cascade changes to multiple callouts

	// TODO: make a single settings class to hold these
	@Override
	protected LongSetting cdAdvance() {
		return backend.getCdTriggerAdvancePersonal();
	}

	@Override
	protected BooleanSetting enableTts() {
		return backend.getEnableTtsPersonal();
	}

	@Override
	protected IntSetting overlayMax() {
		return backend.getOverlayMaxPersonal();
	}

	@Override
	protected Map<ExtendedCooldownDescriptor, CooldownSetting> cds() {
		return backend.getPersonalCdSettings();
	}

	@Override
	protected BooleanSetting enableOverlay() {
		return overlay.getEnabled();
	}
}
