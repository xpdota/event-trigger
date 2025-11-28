package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("trigger")
public final class EasyTrigger<X> extends BaseTrigger<X> implements HasMutableActions<X> {

	private static final Logger log = LoggerFactory.getLogger(EasyTrigger.class);

	@JsonIgnore
	private long misses;
	@JsonIgnore
	private long hits;

	@JsonProperty
	private SequentialTriggerConcurrencyMode concurrency = SequentialTriggerConcurrencyMode.BLOCK_NEW;

	private Class<X> eventType = (Class<X>) Event.class;
	private List<Condition<? super X>> conditions = Collections.emptyList();
	private List<Action<? super X>> actions = Collections.emptyList();
	private int timeoutMs = 600_000;

	private SequentialTrigger<BaseEvent> sqBase = SqtTemplates.nothing();
	private EasyTriggerContext ctx;

	public EasyTrigger() {
		// TODO: unit test for updating trigger while running
		recalcFully();
	}

	@Override
	protected void handleEventInternal(EventContext context, BaseEvent event, EasyTriggerContext ectx) {
		this.ctx = ectx;
		sqBase.feed(context, event);
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

	@Override
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

	public long getHits() {
		return hits;
	}

	public long getMisses() {
		return misses;
	}

	@Override
	public String toString() {
		return "EasyTrigger<%s>(%s)".formatted(getEventType().getSimpleName(), getName());
	}
}
