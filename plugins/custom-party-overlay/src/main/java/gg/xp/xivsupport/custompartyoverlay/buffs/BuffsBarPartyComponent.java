package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.state.combatstate.BuffUtils;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class BuffsBarPartyComponent extends BasePartyListComponent {

	private final BuffsBar bar = new BuffsBar();
	private final StatusEffectRepository buffRepo;
	private final BuffsBarConfig config;

	public BuffsBarPartyComponent(StatusEffectRepository buffRepo, BuffsBarConfig config) {
		this.buffRepo = buffRepo;
		this.config = config;
		config.addAndRunListener(this::applySettings);
	}

	private void applySettings() {
		bar.setNormalBuffColor(config.getNormalTextColor().get());
		bar.setMyBuffColor(config.getMyBuffTextColor().get());
		bar.setRemovableBuffColor(config.getRemoveableBuffColor().get());
		bar.setXPadding(config.getxPadding().get());
		bar.setEnableShadows(config.getShadows().get());
		bar.setEnableTimers(config.getTimers().get());
		bar.reformat();
	}

	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		boolean showFc = config.getShowFcBuffs().get();
		boolean showFood = config.getShowFoodBuff().get();
		bar.setBuffs(buffRepo.filteredSortedStatusesOnTarget(xpc, ba -> {
			return (showFc || !BuffUtils.isFcBuff(ba)) && (showFood || !BuffUtils.isFoodBuff(ba));
		}));
	}
}
