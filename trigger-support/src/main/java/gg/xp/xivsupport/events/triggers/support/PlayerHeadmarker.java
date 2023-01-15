package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.scan.FeedHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Cast adapter for a headmarker appearing on the player
 */
@FeedHelper(HeadmarkerAdapter.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlayerHeadmarker {
	/**
	 * @return The headmarker IDs to trigger on
	 */
	long[] value();

	/**
	 * @return False to use the absolute ID of the headmarker event, true to use the value relative
	 * to the first headmarker seen in the pull (for offset headmarkers).
	 */
	boolean offset() default false;
}
