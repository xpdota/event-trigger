package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

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
