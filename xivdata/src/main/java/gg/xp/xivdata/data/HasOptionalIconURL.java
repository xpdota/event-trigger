package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface HasOptionalIconURL {
	@Nullable HasIconURL getIconUrl();
}
