package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public interface OmenInfo extends HasAbility {

	/**
	 * @return The time the ability is expected fire (if casting) or when it actually did fire (if used)
	 */
	Instant happensAt();

	default Duration timeDeltaFrom(Instant compareTo) {
		return Duration.between(happensAt(), compareTo);
	};

	HasSourceEntity event();

	@Nullable Position position();

	@Nullable XivCombatant target();

	OmenEventType type();
}
