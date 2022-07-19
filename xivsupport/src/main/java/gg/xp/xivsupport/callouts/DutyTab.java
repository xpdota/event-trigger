package gg.xp.xivsupport.callouts;

import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.gui.util.HasFriendlyName;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DutyTab {
	KnownDuty value();
}
