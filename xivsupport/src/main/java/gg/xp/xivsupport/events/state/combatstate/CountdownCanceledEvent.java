package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a player canceling a countdown.
 */
public class CountdownCanceledEvent extends BaseEvent implements HasSourceEntity {

	private final XivPlayerCharacter source;
	private final @Nullable CountdownStartedEvent canceledCountdown;

	public CountdownCanceledEvent(XivPlayerCharacter source, @Nullable CountdownStartedEvent canceledCountdown) {
		this.source = source;
		this.canceledCountdown = canceledCountdown;
	}

	/**
	 * @return the {@link CountdownStartedEvent} which is being canceled (if known). null if unknown.
	 */
	public @Nullable CountdownStartedEvent getCanceledCountdown() {
		return canceledCountdown;
	}

	/**
	 * @return The player who canceled the countdown.
	 */
	@Override
	public XivCombatant getSource() {
		return source;
	}
}
