package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class TriggerFolder extends BaseTrigger<Object> implements HasChildTriggers {

	private static final Logger log = LoggerFactory.getLogger(TriggerFolder.class);

	private List<Condition<Object>> conditions = Collections.emptyList();
	private List<BaseTrigger<?>> children = Collections.emptyList();

	@Override
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
		if (!(children instanceof ArrayList)) {
			children = new ArrayList<>(children);
		}
	}

	@Override
	public void recalc() {
		makeWritable();
		conditions.sort(Comparator.comparing(Condition::sortOrder));
		conditions.forEach(Condition::recalc);
		children.forEach(BaseTrigger::recalc);
		Stream.concat(conditions.stream(), children.stream()).forEach(item -> {
			if (item instanceof HasMutableEventType het) {
				het.setEventType(getEventType());
			}
		});
	}

	@Override
	public List<BaseTrigger<?>> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public void setChildren(List<BaseTrigger<?>> triggers) {
		this.children = new ArrayList<>(triggers);
		recalc();
	}

	@Override
	public void addChild(BaseTrigger<?> trigger) {
		makeWritable();
		children.add(trigger);
		recalc();
	}

	@Override
	public void addChild(BaseTrigger<?> trigger, int index) {
		makeWritable();
		children.add(index, trigger);
		recalc();
	}

	@Override
	public void removeChild(BaseTrigger<?> child) {
		makeWritable();
		children.remove(child);
		recalc();
	}

	@Override
	protected void handleEventInternal(EventContext context, BaseEvent event, EasyTriggerContext ectx) {
		boolean matchesConditions = conditions.stream().allMatch(cond -> cond.test(ectx, event));
		if (!matchesConditions) {
			return;
		}

		for (BaseTrigger<?> child : this.children) {
			try {
				child.handleEvent(context, event);
			}
			catch (Throwable t) {
				log.error("Nested trigger threw error (parent {}, child {})", getName(), child.getName(), t);
			}
		}
	}
}
