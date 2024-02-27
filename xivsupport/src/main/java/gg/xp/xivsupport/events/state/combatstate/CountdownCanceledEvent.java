package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Represents a player canceling a countdown.
 */
public class CountdownCanceledEvent extends BaseEvent implements HasSourceEntity {

	@Serial
	private static final long serialVersionUID = 877893557630215691L;
	private final XivCombatant source;
	private final @Nullable CountdownStartedEvent canceledCountdown;

	public CountdownCanceledEvent(XivCombatant source, @Nullable CountdownStartedEvent canceledCountdown) {
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
