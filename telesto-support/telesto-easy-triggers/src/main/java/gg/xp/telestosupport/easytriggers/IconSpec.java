package gg.xp.telestosupport.easytriggers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import org.jetbrains.annotations.Nullable;

public class IconSpec {

	public IconType type = IconType.IconId;
	public long value;

	public @Nullable Long toIconId(@Nullable Event event) {
		switch (type) {
			case IconId -> {
				return value;
			}
			case AbilityId -> {
				ActionInfo ai = ActionLibrary.forId(value);
				if (ai != null) {
					return ai.iconId();
				}
			}
			case StatusId -> {
				StatusEffectInfo sei = StatusEffectLibrary.forId(value);
				if (sei != null) {
					return sei.baseIconId();
				}
			}
			case AbilityAuto -> {
				if (event instanceof HasAbility ha) {
					ActionInfo ai = ActionLibrary.forId(ha.getAbility().getId());
					if (ai != null) {
						return ai.iconId();
					}
				}
			}
			case StatusAuto -> {
				if (event instanceof HasStatusEffect hse) {
					StatusEffectInfo sei = StatusEffectLibrary.forId(hse.getBuff().getId());
					if (sei != null) {
						return sei.iconIdForStackCount(hse.getStacks());
					}
				}
			}
		}
		return null;
	}
}
