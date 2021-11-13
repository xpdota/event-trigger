package gg.xp.events.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.events.models.HitPoints;
import gg.xp.events.models.Position;
import gg.xp.events.models.XivEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawXivCombatantInfo extends XivEntity {

	private static final long serialVersionUID = -4500158723610391407L;
	private final long id;
	private final String name;
	private final long jobId;
	private final long type;
	private final long curHp;
	private final long maxHp;
	private final long curMp;
	private final long maxMp;
	private final long level;
	private final double posX;
	private final double posY;
	private final double posZ;
	private final double heading;
	private final long worldId;
	private final String worldName;
	private final long bnpcId;
	private final long bnpcNameId;
	private final long partyType;
	private final long ownerId;

	public RawXivCombatantInfo(
			@JsonProperty("ID") long id,
			@JsonProperty("Name") String name,
			@JsonProperty("Job") long jobId,
			@JsonProperty("type") long type,
			@JsonProperty("CurrentHP") long curHp,
			@JsonProperty("MaxHP") long maxHp,
			@JsonProperty("CurrentMP") long curMp,
			@JsonProperty("MaxMP") long maxMp,
			@JsonProperty("Level") long level,
			@JsonProperty("PosX") double posX,
			@JsonProperty("PosY") double posY,
			@JsonProperty("PosZ") double posZ,
			@JsonProperty("Heading") double heading,
			@JsonProperty("WorldID") long worldId,
			@JsonProperty("WorldName") String worldName,
			@JsonProperty("BNpcID") long bnpcId,
			@JsonProperty("BNpcNameID") long bnpcNameId,
			@JsonProperty("PartyType") long partyType,
			@JsonProperty("OwnerID") long ownerId
	) {
		super(id, name);
		this.id = id;
		this.name = name;
		this.jobId = jobId;
		this.type = type;
		this.curHp = curHp;
		this.maxHp = maxHp;
		this.curMp = curMp;
		this.maxMp = maxMp;
		this.level = level;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		this.heading = heading;
		this.worldId = worldId;
		this.worldName = worldName;
		this.bnpcId = bnpcId;
		this.bnpcNameId = bnpcNameId;
		this.partyType = partyType;
		this.ownerId = ownerId;
	}

	public long getJobId() {
		return jobId;
	}

	public HitPoints getHP() {
		return new HitPoints(curHp, maxHp);
	}

	public Position getPos() {
		return new Position(posX, posY, posZ, heading);
	}

	public long getRawType() {
		return type;
	}

	public long getCurMp() {
		return curMp;
	}

	public long getMaxMp() {
		return maxMp;
	}

	public long getWorldId() {
		return worldId;
	}

	public String getWorldName() {
		return worldName;
	}

	public long getBnpcId() {
		return bnpcId;
	}

	public long getBnpcNameId() {
		return bnpcNameId;
	}

	public long getPartyType() {
		return partyType;
	}

	public long getOwnerId() {
		return ownerId;
	}

	public long getLevel() {
		return level;
	}

	@Override
	public String toString() {
		return "RawXivCombatantInfo{" +
				"id=" + id +
				", name='" + name + '\'' +
				", jobId=" + jobId +
				", type=" + type +
				", curHp=" + curHp +
				", maxHp=" + maxHp +
				", curMp=" + curMp +
				", maxMp=" + maxMp +
				", posX=" + posX +
				", posY=" + posY +
				", posZ=" + posZ +
				", heading=" + heading +
				", worldId=" + worldId +
				", worldName='" + worldName + '\'' +
				", bnpcId=" + bnpcId +
				", bnpcNameId='" + bnpcNameId + '\'' +
				", partyType=" + partyType +
				", ownerId=" + ownerId +
				'}';
	}
}
