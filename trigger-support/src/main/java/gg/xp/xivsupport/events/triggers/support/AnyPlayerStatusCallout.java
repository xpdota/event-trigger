package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.scan.FeedHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Callout adapter for local player gaining a buff
 */
@FeedHelper(PlayerStatusAdapter.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface AnyPlayerStatusCallout {
	/**
	 * @return Which status IDs to trigger on
	 */
	long[] value();

	long suppressMs() default 0;
}
