package gg.xp.xivsupport.events.actlines.events;

/**
 * Used to retrieve target index information.
 * <p>
 * For example, if an AoE ability hits 5 targets, each of the 5 targets will have a separate 22-line from ACT.
 * Each one will have a target index of 0-4, with the 'number of targets' field set to 5.
 * <p>
 * An ability that only hits a single target will have an index of 0 and a number of targets of 1. An ability that
 * whiffs completely will be the same, since the "target" is ENVIRONMENT (0xE0000000).
 * <p>
 * Use of this is a very effective way of suppressing duplicate callouts for AoE ability usages.
 */
public interface HasTargetIndex {

	long getTargetIndex();

	/**
	 * @return The number of targets hit by this action. Each event represents one target getting hit. This may be
	 * zero if no targets were hit, as there still must be an event generated to indicate such.
	 */
	long getNumberOfTargets();

	default boolean isFirstTarget() {
		return getTargetIndex() == 0;
	}

	default boolean isLastTarget() {
		return getTargetIndex() >= getNumberOfTargets() - 1;
	}
}
