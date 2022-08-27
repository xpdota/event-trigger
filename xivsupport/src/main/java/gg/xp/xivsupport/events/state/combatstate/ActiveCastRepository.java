package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ActiveCastRepository {
	@Nullable CastTracker getCastFor(XivCombatant cbt);

	List<CastTracker> getAll();
}
