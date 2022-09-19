package gg.xp.xivdata.data.duties;

import org.jetbrains.annotations.Nullable;

public interface Duty {
	String getName();

	Expansion getExpac();

	DutyType getType();

	@Nullable Long getZoneId();
}
