package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Action<X> {
	void accept(EasyTriggerContext context, X event);

	String fixedLabel();

	default void recalc() {
	}
}
