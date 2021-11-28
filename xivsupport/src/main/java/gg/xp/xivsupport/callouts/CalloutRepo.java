package gg.xp.xivsupport.callouts;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CalloutRepo {
	String value();
}
