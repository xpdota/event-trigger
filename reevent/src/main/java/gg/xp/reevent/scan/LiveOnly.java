package gg.xp.reevent.scan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a particular handler is purely an output, and should be
 * excluded from both replays and tests. i.e. we only want them to be used
 * when running live.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LiveOnly {
}
