package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("folder")
public final class TriggerFolder extends BaseTrigger<Object> implements HasChildTriggers {

	private static final Logger log = LoggerFactory.getLogger(TriggerFolder.class);

	private List<Condition<Object>> conditions = Collections.emptyList();
	private List<BaseTrigger<?>> triggers = Collections.emptyList();

	@Override
	@JsonIgnore
	public Class<?> getEventType() {
		return Object.class;
	}

	@Override
	public List<Condition<? super Object>> getConditions() {
		return Collections.unmodifiableList(conditions);
	}

	@Override
	public void setConditions(List<Condition<? super Object>> conditions) {
		this.conditions = new ArrayList<>(conditions);
		recalc();
	}

	@Override
	public void addCondition(Condition<? super Object> condition) {
		makeWritable();
		conditions.add(condition);
		recalc();
	}

	@Override
	public void removeCondition(Condition<? super Object> condition) {
		makeWritable();
		conditions.remove(condition);
		recalc();
	}

	@Override
	public Class<Object> classForConditions() {
		return Object.class;
	}

	private void makeWritable() {
		if (!(conditions instanceof ArrayList)) {
			conditions = new ArrayList<>(conditions);
		}
		if (!(triggers instanceof ArrayList)) {
			triggers = new ArrayList<>(triggers);
		}
	}

	@Override
	public void recalc() {
		makeWritable();
		conditions.sort(Comparator.comparing(Condition::sortOrder));
		conditions.forEach(Condition::recalc);
		triggers.forEach(BaseTrigger::recalc);
		Stream.concat(conditions.stream(), triggers.stream()).forEach(item -> {
			if (item instanceof HasMutableEventType het) {
				het.setEventType(getEventType());
			}
		});
		triggers.forEach(trigger -> trigger.setParent(this));
	}

	@Override
	public List<BaseTrigger<?>> getChildTriggers() {
		return Collections.unmodifiableList(triggers);
	}

	@Override
	public void setChildTriggers(List<BaseTrigger<?>> triggers) {
		this.triggers = new ArrayList<>(triggers);
		recalc();
	}

	@Override
	public void addChildTrigger(BaseTrigger<?> trigger) {
		makeWritable();
		triggers.add(trigger);
		recalc();
	}

	@Override
	public void addChildTrigger(BaseTrigger<?> trigger, int index) {
		makeWritable();
		triggers.add(index, trigger);
		recalc();
	}

	@Override
	public void removeChildTriggers(BaseTrigger<?> child) {
		makeWritable();
		triggers.remove(child);
		recalc();
	}

	@Override
	protected void handleEventInternal(EventContext context, BaseEvent event, EasyTriggerContext ectx) {
		boolean matchesConditions = conditions.stream().allMatch(cond -> cond.test(ectx, event));
		if (!matchesConditions) {
			return;
		}

		for (BaseTrigger<?> child : this.triggers) {
			try {
				child.handleEvent(context, event);
			}
			catch (Throwable t) {
				log.error("Nested trigger threw error (parent {}, child {})", getName(), child.getName(), t);
			}
		}
	}

	@Override
	public String toString() {
		return "TriggerFolder(%s)".formatted(getName());
	}
}
