package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface IdType {
	Class<?> value();
}
