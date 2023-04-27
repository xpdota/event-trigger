package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.scan.Alias;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Alias("casts")
public interface ActiveCastRepository {
	@Nullable CastTracker getCastFor(XivCombatant cbt);

	List<CastTracker> getAll();
}
