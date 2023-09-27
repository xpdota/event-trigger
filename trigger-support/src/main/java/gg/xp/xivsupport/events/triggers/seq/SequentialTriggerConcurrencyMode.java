package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

/**
 * Determines what happens when a sequential trigger hits its start condition while it is already running.
 */
public enum SequentialTriggerConcurrencyMode implements HasFriendlyName {

	/**
	 * The default: Once the trigger has started, it cannot be started again until it has finished. Identical to the
	 * behavior before these settings were added.
	 */
	BLOCK_NEW("Do not allow new invocations while trigger is running"),
	/**
	 * When the start condition is hit while the trigger is running, the old invocation will be killed and replaced
	 * with a new invocation.
	 */
	REPLACE_OLD("New invocation will stop the old invocation and replace it"),
	/**
	 * Multiple invocations can run in parallel.
	 */
	CONCURRENT("Allow multiple concurrent invocations of this trigger"),
	;

	private final String friendlyName;

	SequentialTriggerConcurrencyMode(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
