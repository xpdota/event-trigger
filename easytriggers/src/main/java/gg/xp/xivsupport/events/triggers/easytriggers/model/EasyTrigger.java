package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EasyTrigger<X> implements HasMutableConditions<X>, HasMutableActions<X> {

	private static final Logger log = LoggerFactory.getLogger(EasyTrigger.class);

	@JsonIgnore
	private long misses;
	@JsonIgnore
	private long hits;

	@JsonProperty(defaultValue = "true")
	private boolean enabled = true;

	private Class<X> eventType = (Class<X>) Event.class;
	private List<Condition<? super X>> conditions = Collections.emptyList();
	private List<Action<? super X>> actions = Collections.emptyList();
	private String name = "Give me a name";
	private int timeoutMs = 60_000;

	private SequentialTrigger<BaseEvent> sq = SqtTemplates.nothing();
	private EasyTriggerContext ctx;

	public EasyTrigger() {
		recalc();
	}

	public void handleEvent(EventContext context, Event event) {
		if (!(event instanceof BaseEvent)) {
			return;
		}
		if (sq.isActive()) {
			sq.feed(context, (BaseEvent) event);
		}
		if (!enabled || eventType == null || !eventType.isInstance(event)) {
			return;
		}
		X typedEvent = eventType.cast(event);
		ctx = new EasyTriggerContext(context);
		if (conditions.stream().allMatch(cond -> cond.test(ctx, typedEvent))) {
			hits++;
			sq.feed(context, (BaseEvent) event);
		}
		else {
			misses++;
		}
	}

	public void recalc() {
		makeWritable();
		conditions.sort(Comparator.comparing(Condition::sortOrder));
		conditions.forEach(Condition::recalc);
		actions.forEach(Action::recalc);
		sq = SqtTemplates.sq(timeoutMs,
				eventType,
				// The start condition is handled externally
				se -> true,
				(e1, s) -> {
					for (Action<? super X> action : actions) {

						log.info("Action: {}", action);
						ctx.setAcceptHook(s::accept);
						ctx.setEnqueueHook(s::enqueue);

						try {
							if (action instanceof SqAction sqa) {
								sqa.accept(s, ctx, (BaseEvent) e1);
							}
							else {
								action.accept(ctx, e1);
							}
						} catch (Throwable t) {
							if (s.isDone()) {
								return;
							}
							else {
								log.error("Error in trigger '{}' action '{}'", name, action.dynamicLabel(), t);
							}
						}

						if (ctx.shouldStopProcessing()) {
							return;
						}
					}
				});
	}

	public Class<X> getEventType() {
		return eventType;
	}

	public void setEventType(Class<X> eventType) {
		this.eventType = eventType;
	}

	@Override
	public List<Condition<? super X>> getConditions() {
		return Collections.unmodifiableList(conditions);
	}

	@Override
	public void setConditions(List<Condition<? super X>> conditions) {
		this.conditions = new ArrayList<>(conditions);
	}

	private void makeWritable() {
		if (!(conditions instanceof ArrayList)) {
			conditions = new ArrayList<>(conditions);
		}
		if (!(actions instanceof ArrayList)) {
			actions = new ArrayList<>(actions);
		}
	}

	@Override
	public Class<X> classForConditions() {
		return eventType;
	}

	@Override
	public Class<X> classForActions() {
		return eventType;
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
	public List<Action<? super X>> getActions() {
		return Collections.unmodifiableList(actions);
	}

	@Override
	public void setActions(List<Action<? super X>> actions) {
		this.actions = new ArrayList<>(actions);
	}

	@Override
	public void addAction(Action<? super X> action) {
		makeWritable();
		actions.add(action);
	}

	@Override
	public void removeAction(Action<? super X> action) {
		makeWritable();
		actions.remove(action);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getHits() {
		return hits;
	}

	public long getMisses() {
		return misses;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		recalc();
	}


	// TODO: this doesn't work right anyway, because it doesn't do a deep clone of conditions
//	public EasyTrigger<X> duplicate() {
//		EasyTrigger<X> newTrigger = new EasyTrigger<>();
//		newTrigger.setEventType(eventType);
//		newTrigger.setName(name + " copy");
//		newTrigger.setTts(tts);
//		newTrigger.setText(text);
//		newTrigger.setConditions(new ArrayList<>(conditions));
//		return newTrigger;
//	}
}
