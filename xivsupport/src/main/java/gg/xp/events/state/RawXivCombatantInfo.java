package gg.xp.events.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.events.models.XivEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawXivCombatantInfo extends XivEntity {

	private static final long serialVersionUID = -4500158723610391407L;
	private final int jobId;
	private final int type;
	private final long curHp;
	private final long maxHp;
	private final double posX;
	private final double posY;
	private final double posZ;
	private final double heading;

	public RawXivCombatantInfo(
			@JsonProperty("ID") long id,
			@JsonProperty("Name") String name,
			@JsonProperty("Job") int jobId,
			@JsonProperty("type") int type,
			@JsonProperty("CurrentHP") long curHp,
			@JsonProperty("MaxHP") long maxHp,
			@JsonProperty("PosX") double posX,
			@JsonProperty("PosY") double posY,
			@JsonProperty("PosZ") double posZ,
			@JsonProperty("Heading") double heading
	) {
		super(id, name);
		this.jobId = jobId;
		this.type = type;
		this.curHp = curHp;
		this.maxHp = maxHp;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.heading = heading;
	}

	public int getJobId() {
		return jobId;
	}

	public long getCurHp() {
		return curHp;
	}

	public long getMaxHp() {
		return maxHp;
	}

	public double getPosX() {
		return posX;
	}

	public double getPosY() {
		return posY;
	}

	public double getPosZ() {
		return posZ;
	}

	public double getHeading() {
		return heading;
	}

	public int getType() {
		return type;
	}

	@Override
	public String toString() {
		return "RawXivCombatantInfo{" +
				"id=" + getId() +
				", name=" + getName() +
				", jobId=" + jobId +
				", type=" + type +
				", curHp=" + curHp +
				", maxHp=" + maxHp +
				", posX=" + posX +
				", posY=" + posY +
				", posZ=" + posZ +
				", heading=" + heading +
				'}';
	}
}
