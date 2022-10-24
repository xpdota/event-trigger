package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.scan.FeedHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@FeedHelper(NpcCastAdapter.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface NpcCastCallout {
	long[] value();
}
