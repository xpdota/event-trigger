package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jobs.CdTracker;
import gg.xp.xivsupport.events.triggers.jobs.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.renderers.ActionAndStatusRenderer;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
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
public class PartyCdTrackerOverlay extends BaseCdTrackerOverlay {

	private static final Logger log = LoggerFactory.getLogger(PartyCdTrackerOverlay.class);
	private final CdTracker cdTracker;
	private final StatusEffectRepository buffRepo;
	private final XivState state;

	public PartyCdTrackerOverlay(PersistenceProvider persistence, CdTracker cdTracker, StatusEffectRepository buffRepo, XivState state) {
		super("Party Cd Tracker", "party-cd-tracker.overlay", persistence, cdTracker.getOverlayMaxParty());
		this.cdTracker = cdTracker;
		this.buffRepo = buffRepo;
		this.state = state;
	}

	@Override
	protected Map<CdTrackingKey, AbilityUsedEvent> getCooldowns() {
		return cdTracker.getPartyCooldowns();
	}

	@Override
	protected List<BuffApplied> getBuffs() {
		return this.buffRepo.getBuffsAndPreapps().stream()
				.filter(buff -> state.getPartyList().contains(buff.getSource().walkParentChain()))
				.collect(Collectors.toList());
	}
}
