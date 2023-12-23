package gg.xp.reevent.scan;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Causes the annotated class to NOT be scanned by {@link AutoHandlerScan} even if it does have annotations
 * that would otherwise cause it to be scanned.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NoAutoScan {
}
