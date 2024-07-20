package gg.xp.xivsupport.events.actlines.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HasCastPrecursor {
	@Nullable
	AbilityCastStart getPrecursor();

	void setPrecursor(@NotNull AbilityCastStart precursor);
}
