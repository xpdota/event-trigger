package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

// TODO: cooldown and stuff? What else would we want here?
public record ActionInfo(
		long actionid,
		String name,
		long iconId
) {
	public @Nullable ActionIcon getIcon() {
		return ActionLibrary.iconForInfo(this);
	}
}
