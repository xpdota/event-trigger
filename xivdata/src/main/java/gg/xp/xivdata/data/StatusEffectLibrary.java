package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class StatusEffectLibrary {

	private static final StatusEffectLibraryImpl INSTANCE = new StatusEffectLibraryImpl(StatusEffectLibrary.class.getResourceAsStream("/xiv/statuseffect/StatusEffect.oos.gz"));

	public static void main(String[] args) {
		INSTANCE.getAll().values().stream().distinct().sorted().map(s -> String.format("%06d", s.statusEffectId())).forEach(System.out::println);
	}

	public static Map<Integer, StatusEffectInfo> getAll() {
		return INSTANCE.getAll();
	}

	public static @Nullable StatusEffectInfo forId(long id) {
		return getAll().get((int) id);
	}

	public static int getMaxStacks(long buffId) {
		// There are two main considerations here.
		// Sometimes, the 'stacks' value is used to represent something other than stacks (like on NIN)
		// Therefore, we have to assume that it is a garbage value and assume 0 stacks (i.e. not a stacking buff)
		// if rawStacks > maxStacks.
		// However, there are also unknown status effects, therefore we just assume 16 is the max for those, since that
		// seems to be the max for any legitimate buff.
		StatusEffectInfo statusEffectInfo = forId(buffId);
		long maxStacks;
		if (statusEffectInfo == null) {
			maxStacks = 16;
		}
		else {
			maxStacks = statusEffectInfo.maxStacks();
		}
		//noinspection NumericCastThatLosesPrecision - never that high
		return (int) maxStacks;
	}

	public static int calcActualStacks(long buffId, long rawStacks) {
		int maxStacks = getMaxStacks(buffId);
		if (rawStacks >= 0 && rawStacks <= maxStacks) {
			return (int) rawStacks;
		}
		else {
			return 0;
		}
	}

	public static @Nullable StatusEffectIcon iconForId(long id, long stacks) {
		return INSTANCE.iconForId(id, stacks);
	}

	public static @Nullable StatusEffectIcon iconId(long effectiveIconId) {
		return INSTANCE.iconId(effectiveIconId);
	}
}
