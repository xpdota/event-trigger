package gg.xp.xivsupport.events.actlines.events;

import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;

/**
 * Represents something with a source entity. For example, the caster of an ability, or whoever applied a buff.
 */
public interface HasSourceEntity {

	/**
	 * @return The source of this action. Note that this is a snapshot of the source at the point in time when the
	 * action occurred. To see the current data, use {@link XivState#getLatestCombatantData(XivCombatant)}.
	 */
	XivCombatant getSource();

}
