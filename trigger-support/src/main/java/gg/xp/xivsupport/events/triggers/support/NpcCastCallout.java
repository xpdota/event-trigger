package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.scan.FeedHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Callout adapter for enemy "starts casting" events.
 */
@FeedHelper(NpcCastAdapter.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface NpcCastCallout {
	/**
	 * @return Which cast IDs to trigger on
	 */
	long[] value();

	long suppressMs() default -1;

	/**
	 * @return Whether the callout should be removed if the cast stops
	 */
	boolean cancellable() default false;

	/**
	 * @return Whether the callout should only trigger on the player casting the spell
	 */
	boolean onYou() default false;
}
