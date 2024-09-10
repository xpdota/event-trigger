package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

/**
 * Represents an LB gauge update.
 * <p>
 * Please make sure you are handling things correctly when there is no LB available in the duty.
 * See {@link #isLbAvailable()}
 */
public class LimitBreakGaugeEvent extends BaseEvent implements HasPrimaryValue {

	public static final int PER_BAR = 10_000;
	@Serial
	private static final long serialVersionUID = -8062227546447193172L;
	// total number of bars
	private final int totalBars;
	// LB amount, each bar is 10000
	private final int rawAmount;

	public LimitBreakGaugeEvent(int totalBars, int rawAmount) {
		this.totalBars = totalBars;
		this.rawAmount = rawAmount;
	}

	/**
	 * @return The number of possible bars. e.g. 3 bars in a raid. Not affected by how many bars are full.
	 */
	public int getTotalBars() {
		return totalBars;
	}

	/**
	 * @return The maximum for {@link #getCurrentRawValue()} as determined by {@link #getTotalBars()}.
	 */
	public int getMaxRawValue() {
		return totalBars * PER_BAR;
	}

	/**
	 * @return The raw amount of the LB gauge, as an integer. Each 10,000 increment is one bar.
	 * @see #PER_BAR
	 */
	public int getCurrentRawValue() {
		return rawAmount;
	}

	/**
	 * @return The current LB gauge state as a float. Equivalent to {@link #getCurrentRawValue()} / {@link #PER_BAR} (floating
	 * point division).
	 */
	public float getCurrentValue() {
		return ((float) rawAmount) / PER_BAR;
	}

	/**
	 * @return the number of completely full bars.
	 */
	public int getFullBars() {
		return rawAmount / PER_BAR;
	}

	/**
	 * @return whether the LB gauge is empty. If no LB is available in the duty, always returns true.
	 */
	public boolean isEmpty() {
		return rawAmount == 0;
	}

	/**
	 * @return whether the LB guage is full. If no LB is available in the duty, always returns true.
	 */
	public boolean isFull() {
		return rawAmount >= getMaxRawValue();
	}

	/**
	 * @return whether LB is currently usable, i.e. at least one complete LB bar is full.
	 */
	public boolean isLbUsable() {
		return getFullBars() > 0;
	}

	/**
	 * @return whether there is any LB bar whatsoever, regardless of current state. i.e. whether the current duty
	 * and party size supports LBs.
	 */
	public boolean isLbAvailable() {
		return totalBars > 0;
	}

	@Override
	public String toString() {
		return "LimitBreakGaugeEvent{" +
		       "totalBars=" + totalBars +
		       ", rawAmount=" + rawAmount +
		       '}';
	}

	@Override
	public String getPrimaryValue() {
		if (totalBars > 0) {
			int bars = getFullBars();
			String barsFmt = bars > 0 ? ("LB" + bars) : "No LB";
			return "%s / %s (%s)".formatted(rawAmount, getMaxRawValue(), barsFmt);
		}
		else {
			return "0 / 0 (LB Unavailable)";
		}
	}
}
