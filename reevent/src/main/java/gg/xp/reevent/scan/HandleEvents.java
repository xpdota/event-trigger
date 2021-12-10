package gg.xp.reevent.scan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation tells {@link AutoHandlerScan} that this is method should be used to handle events.
 * <p>
 * The class will automatically be instantiated and added to the container. One instance of the class will be created
 * and will be retained for the life of the container.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleEvents {
	int order() default 0;

	String name() default "";
}
