package gg.xp.xivsupport.events.actlines.parsers;

public enum EntityLookupMissBehavior {
	/**
	 * Ignore completely. No warning. Should only be used for things like removing a combatant.
	 */
	IGNORE,
	/**
	 * Request a combatant refresh but do not warn. Useful for adding combatants.
	 */
	GET,
	/**
	 * Request a combatant refresh, but also warn. This should be used for most events.
	 */
	GET_AND_WARN
}
