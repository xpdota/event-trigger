package gg.xp.xivsupport.gui.map.omen;

/**
 * Describes where in relation to the caster and target an omen should be drawn
 */
public enum OmenLocationType {
	/**
	 * Draw at the caster using the caster's facing angle
	 */
	CASTER,
	/**
	 * Draw at the caster, but facing the target (think baited cleaves where the NPC doesn't turn)
	 * <p>
	 * If no target is available, behaves the same as {@link #CASTER}
	 */
	CASTER_FACE_TARGET,
	/**
	 * Draw at the target. If no target is available, behaves the same as {@link #CASTER}
	 */
	TARGET_IF_AVAILABLE,

}
