package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.callouts.ModifiableCallout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EasyTrigger<X> {

	private ModifiableCallout<X> call;

	private Class<X> eventType = (Class<X>) Event.class;
	private List<Condition<? super X>> conditions = Collections.emptyList();
	private String name = "Give me a name";
	private String tts = "The text that you want read out loud (or leave empty)";
	private String text = "The text that you want displayed (or leave empty). Supports Groovy expressions in curly braces.";

	public EasyTrigger() {
		recalc();
	}

	public void handleEvent(EventContext context, Event event) {
		if (eventType != null && eventType.isInstance(event)) {
			X typedEvent = eventType.cast(event);
			if (conditions.stream().allMatch(cond -> cond.test(typedEvent))) {
				context.accept(call.getModified(typedEvent));
			}
		}
	}

	private void recalc() {
		call = new ModifiableCallout<>("Easy Trigger Callout", tts, text, Collections.emptyList());
	}

	public Class<X> getEventType() {
		return eventType;
	}

	public void setEventType(Class<X> eventType) {
		this.eventType = eventType;
	}

	public List<Condition<? super X>> getConditions() {
		return Collections.unmodifiableList(conditions);
	}

	public void setConditions(List<Condition<? super X>> conditions) {
		this.conditions = new ArrayList<>(conditions);
	}

	private void makeWritable() {
		if (!(conditions instanceof ArrayList)) {
			conditions = new ArrayList<>(conditions);
		}
	}

	public void addCondition(Condition<? super X> condition) {
		makeWritable();
		conditions.add(condition);
	}

	public void removeCondition(Condition<? super X> condition) {
		makeWritable();
		conditions.remove(condition);
	}

	public String getTts() {
		return tts;
	}

	public void setTts(String tts) {
		this.tts = tts;
		recalc();
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		recalc();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EasyTrigger<X> duplicate() {
		EasyTrigger<X> newTrigger = new EasyTrigger<>();
		newTrigger.setEventType(eventType);
		newTrigger.setName(name + " copy");
		newTrigger.setTts(tts);
		newTrigger.setTts(text);
		newTrigger.setConditions(new ArrayList<>(conditions));
		return newTrigger;
	}
}
