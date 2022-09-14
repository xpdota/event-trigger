package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.gui.util.ColorUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;
import java.util.UUID;

public class CalloutAction implements Action<Event>, GenericCalloutAction {
	private String tts = "The text that you want read out loud (or leave empty)";
	private String text = "The text that you want displayed (or leave empty). Supports Groovy expressions in curly braces.";
	private @Nullable Integer colorRaw;
	private long hangTime = 5000;
	private boolean useIcon = true;
	private ModifiableCallout<Event> call;
	// This will enable future expansion into remote triggers that can be auto-updated
	// while still allowing for local customizations of callouts.
	@JsonProperty
	private UUID uuid;

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		RawModifiedCallout<?> modified = call.getModified(event, context.getExtraVariables());
		Color color = getColor();
		if (color != null) {
			modified.setColorOverride(color);
		}
		context.getEventContext().accept(modified);

	}

	@Override
	public String fixedLabel() {
		return "Callout";
	}

	@Override
	public void recalc() {
		ModifiableCallout<Event> call = new ModifiableCallout<>("Easy Trigger Callout", tts, text, ModifiableCallout.expiresIn(Duration.ofMillis(hangTime)));
		if (useIcon) {
			call.autoIcon();
		}
		this.call = call;
		if (this.uuid == null) {
			uuid = UUID.randomUUID();
		}
	}

	@Override
	@JsonIgnore
	public @Nullable Color getColor() {
		Integer colorRaw = this.colorRaw;
		if (colorRaw == null) {
			return null;
		}
		return ColorUtils.intToColor(colorRaw);
	}

	@Override
	@JsonIgnore
	public void setColor(@Nullable Color color) {
		if (color == null) {
			colorRaw = null;
		}
		else {
			colorRaw = ColorUtils.colorToInt(color);
		}
	}

	@Override
	public String getTts() {
		return tts;
	}

	@Override
	public void setTts(String tts) {
		this.tts = tts;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public Integer getColorRaw() {
		return colorRaw;
	}

	@Override
	public void setColorRaw(Integer colorRaw) {
		this.colorRaw = colorRaw;
	}

	@Override
	public long getHangTime() {
		return hangTime;
	}

	@Override
	public void setHangTime(long hangTime) {
		this.hangTime = hangTime;
	}

	@Override
	public boolean isUseIcon() {
		return useIcon;
	}

	@Override
	public void setUseIcon(boolean useIcon) {
		this.useIcon = useIcon;
	}
}
