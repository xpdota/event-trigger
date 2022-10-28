package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.models.XivStatusEffect;

public final class BuffUtils {

	private BuffUtils() {
	}

	public static boolean isFcBuff(HasStatusEffect hse) {
		return isFcBuff(hse.getBuff());
	}

	private static boolean isFcBuff(XivStatusEffect buff) {
		StatusEffectInfo info = buff.getInfo();
		if (info == null) {
			return false;
		}
		return info.isFcBuff();
	}

	public static boolean isFoodBuff(HasStatusEffect hse) {
		return isFoodBuff(hse.getBuff());
	}

	private static boolean isFoodBuff(XivStatusEffect buff) {
		return buff.getId() == 0x30;
	}
}
