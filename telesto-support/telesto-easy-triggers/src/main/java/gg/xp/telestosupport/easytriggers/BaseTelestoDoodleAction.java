package gg.xp.telestosupport.easytriggers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.telestosupport.doodle.DoodleSpec;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableEventType;
import gg.xp.xivsupport.gui.util.ColorUtils;

import java.awt.*;
import java.time.Duration;

public abstract class BaseTelestoDoodleAction implements Action<Event>, HasMutableEventType {
	@JsonProperty("color")
	@Description("Color")
	@JsonIgnore
	public Color color = new Color(255, 0, 255, 192);
	@JsonProperty("name")
	@Description("Doodle Name")
	public String name = "";
	@JsonProperty("duration")
	@Description("Duration (ms)")
	public int duration = 10_000;

	private Class<?> eventType;

	protected void finishSpec(DoodleSpec spec, BaseEvent triggerEvent) {
		if (name != null && !name.isBlank()) {
			spec.name = name;
		}
		spec.color = color;
		spec.expiryTime = Duration.ofMillis(duration);
		spec.timeBasis = triggerEvent;
	}

	@Override
	public String fixedLabel() {
		return null;
	}

	@Override
	public Class<?> getEventType() {
		return eventType;
	}

	@Override
	public void setEventType(Class<?> eventType) {
		this.eventType = eventType;
	}

	@JsonProperty("color")
	public int getColorFlattened() {
		return ColorUtils.colorToInt(color);
	}

	@JsonProperty("color")
	public void setColorFlattened(int color) {
		this.color = ColorUtils.intToColor(color);
	}

}
