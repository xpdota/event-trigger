package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;

/**
 * Represents something with a target entity. For example, the target of an ability, or the entity receiving a buff.
 */
public interface HasTargetEntity {

	/**
	 * @return The target of this action. Note that this is a snapshot of the target at the point in time when the
	 * action occurred. To see the current data, use {@link XivState#getLatestCombatantData(XivCombatant)}.
	 */
	XivCombatant getTarget();

}
