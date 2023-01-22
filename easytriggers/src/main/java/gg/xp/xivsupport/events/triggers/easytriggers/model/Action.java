package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Action<X> {
	void accept(EasyTriggerContext context, X event);

	@Nullable String fixedLabel();

	String dynamicLabel();

	default void recalc() {
	}
}
