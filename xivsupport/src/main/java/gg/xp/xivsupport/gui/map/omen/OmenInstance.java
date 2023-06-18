package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

public interface OmenInstance extends HasAbility {

	/**
	 * @return The time the ability is expected fire (if casting) or when it actually did fire (if used)
	 */
	Instant happensAt();

	default Duration timeDeltaFrom(Instant compareTo) {
		return Duration.between(happensAt(), compareTo);
	};

	@NotNull XivCombatant source();


	@Nullable Position omenPosition(Function<XivCombatant, Position> freshPosLookup);
	OmenEventType type();

	// If this is null, why did we make an omen in the first place?
	@NotNull ActionOmenInfo info();

	default float radius() {
		int raw = info().rawEffectRange();
		if (info().type().addHitbox()) {
			return raw + source().getRadius();
		}
		return raw;
	}
}
