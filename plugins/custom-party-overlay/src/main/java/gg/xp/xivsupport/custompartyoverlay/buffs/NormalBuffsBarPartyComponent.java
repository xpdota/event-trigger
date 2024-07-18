package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.BuffUtils;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.util.List;

public class NormalBuffsBarPartyComponent extends BaseBuffsBarPartyComponent {

	private final StatusEffectRepository buffRepo;
	private final NormalBuffsBarConfig config;

	public NormalBuffsBarPartyComponent(StatusEffectRepository buffRepo, NormalBuffsBarConfig config) {
		super(config);
		this.buffRepo = buffRepo;
		this.config = config;
	}

	@Override
	protected List<BuffApplied> getBuffsToDisplay(XivPlayerCharacter xpc) {
		boolean showFc = config.getShowFcBuffs().get();
		boolean showFood = config.getShowFoodBuff().get();
		return buffRepo.filteredSortedStatusesOnTarget(xpc, ba -> {
			return (showFc || !BuffUtils.isFcBuff(ba)) && (showFood || !BuffUtils.isFoodBuff(ba));
		}, config.getShowPreapps().get());
	}
}
