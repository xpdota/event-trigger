package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.util.List;

public class CustomBuffsBarPartyComponent extends BaseBuffsBarPartyComponent {
	private final StatusEffectRepository buffRepo;
	private final CustomBuffsBarConfig config;

	public CustomBuffsBarPartyComponent(StatusEffectRepository buffRepo, CustomBuffsBarConfig config) {
		super(config);
		this.buffRepo = buffRepo;
		this.config = config;
	}

	@Override
	protected List<BuffApplied> getBuffsToDisplay(XivPlayerCharacter xpc) {
		return buffRepo.filteredSortedStatusesOnTarget(xpc, ba -> config.isBuffAllowed(ba.getBuff().getId()),
				config.getShowPreapps().get());
	}
}
