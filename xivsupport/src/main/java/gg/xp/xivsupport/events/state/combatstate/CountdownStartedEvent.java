package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.time.Duration;

/**
 * Represents a player beginning a countdown.
 */
public class CountdownStartedEvent extends BaseEvent implements HasDuration, HasSourceEntity {

	@Serial
	private static final long serialVersionUID = 4243145380243772257L;
	private final Duration initialDuration;
	private final XivCombatant source;
	private boolean isCanceled;

	public CountdownStartedEvent(Duration initialDuration, XivCombatant source) {
		this.initialDuration = initialDuration;
		this.source = source;
	}

	/**
	 * @return The initial duration of the countdown
	 */
	@Override
	public Duration getInitialDuration() {
		return initialDuration;
	}

	/**
	 * Indicates that the countdown has been canceled.
	 */
	public void markAsCanceled() {
		this.isCanceled = true;
	}

	/**
	 * @return whether the countdown has been canceled.
	 */
	public boolean isCanceled() {
		return isCanceled;
	}

	/**
	 * @return The player who started the countdown.
	 */
	@Override
	public XivCombatant getSource() {
		return source;
	}
}
