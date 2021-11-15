package gg.xp.events.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.events.models.XivEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawXivPartyInfo extends XivEntity {
	private static final long serialVersionUID = 5313136766980667451L;
	private final int worldId;
	private final int jobId;
	private final int level;
	private final boolean inParty;

	public RawXivPartyInfo(
			@JsonProperty("id") String idRaw,
			@JsonProperty("name") String name,
			@JsonProperty("worldId") int worldId,
			@JsonProperty("job") int jobId,
			@JsonProperty("level") int level,
			@JsonProperty("inParty") boolean inParty
			) {
		super(Long.parseLong(idRaw, 16), name);
		this.worldId = worldId;
		this.jobId = jobId;
		this.level = level;
		this.inParty = inParty;
	}

	public int getWorldId() {
		return worldId;
	}

	public int getJobId() {
		return jobId;
	}

	public int getLevel() {
		return level;
	}

	public boolean isInParty() {
		return inParty;
	}
}
