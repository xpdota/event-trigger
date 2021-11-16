package gg.xp.xivsupport.models;

public enum CombatantType {
	/**
	 * Player characters (Any player, not just *the* player)
	 */
	PC,
	/**
	 * NPC, including pets/minions/companions
	 */
	NPC,
	/**
	 * Special type for fake actors that are related to a real actor.
	 *
	 * Note that fake actors that do not seem to match up to a real actor still return 'NPC'
	 */
	FAKE,
	/**
	 * Gathering point
	 */
	GP
}
