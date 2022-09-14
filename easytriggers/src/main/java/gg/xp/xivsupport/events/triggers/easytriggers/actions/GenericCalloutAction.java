package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.HasDuration;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

public interface GenericCalloutAction {


	@JsonIgnore
	@Nullable Color getColor();

	@JsonIgnore
	void setColor(@Nullable Color color);

	String getTts();

	void setTts(String tts);

	String getText();

	void setText(String text);

	long getHangTime();

	void setHangTime(long hangTime);

	boolean isUseIcon();

	void setUseIcon(boolean useIcon);

	Integer getColorRaw();

	void setColorRaw(Integer colorRaw);
}
