package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.CdTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class PartyCdTrackerOverlay extends BaseCdTrackerOverlay {

	private static final Logger log = LoggerFactory.getLogger(PartyCdTrackerOverlay.class);
	private final CdTracker cdTracker;
	private final StatusEffectRepository buffRepo;
	private final XivState state;

	public PartyCdTrackerOverlay(PersistenceProvider persistence, CdTracker cdTracker, OverlayConfig oc, StatusEffectRepository buffRepo, XivState state) {
		super("Party Cd Tracker", "party-cd-tracker.overlay", oc, persistence, cdTracker.getOverlayMaxParty());
		this.cdTracker = cdTracker;
		this.buffRepo = buffRepo;
		this.state = state;
	}

	@Override
	protected Map<CdTrackingKey, AbilityUsedEvent> getCooldowns() {
		return cdTracker.getOverlayPartyCds();
	}

	@Override
	protected List<BuffApplied> getBuffs() {
		//noinspection SuspiciousMethodCalls
		return this.buffRepo.getBuffsAndPreapps().stream()
				.filter(buff -> state.getPartyList().contains(buff.getSource().walkParentChain()))
				.collect(Collectors.toList());
	}
}
