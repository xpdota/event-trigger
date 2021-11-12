package gg.xp.events.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gg.xp.events.models.XivEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawXivPartyInfo extends XivEntity {
	private static final long serialVersionUID = 5313136766980667451L;
	private final int worldId;
	private final int jobId;
	private final int level;

	public RawXivPartyInfo(
			long id,
			String name,
			int worldId,
			int jobId,
			int level
			) {
		super(id, name);
		this.worldId = worldId;
		this.jobId = jobId;
		this.level = level;
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
}
