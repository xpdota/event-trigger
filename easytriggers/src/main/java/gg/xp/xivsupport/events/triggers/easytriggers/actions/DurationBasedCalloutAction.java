package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.gui.util.ColorUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

public class DurationBasedCalloutAction implements Action<HasDuration>, GenericCalloutAction {
	private String tts = "TTS Text Here";
	private String text = "On-screen text here {event.estimatedRemainingDuration}";
	private @Nullable Integer colorRaw;
	private boolean plusDuration = true;
	private long hangTime = 5000;
	private boolean useIcon = true;
	private ModifiableCallout<HasDuration> call;
	@JsonProperty
	private UUID uuid;

	@Override
	public void accept(EasyTriggerContext context, HasDuration event) {
		RawModifiedCallout<?> modified = call.getModified(event, context.getExtraVariables());
		Color color = getColor();
		if (color != null) {
			modified.setColorOverride(color);
		}
		context.accept(modified);
	}

	@Override
	public String fixedLabel() {
		return null;
	}

	@Override
	public String dynamicLabel() {
		String displayedCall;
		if (tts != null && !tts.isBlank()) {
			displayedCall = tts;
		}
		else if (text != null && !text.isBlank()) {
			displayedCall = text;
		}
		else {
			return "Call <nothing>";
		}
		return String.format("Call '%s'", displayedCall);
	}

	@Override
	public void recalc() {
		Duration duration = Duration.ofMillis(hangTime);
		Predicate<HasDuration> expiryPredicate = plusDuration ? ModifiableCallout.durationExpiryPlusLingerTime(duration) : ModifiableCallout.expiresIn(duration);
		ModifiableCallout<HasDuration> call = new ModifiableCallout<>("Easy Trigger Callout", tts, text, expiryPredicate);
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
		recalc();
	}


	@Override
	public String getText() {
		return text;
	}


	@Override
	public void setText(String text) {
		this.text = text;
		recalc();
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

	@Override
	public Integer getColorRaw() {
		return colorRaw;
	}

	@Override
	public void setColorRaw(Integer colorRaw) {
		this.colorRaw = colorRaw;
	}

	public boolean isPlusDuration() {
		return plusDuration;
	}

	public void setPlusDuration(boolean plusDuration) {
		this.plusDuration = plusDuration;
	}
}
