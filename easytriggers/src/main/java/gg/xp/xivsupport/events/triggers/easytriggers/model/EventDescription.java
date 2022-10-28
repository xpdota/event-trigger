package gg.xp.xivsupport.events.triggers.easytriggers.model;

import org.jetbrains.annotations.Nullable;

public interface EventDescription<X> {
	Class<X> type();

	String description();

	String defaultTts();

	String defaultText();

	EasyTrigger<X> newEmptyInst(@Nullable String callText);

	EasyTrigger<X> newDefaultInst();
}
