package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.models.XivZone;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public record FflogsFight(
		XivZone zone,
		double fightPercentage,
		boolean kill,
		int id,
		long startTime,
		long endTime
) {

	public Duration duration() {
		return Duration.between(
				Instant.ofEpochMilli(startTime),
				Instant.ofEpochMilli(endTime)
		);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonCreator
	public static FflogsFight fromJson(
			@JsonProperty("gameZone") Map<String, Object> gameZone,
			@JsonProperty(value = "fightPercentage", defaultValue = "0") double fightPercentage,
			@JsonProperty("kill") boolean kill,
			@JsonProperty("id") int id,
			@JsonProperty("startTime") long startTime,
			@JsonProperty("endTime") long endTime
	) {
		double actualFightCompletion;
		if (kill) {
			actualFightCompletion = 1.0d;
		}
		else {
			actualFightCompletion = fightPercentage / 100.0;
		}
		return new FflogsFight(
				new XivZone(((Number) gameZone.get("id")).longValue(), (String) gameZone.get("name")),
				actualFightCompletion,
				kill,
				id,
				startTime,
				endTime
		);
	}
}
