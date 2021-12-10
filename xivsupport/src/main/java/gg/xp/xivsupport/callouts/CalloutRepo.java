package gg.xp.xivsupport.callouts;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When a class is annotated with this, and it would otherwise be added to the container (e.g. by having @HandleEvents
 * or @ScanMe, or being manually added), then any fields of type {@link ModifiableCallout} will be recognized as a
 * callout that can be modified or disabled via user settings.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CalloutRepo {
	String value();
}
