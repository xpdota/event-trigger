package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EasyTriggerMigrationHelper {
	@JsonProperty
	public String tts;
	@JsonProperty
	public String text;
	@JsonProperty
	public @Nullable Integer colorRaw;
	@JsonProperty
	public long hangTime;
	@JsonProperty
	public boolean useIcon;
	@JsonProperty
	public boolean useDuration;
}
