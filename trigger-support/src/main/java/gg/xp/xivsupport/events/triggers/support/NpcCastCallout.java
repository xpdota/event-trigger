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
}
