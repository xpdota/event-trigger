package gg.xp.xivsupport.events.triggers.easytriggers.model;

public interface EventDescription<X> {
	Class<X> type();

	String description();

	String defaultTts();

	String defaultText();

	EasyTrigger<X> newInst();
}
