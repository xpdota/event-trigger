package gg.xp.xivsupport.callouts.conversions;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class GlobalReplacement {

	@JsonProperty
	public @Nullable Pattern find;
	@JsonProperty
	public String replaceWith = "";
	@JsonProperty
	public boolean tts = true;
	@JsonProperty
	public boolean text = true;

}
