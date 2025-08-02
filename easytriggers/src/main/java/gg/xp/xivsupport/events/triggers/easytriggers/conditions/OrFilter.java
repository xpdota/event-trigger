package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableConditions;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableEventType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrFilter<X> implements Condition<X>, HasMutableEventType {

	private static final Logger log = LoggerFactory.getLogger(OrFilter.class);

	@JsonProperty
	public List<Condition<? super X>> conditions = Collections.emptyList();
	@JsonProperty
	public Class<X> eventType = (Class<X>) BaseEvent.class;

	@Override
	public boolean test(EasyTriggerContext context, X event) {
		return conditions.stream().anyMatch(condition -> condition.test(context, event));
	}

	@Override
	@JsonInclude
	public Class<X> getEventType() {
		return eventType;
	}

	private void makeWritable() {
		conditions = new ArrayList<>(conditions);
	}

	@Override
	public void setEventType(Class<?> eventType) {
		log.trace("Event type {} -> {}", this.eventType, eventType);
		this.eventType = (Class<X>) eventType;
		Stream.of(conditions)
				.flatMap(Collection::stream)
				.forEach(item -> {
					if (item instanceof HasMutableEventType het) {
						het.setEventType(eventType);
					}
				});
	}

	@Override
	public @Nullable String fixedLabel() {
		return "Any of: ";
	}

	@Override
	public String dynamicLabel() {
		return "Any Of: " + conditions.stream()
				.map(Condition::dynamicLabel)
				.collect(Collectors.joining(", "));
	}

	public HasMutableConditions<X> conditionsController() {
		return new HasMutableConditions<>() {
			@Override
			public void setConditions(List<Condition<? super X>> conditions) {
				OrFilter.this.conditions = conditions;
			}

			@Override
			public void addCondition(Condition<? super X> condition) {
				makeWritable();
				conditions.add(condition);
			}

			@Override
			public void removeCondition(Condition<? super X> condition) {
				makeWritable();
				conditions.remove(condition);
			}

			@Override
			public Class<X> classForConditions() {
				return eventType;
			}

			@Override
			public List<Condition<? super X>> getConditions() {
				return new ArrayList<>(conditions);
			}
		};
	}
}
