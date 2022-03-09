package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageEffect;

import java.util.List;

public interface HasEffects {

	List<AbilityEffect> getEffects();

	// TODO: possibly not accurate, need to account for parries and stuff
	default long getDamage() {
		int amount = 0;
		//noinspection Convert2streamapi
		for (AbilityEffect effect : getEffects()) {
			if (effect instanceof DamageEffect de) {
				amount+= de.getAmount();
			}
		}
		return amount;
	}
}
