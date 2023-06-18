package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivdata.data.*;
import org.jetbrains.annotations.Nullable;

public record ActionOmenInfo(OmenType type, int rawEffectRange, int xAxisModifier, int coneAngle) {

	public static ActionOmenInfo fromAction(long id) {
		return fromActionInfo(ActionLibrary.forId(id));
	}

	public static @Nullable ActionOmenInfo fromActionInfo(ActionInfo ai) {
		if (ai == null) {
			return null;
		}
		OmenType type = OmenType.fromCastType(ai.castType());
		if (type == null) {
			return null;
		}
		return new ActionOmenInfo(type, ai.effectRange(), ai.xAxisModifier(), ai.coneAngle());
	}

	public boolean isRaidwide() {
		return type.shape() == OmenShape.CIRCLE && rawEffectRange >= 50;
	}

}
