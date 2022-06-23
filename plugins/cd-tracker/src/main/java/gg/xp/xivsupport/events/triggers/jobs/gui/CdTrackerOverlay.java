package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.CdTracker;
import gg.xp.xivsupport.events.state.combatstate.CooldownHelper;
import gg.xp.xivsupport.events.state.combatstate.CooldownStatus;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.OverlayMain;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class CdTrackerOverlay extends BaseCdTrackerOverlay {

	private final CdTracker cdTracker;
	private final CooldownHelper cdh;

	public CdTrackerOverlay(PersistenceProvider persistence, CdTracker cdTracker, OverlayConfig oc, CooldownHelper cdh) {
		super("Cd Tracker", "cd-tracker.overlay", oc, persistence, cdTracker.getOverlayMaxPersonal());
		this.cdTracker = cdTracker;
		this.cdh = cdh;
	}

	@Override
	protected List<CooldownStatus> getCds() {
		return cdh.getCooldowns(XivCombatant::isThePlayer, cdTracker::isEnabledForPersonalOverlay);
	}
}
