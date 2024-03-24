package gg.xp.xivsupport.events.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class RawXivCombatantInfo implements Serializable {

	@Serial
	private static final long serialVersionUID = -4500158723610391407L;
	private final long id;
	private final String name;
	private final long jobId;
	private final long type;
	private final HitPoints hp;
	private final ManaPoints mp;
	private final int level;
	private final Position pos;
	private final long worldId;
	private final String worldName;
	private final int bnpcId;
	private final int bnpcNameId;
	private final int partyType;
	private final long ownerId;
	private final short transformationId;
	private final short weaponId;
	private final float radius;
	private final long targetId;

	public RawXivCombatantInfo(
			long id,
			String name,
			long jobId,
			long type,
			long curHp,
			long maxHp,
			long curMp,
			long maxMp,
			long level,
			double posX,
			double posY,
			double posZ,
			double heading,
			long worldId,
			String worldName,
			long bnpcId,
			long bnpcNameId,
			long partyType,
			long ownerId
	) {
		this(id, name, jobId, type, curHp, maxHp, curMp, maxMp, level, posX, posY, posZ, heading, worldId, worldName, bnpcId, bnpcNameId, partyType, ownerId, (short) -1, (short) -1, 0.0f, 0);
	}

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
			@JsonProperty("OwnerID") long ownerId,
			@JsonProperty("TransformationId") Short transformationId,
			@JsonProperty("WeaponId") Short weaponId,
			@JsonProperty("Radius") float radius,
			@JsonProperty("TargetID") long targetId

	) {
		this.id = id;
		// Reuse strings
		this.name = name != null ? name.intern() : null;
		this.jobId = jobId;
		this.type = type;
		this.hp = new HitPoints(curHp, maxHp);
		this.mp = ManaPoints.of(curMp, maxMp);
		this.level = (int) level;
		this.pos = new Position(posX, posY, posZ, heading);
		this.worldId = worldId;
		// new OP behavior
		if (worldName == null || "outofrange2".equals(worldName)) {
			this.worldName = null;
		}
		else {
			this.worldName = worldName.intern();
		}
		this.bnpcId = (int) bnpcId;
		this.bnpcNameId = (int) bnpcNameId;
		this.partyType = (int) partyType;
		this.ownerId = ownerId;
		this.transformationId = transformationId == null ? -1 : transformationId;
		this.weaponId = weaponId == null ? -1 : weaponId;
		this.radius = radius;
		this.targetId = targetId;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getJobId() {
		return jobId;
	}

	public HitPoints getHP() {
		return hp;
	}

	public ManaPoints getMP() {
		return mp;
	}

	public Position getPos() {
		return pos;
	}

	public long getRawType() {
		return type;
	}

	public long getCurMp() {
		return mp.current();
	}

	public long getMaxMp() {
		return mp.max();
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

	public short getTransformationId() {
		return transformationId;
	}

	public short getWeaponId() {
		return weaponId;
	}

	public float getRadius() {
		return radius;
	}

	public long getTargetId() {
		return targetId;
	}

	@Override
	public String toString() {
		return "RawXivCombatantInfo{" +
		       "id=" + id +
		       ", name='" + name + '\'' +
		       ", jobId=" + jobId +
		       ", type=" + type +
		       ", hp=" + hp +
		       ", mp=" + mp +
		       ", pos=" + pos +
		       ", worldId=" + worldId +
		       ", worldName='" + worldName + '\'' +
		       ", bnpcId=" + bnpcId +
		       ", bnpcNameId='" + bnpcNameId + '\'' +
		       ", partyType=" + partyType +
		       ", ownerId=" + ownerId +
		       ", tfId=" + transformationId +
		       ", wepId=" + weaponId +
		       ", radius=" + radius +
		       ", targetId=" + targetId +
		       '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RawXivCombatantInfo that = (RawXivCombatantInfo) o;
		return id == that.id
		       && jobId == that.jobId
		       && type == that.type
		       && Objects.equals(this.hp, that.hp)
		       && Objects.equals(this.mp, that.mp)
		       && Objects.equals(this.pos, that.pos)
		       && level == that.level
		       && worldId == that.worldId
		       && bnpcId == that.bnpcId
		       && bnpcNameId == that.bnpcNameId
		       && partyType == that.partyType
		       && ownerId == that.ownerId
		       && Objects.equals(name, that.name)
		       && Objects.equals(worldName, that.worldName)
		       && Objects.equals(transformationId, that.transformationId)
		       && Objects.equals(weaponId, that.weaponId)
		       && Objects.equals(targetId, that.targetId)
		       && Objects.equals(radius, that.radius);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, jobId, type, hp, mp, level, pos, worldId, worldName, bnpcId, bnpcNameId, partyType, ownerId, transformationId, weaponId, radius, targetId);
	}
}
