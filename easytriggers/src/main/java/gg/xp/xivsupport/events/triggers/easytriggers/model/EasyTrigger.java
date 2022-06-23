package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.HasDuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EasyTrigger<X> implements HasMutableConditions<X> {

	@JsonIgnore
	private long misses;
	@JsonIgnore
	private long hits;

	@JsonProperty(defaultValue = "true")
	private boolean enabled = true;
	private ModifiableCallout<X> call;

	private Class<X> eventType = (Class<X>) Event.class;
	private List<Condition<? super X>> conditions = Collections.emptyList();
	// TODO: hangtime conditions
	private String name = "Give me a name";
	private String tts = "The text that you want read out loud (or leave empty)";
	private String text = "The text that you want displayed (or leave empty). Supports Groovy expressions in curly braces.";
	private long hangTime = 5000;
	private boolean useDuration = true;
	private boolean useIcon = true;

	public EasyTrigger() {
		recalc();
	}

	public void handleEvent(EventContext context, Event event) {
		if (enabled && eventType != null && eventType.isInstance(event)) {
			X typedEvent = eventType.cast(event);
			if (conditions.stream().allMatch(cond -> cond.test(typedEvent))) {
				context.accept(call.getModified(typedEvent));
				hits++;
			}
			else {
				misses++;
			}
		}
	}

	private void recalc() {
		Predicate<X> expiry;
		Duration hangTime = Duration.ofMillis(this.hangTime);
		if (useDuration && HasDuration.class.isAssignableFrom(eventType)) {
			//noinspection RedundantCast,unchecked
			expiry = (Predicate<X>) (Predicate<? extends HasDuration>) hd -> hd.getEstimatedTimeSinceExpiry().compareTo(hangTime) > 0;
		}
		else {
			expiry = ModifiableCallout.expiresIn(hangTime);
		}
		ModifiableCallout<X> call = new ModifiableCallout<>("Easy Trigger Callout", tts, text, expiry);
		if (useIcon) {
			call.autoIcon();
		}
		this.call = call;
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
	}

	@Override
	public Class<X> classForConditions() {
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

	public long getHangTime() {
		return hangTime;
	}

	public void setHangTime(long hangTime) {
		this.hangTime = hangTime;
		recalc();
	}

	public boolean isUseDuration() {
		return useDuration;
	}

	public void setUseDuration(boolean useDuration) {
		this.useDuration = useDuration;
		recalc();
	}

	public boolean isUseIcon() {
		return useIcon;
	}

	public void setUseIcon(boolean useIcon) {
		this.useIcon = useIcon;
		recalc();
	}

	public EasyTrigger<X> duplicate() {
		EasyTrigger<X> newTrigger = new EasyTrigger<>();
		newTrigger.setEventType(eventType);
		newTrigger.setName(name + " copy");
		newTrigger.setTts(tts);
		newTrigger.setText(text);
		newTrigger.setConditions(new ArrayList<>(conditions));
		return newTrigger;
	}
}
