package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.gui.util.ColorUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EasyTrigger<X> implements HasMutableConditions<X>, HasMutableActions<X> {

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

	public EasyTrigger() {
		recalc();
	}

	public void handleEvent(EventContext context, Event event) {
		if (enabled && eventType != null && eventType.isInstance(event)) {
			X typedEvent = eventType.cast(event);
			EasyTriggerContext ctx = new EasyTriggerContext(context);
			if (conditions.stream().allMatch(cond -> cond.test(ctx, typedEvent))) {
				hits++;
				// TODO: to allow for delays, this could be a sequential trigger
				for (Action<? super X> action : actions) {
					action.accept(ctx, typedEvent);
					if (ctx.shouldStopProcessing()) {
						return;
					}
				}
			}
			else {
				misses++;
			}
		}
	}

	public void recalc() {
		makeWritable();
		conditions.sort(Comparator.comparing(Condition::sortOrder));
		conditions.forEach(Condition::recalc);
		actions.forEach(Action::recalc);
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
