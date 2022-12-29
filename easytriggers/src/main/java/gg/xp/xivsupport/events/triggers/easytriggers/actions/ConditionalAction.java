package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableActions;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableConditions;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableEventType;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SqAction;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ConditionalAction<X extends BaseEvent> implements SqAction<X>, HasMutableEventType {

	private static final Logger log = LoggerFactory.getLogger(ConditionalAction.class);

	@JsonProperty
	public List<Condition<? super X>> conditions = Collections.emptyList();
	@JsonProperty
	public List<Action<? super X>> trueActions = Collections.emptyList();
	@JsonProperty
	public List<Action<? super X>> falseActions = Collections.emptyList();
	@JsonProperty
	public Class<X> eventType = (Class<X>) BaseEvent.class;

	@Override
	public void accept(SequentialTriggerController<X> stc, EasyTriggerContext context, X event) {
		if (!eventType.isInstance(event)) {
			return;
		}
		if (conditions.stream().allMatch(cond -> cond.test(context, event))) {
			context.runActions(trueActions, stc, event);
		}
		else {
			context.runActions(falseActions, stc, event);
		}

	}

	@Override
	public void recalc() {
		makeWritable();
		conditions.sort(Comparator.comparing(Condition::sortOrder));
		conditions.forEach(Condition::recalc);
		trueActions.forEach(Action::recalc);
		falseActions.forEach(Action::recalc);
	}

	private void makeWritable() {
		conditions = new ArrayList<>(conditions);
		trueActions = new ArrayList<>(trueActions);
		falseActions = new ArrayList<>(falseActions);
	}

	@Override
	public void accept(EasyTriggerContext context, X event) {
		// TODO: this method is useless for these
	}

	@Override
	public String fixedLabel() {
		return "Conditional";
	}

	@Override
	public String dynamicLabel() {
		// TODO: figure out best label
		return "Conditional Action";
	}

	@Override
	public Class<X> getEventType() {
		return eventType;
	}

	@Override
	public void setEventType(Class<?> eventType) {
		log.info("Event type {} -> {}", this.eventType, eventType);
		this.eventType = (Class<X>) eventType;
		Stream.of(conditions, trueActions, falseActions)
				.flatMap(Collection::stream)
				.forEach(item -> {
					if (item instanceof HasMutableEventType het) {
						het.setEventType(eventType);
					}
				});
	}

	public HasMutableActions<X> trueActionsController() {
		return new HasMutableActions<X>() {
			@Override
			public List<Action<? super X>> getActions() {
				return trueActions;
			}

			@Override
			public void setActions(List<Action<? super X>> actions) {
				trueActions = actions;
			}

			@Override
			public void addAction(Action<? super X> action) {
				makeWritable();
				trueActions.add(action);
			}

			@Override
			public void addAction(Action<? super X> action, int index) {
				makeWritable();
				trueActions.add(index, action);

			}

			@Override
			public void removeAction(Action<? super X> action) {
				makeWritable();
				trueActions.remove(action);
			}

			@Override
			public Class<X> classForActions() {
				return eventType;
			}
		};
	}

	public HasMutableActions<X> falseActionsController() {
		return new HasMutableActions<X>() {
			@Override
			public List<Action<? super X>> getActions() {
				return falseActions;
			}

			@Override
			public void setActions(List<Action<? super X>> actions) {
				falseActions = actions;
			}

			@Override
			public void addAction(Action<? super X> action) {
				makeWritable();
				falseActions.add(action);
			}

			@Override
			public void addAction(Action<? super X> action, int index) {
				makeWritable();
				falseActions.add(index, action);

			}

			@Override
			public void removeAction(Action<? super X> action) {
				makeWritable();
				falseActions.remove(action);
			}

			@Override
			public Class<X> classForActions() {
				return eventType;
			}
		};
	}

	public HasMutableConditions<X> conditionsController() {
		return new HasMutableConditions<X>() {
			@Override
			public void setConditions(List<Condition<? super X>> conditions) {
				makeWritable();
				ConditionalAction.this.conditions = conditions;
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
