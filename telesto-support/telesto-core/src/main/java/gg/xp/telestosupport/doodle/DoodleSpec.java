package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.BaseEvent;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

public abstract class DoodleSpec implements Serializable {

	private static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);
	@Serial
	private static final long serialVersionUID = 4712819919444993909L;

	@JsonProperty("name")
	public @Nullable String name;
	@JsonIgnore
	public Color color = Color.WHITE;
	@JsonIgnore
	public Duration expiryTime = DEFAULT_DURATION;
	@JsonIgnore
	public BaseEvent timeBasis;
	@JsonIgnore
	public Supplier<Boolean> expiryCondition = () -> {
		if (timeBasis == null) {
			return false;
		}
		return timeBasis.getEffectiveTimeSince().compareTo(expiryTime) > 0;
	};

	@JsonProperty("type")
	public abstract String type();


	@JsonAnyGetter
	public Map<String, Double> convertColor() {
		return Map.of("r",
				color.getRed() / 255.0,
				"g",
				color.getGreen() / 255.0,
				"b",
				color.getBlue() / 255.0,
				"a",
				color.getAlpha() / 255.0);
	}

	@JsonProperty("expiresin")
	public long expiryInMs() {
		return expiryTime.toMillis();
	}

	public boolean isExpired() {
		return expiryCondition.get();
	}

	@JsonIgnore
	public @Nullable String getName() {
		return name;
	}
}
