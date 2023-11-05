package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.models.Position;

import javax.annotation.Nullable;

public interface DescribesCastLocation<X extends Event & HasSourceEntity & HasAbility> {

	/**
	 * @return The event being described by this additional data. If the event contains its own data, rather than
	 * being provided after-the-fact, then this should point to itself.
	 */
	X originalEvent();

	/**
	 * If the cast is targeted on a location (including an optional rotation), then it will be returned in this
	 * method. If it is only a rotation, or a non-location target, then this will be null.
	 *
	 * @return The position, else null.
	 */
	@Nullable
	Position getPos();

	/**
	 * If the cast is not targeted on a location, but is targeted in a direction from the caster, return that
	 * direction.
	 *
	 * @return The direction, else null.
	 */
	@Nullable
	Double getHeadingOnly();

}
