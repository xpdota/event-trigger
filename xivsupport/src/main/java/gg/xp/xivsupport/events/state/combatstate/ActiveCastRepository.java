package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface ActiveCastRepository {
	@Nullable CastTracker getCastFor(XivCombatant cbt);

	List<CastTracker> getAll();

	default Optional<CastTracker> getActiveCastById(long... ids) {
		return getAll()
				.stream()
				.filter(ct -> ct.getResult() == CastResult.IN_PROGRESS)
				.filter(ct -> ct.getCast().abilityIdMatches(ids))
				.findFirst();
	}
}
