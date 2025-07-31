package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Condition<X> {
	@SuppressWarnings("unused")
	boolean test(EasyTriggerContext context, X event);

	@Nullable String fixedLabel();

	String dynamicLabel();

	default void recalc() {
	}

	default int sortOrder() {
		return 0;
	}

	// TODO: make sure this doesn't get serialized - does JsonIgnore propagate?
	// It seems to
	@JsonIgnore
	Class<X> getEventType();
}
