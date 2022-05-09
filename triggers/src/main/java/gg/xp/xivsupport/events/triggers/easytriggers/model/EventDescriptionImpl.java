package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class EventDescriptionImpl<X> implements EventDescription<X> {
	private final Class<X> type;
	private final String description;
	private final String defaultText;
	private final String defaultTts;
	private final List<Supplier<Condition<? super X>>> defaultFilters;
	public EventDescriptionImpl(
			Class<X> type,
			String description,
			String defaultTextAndTts,
			List<Supplier<Condition<? super X>>> defaultFilters) {
		this(type, description, defaultTextAndTts, defaultTextAndTts, defaultFilters);
	}

	public EventDescriptionImpl(
			Class<X> type,
			String description,
			String defaultTts,
			String defaultText,
			List<Supplier<Condition<? super X>>> defaultFilters) {
		this.type = type;
		this.description = description;
		this.defaultText = defaultText;
		this.defaultTts = defaultTts;
		this.defaultFilters = defaultFilters;
	}

	@Override
	public Class<X> type() {
		return type;
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public String defaultText() {
		return defaultText;
	}

	@Override
	public String defaultTts() {
		return defaultTts;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EventDescriptionImpl<X> that = (EventDescriptionImpl<X>) o;
		return Objects.equals(type, that.type) && Objects.equals(description, that.description) && Objects.equals(defaultText, that.defaultText) && Objects.equals(defaultTts, that.defaultTts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, description, defaultText, defaultTts);
	}

	@Override
	public EasyTrigger<X> newEmptyInst() {
		EasyTrigger<X> easy = new EasyTrigger<>();
		easy.setEventType(type);
		easy.setTts(defaultTts);
		easy.setText(defaultText);
		return easy;
	}

	@Override
	public EasyTrigger<X> newDefaultInst() {
		EasyTrigger<X> easy = new EasyTrigger<>();
		easy.setEventType(type);
		easy.setTts(defaultTts);
		easy.setText(defaultText);
		defaultFilters.forEach(fp -> easy.addCondition(fp.get()));
		return easy;
	}

	/**
	 * Override this to specify default filters
	 *
	 * @param trigger
	 */
	public List<Supplier<? extends Condition<X>>> defaultFilters() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return "EventDescriptionImpl{" +
				"type=" + type +
				", description='" + description + '\'' +
				", defaultText='" + defaultText + '\'' +
				", defaultTts='" + defaultTts + '\'' +
				'}';
	}
}
