package gg.xp.xivdata.data.rsv;

import org.jetbrains.annotations.Nullable;

public class NoopRsvLibrary implements RsvLibrary {
	@Override
	public @Nullable String get(String key) {
		return null;
	}
}
