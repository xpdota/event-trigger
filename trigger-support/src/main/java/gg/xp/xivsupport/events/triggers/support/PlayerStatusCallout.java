package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.scan.FeedHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Callout adapter for local player gaining a buff
 */
@FeedHelper(PlayerStatusAdapter.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlayerStatusCallout {
	/**
	 * @return Which status IDs to trigger on
	 */
	long[] value();
}
