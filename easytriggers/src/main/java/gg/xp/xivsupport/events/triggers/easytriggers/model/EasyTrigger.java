package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerConcurrencyMode;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EasyTrigger<X> implements HasMutableConditions<X>, HasMutableActions<X> {

	private static final Logger log = LoggerFactory.getLogger(EasyTrigger.class);

	@JsonIgnore
	private long misses;
	@JsonIgnore
	private long hits;

	@JsonProperty(defaultValue = "true")
	private boolean enabled = true;

	@JsonProperty
	private SequentialTriggerConcurrencyMode concurrency = SequentialTriggerConcurrencyMode.BLOCK_NEW;

	private Class<X> eventType = (Class<X>) Event.class;
	private List<Condition<? super X>> conditions = Collections.emptyList();
	private List<Action<? super X>> actions = Collections.emptyList();
	private String name = "Give me a name";
	private int timeoutMs = 600_000;

	private SequentialTrigger<BaseEvent> sqBase = SqtTemplates.nothing();
	private EasyTriggerContext ctx;

	public EasyTrigger() {
		// TODO: unit test for updating trigger while running
		recalcFully();
	}

	public void handleEvent(EventContext context, Event event) {
		if (!(event instanceof BaseEvent) || !enabled || eventType == null) {
			return;
		}
		ctx = new EasyTriggerContext(context, this);
		sqBase.feed(context, (BaseEvent) event);
	}

	private boolean matchesStartCondition(X event) {
		return conditions.stream().allMatch(cond -> cond.test(ctx, event));
	}

	private void recalcFully() {
		sqBase = SqtTemplates.sq(timeoutMs,
				eventType,
				// The start condition is handled externally
				event -> {
					X typedEvent = eventType.cast(event);
					if (matchesStartCondition(typedEvent)) {
						hits++;
						return true;
					}
					else {
						misses++;
						return false;
					}
				},
				(e1, s) -> {
					ctx.runActions((List) actions, s, (BaseEvent) e1);
				});
		recalc();
	}

	public void recalc() {
		makeWritable();
		conditions.sort(Comparator.comparing(Condition::sortOrder));
		conditions.forEach(Condition::recalc);
		actions.forEach(Action::recalc);
		sqBase.setConcurrency(concurrency);
		Stream.concat(conditions.stream(), actions.stream()).forEach(item -> {
			if (item instanceof HasMutableEventType het) {
				het.setEventType(getEventType());
			}
		});
	}

	@Override
	public Class<X> getEventType() {
		return eventType;
	}

	public void setEventType(Class<X> eventType) {
		this.eventType = eventType;
		recalcFully();
	}

	@Override
	public List<Condition<? super X>> getConditions() {
		return Collections.unmodifiableList(conditions);
	}

	@Override
	public void setConditions(List<Condition<? super X>> conditions) {
		this.conditions = new ArrayList<>(conditions);
		recalc();
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
		recalc();
	}

	@Override
	public void removeCondition(Condition<? super X> condition) {
		makeWritable();
		conditions.remove(condition);
		recalc();
	}

	public SequentialTriggerConcurrencyMode getConcurrency() {
		return concurrency;
	}

	public void setConcurrency(SequentialTriggerConcurrencyMode concurrency) {
		this.concurrency = concurrency;
		recalc();
	}

	@Override
	public List<Action<? super X>> getActions() {
		return Collections.unmodifiableList(actions);
	}

	@Override
	public void setActions(List<Action<? super X>> actions) {
		this.actions = new ArrayList<>(actions);
		recalc();
	}

	@Override
	public void addAction(Action<? super X> action) {
		makeWritable();
		actions.add(action);
		recalc();
	}

	@Override
	public void addAction(Action<? super X> action, int index) {
		makeWritable();
		actions.add(index, action);
		recalc();
	}

	@Override
	public void removeAction(Action<? super X> action) {
		makeWritable();
		actions.remove(action);
		recalc();
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
//	public void handleEventOld(EventContext context, Event event) {
//		if (!(event instanceof BaseEvent)) {
//			return;
//		}
//		switch (concurrency) {
//			// Mirrors standard sequential trigger logic
//			case BLOCK_NEW -> {
//				// Block new - if currently active, feed.
//				if (sqCurrent.isActive()) {
//					sqCurrent.feed(context, (BaseEvent) event);
//					// TODO: shouldn't there be a 'return' right here?
//					return;
//				}
//				if (!enabled || eventType == null || !eventType.isInstance(event)) {
//					return;
//				}
//				X typedEvent = eventType.cast(event);
//				ctx = new EasyTriggerContext(context, this);
//				if (matchesStartCondition(typedEvent)) {
//					hits++;
//					sqCurrent = sqBase;
//					sqCurrent.feed(context, (BaseEvent) event);
//				}
//				else {
//					misses++;
//				}
//			}
//			case REPLACE_OLD -> {
//				if (!enabled || eventType == null || !eventType.isInstance(event)) {
//					return;
//				}
//				X typedEvent = eventType.cast(event);
//				ctx = new EasyTriggerContext(context, this);
//				if (matchesStartCondition(typedEvent)) {
//					hits++;
//					if (sqCurrent != null && sqCurrent.isActive()) {
//						sqCurrent.stopSilently();
//					}
//					sqCurrent = sqBase;
//					sqCurrent.feed(context, (BaseEvent) event);
//				}
//				else {
//					if (sqCurrent.isActive()) {
//						sqCurrent.feed(context, (BaseEvent) event);
//					}
//					misses++;
//				}
//			}
//			case CONCURRENT -> {
//				X typedEvent = eventType.cast(event);
//				ctx = new EasyTriggerContext(context, this);
//				var iter = sqMultiCurrent.iterator();
//				while (iter.hasNext()) {
//					SequentialTrigger<BaseEvent> next = iter.next();
//					next.feed(context, (BaseEvent) event);
//					if (!next.isActive()) {
//						iter.remove();
//					}
//				}
//				if (matchesStartCondition(typedEvent)) {
//					hits++;
//					sqMultiCurrent.add()
//				}
//			}
//		}
//	}
}
