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
	 * Pet of a player
	 */
	PET,
	/**
	 * Special type for fake actors that are related to a real actor.
	 *
	 * Note that fake actors that do not seem to match up to a real actor still return 'NPC'
	 */
	FAKE,
	/**
	 * Non-combat NPC
	 */
	NONCOM,
	/**
	 * Gathering point
	 */
	GP,
	/**
	 * Catch-all for everything else (housing items, etc)
	 */
	OTHER
}
