package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import org.jetbrains.annotations.Nullable;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type",
		defaultImpl = EasyTrigger.class
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = EasyTrigger.class, name = "trigger"),
		@JsonSubTypes.Type(value = TriggerFolder.class, name = "folder"),
})
public abstract sealed class BaseTrigger<X> implements HasMutableConditions<X> permits TriggerFolder, EasyTrigger, FailedDeserializationTrigger {

	@JsonProperty(defaultValue = "true")
	private boolean enabled = true;
	private String name = "Give me a name";

	public abstract void recalc();

	public boolean isEnabled() {
		return enabled;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		recalc();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void handleEvent(EventContext context, Event event) {
		if (event instanceof BaseEvent be && enabled && getEventType() != null) {
			EasyTriggerContext ectx = new EasyTriggerContext(context, this);
			handleEventInternal(context, be, ectx);
		}
	}

	protected abstract void handleEventInternal(EventContext context, BaseEvent event, EasyTriggerContext ectx);

	@JsonIgnore
	private @Nullable HasChildTriggers parent;

	@JsonIgnore
	public @Nullable HasChildTriggers getParent() {
		return this.parent;
	}

	@JsonIgnore
	public void setParent(HasChildTriggers parent) {
		this.parent = parent;
	}

	@JsonIgnore
	public boolean isDisabledByParent() {
		if (parent instanceof BaseTrigger<?> et) {
			return !et.isEnabled() || et.isDisabledByParent();
		}
		return false;
	}
}
