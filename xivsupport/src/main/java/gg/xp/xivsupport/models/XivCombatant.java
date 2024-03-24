package gg.xp.xivsupport.models;


import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class XivCombatant extends XivEntity {

	@Serial
	private static final long serialVersionUID = -6395674063997151018L;
	private final boolean isPc;
	private final boolean isThePlayer;
	private final long rawType;
	private final @Nullable HitPoints hp;
	private final @Nullable ManaPoints mp;
	private final @Nullable Position pos;
	private final int bNpcId;
	private final int bNpcNameId;
	private final int partyType;
	private final int level;
	private final long ownerId;
	private final short weaponId;
	private final float radius;
	private boolean isFake;
	private @Nullable XivCombatant parent;
	private final int shieldAmount;
	private final short transformationId;
	private final long targetId;

	public XivCombatant(
			long id,
			String name,
			boolean isPc,
			boolean isThePlayer,
			long rawType,
			@Nullable HitPoints hp,
			@Nullable ManaPoints mp,
			@Nullable Position pos,
			long bNpcId,
			long bNpcNameId,
			long partyType,
			long level,
			long ownerId,
			long shieldAmount,
			short transformationId,
			short weaponId,
			float radius, long targetId) {
		super(id, name);
		this.isPc = isPc;
		this.isThePlayer = isThePlayer;
		this.rawType = rawType;
		this.hp = hp;
		this.mp = mp;
		this.pos = pos;
		this.bNpcId = (int) bNpcId;
		this.bNpcNameId = (int) bNpcNameId;
		this.partyType = (int) partyType;
		this.level = (int) level;
		this.ownerId = ownerId;
		this.shieldAmount = (int) shieldAmount;
		this.transformationId = transformationId;
		this.weaponId = weaponId;
		this.radius = radius;
		this.targetId = targetId;
	}

	public XivCombatant(
			long id,
			String name,
			boolean isPc,
			boolean isThePlayer,
			long rawType,
			@Nullable HitPoints hp,
			@Nullable ManaPoints mp,
			@Nullable Position pos,
			long bNpcId,
			long bNpcNameId,
			long partyType,
			long level,
			long ownerId,
			long shieldAmount) {
		this(id, name, isPc, isThePlayer, rawType, hp, mp, pos, bNpcId, bNpcNameId, partyType, level, ownerId, shieldAmount, (short) -1, (short) -1, (short) 0, 0);
	}

	/**
	 * Simplified ctor for entity lookups that miss
	 *
	 * @param id numerical ID
	 * @param name human-readable name
	 */
	public XivCombatant(long id, String name) {
		this(id, name, false, false, 0, null, null, null, 0, 0, 0, 0, 0, 0);
	}


	public boolean isPc() {
		return isPc;
	}

	public boolean isThePlayer() {
		return isThePlayer;
	}

	@Override
	public String toString() {
		if (isEnvironment()) {
			return super.toString();
		}
		String npcInfo = bNpcId == 0 ? "" : String.format(" NPC %s:%s", getbNpcId(), getbNpcNameId());
		return String.format("XivCombatant(0x%X:%s:%s at %s%s)", getId(), getName(), getType(), getPos(), npcInfo);
	}

	// TODO: replace the others with this
	public CombatantType getType() {
		if (isPc()) {
			return CombatantType.PC;
		}
		else if (isFake) {
			return CombatantType.FAKE;
		}
		else if (rawType == 6) {
			return CombatantType.GP;
		}
		else if (rawType == 2) {
			if (parent != null && parent.isPc()) {
				return CombatantType.PET;
			}
			return CombatantType.NPC;
		}
		else if (rawType == 3) {
			return CombatantType.NONCOM;
		}
		else {
			return CombatantType.OTHER;
		}
	}

	/**
	 * Some of these are in OverlayPlugin EnmityMemoryCommon.cs
	 * 0 = ?
	 * 1 = PC
	 * 2 = Combatant NPCs and pets? Both Selene and Chocobo seem to be in here, as do enemies
	 * 3 = Non-combat NPC?
	 * 4 = Treasure coffer?
	 * 5 = Aetheryte?
	 * 6 = Gathering point? I got "Mature Tree" in here
	 * 7 = Gardening patch?
	 * 9 = Minion?
	 * 12 = Interactable housing instance?
	 *
	 * @return Raw type from ACT
	 */
	public long getRawType() {
		return rawType;
	}

	public @Nullable HitPoints getHp() {
		return isCombative() ? hp : null;
	}

	public @Nullable ManaPoints getMp() {
		return isCombative() ? mp : null;
	}

	public @Nullable Position getPos() {
		return pos;
	}

	public long getbNpcId() {
		return bNpcId;
	}

	public boolean npcIdMatches(long... expected) {
		for (long e : expected) {
			if (e == bNpcId) {
				return true;
			}
		}
		return false;
	}

	public long getbNpcNameId() {
		return bNpcNameId;
	}

	public long getPartyType() {
		return partyType;
	}

	public long getLevel() {
		return level;
	}

	public long getOwnerId() {
		return ownerId;
	}

	public boolean isFake() {
		return isFake;
	}

	public void setFake(boolean fake) {
		isFake = fake;
	}

	public @Nullable XivCombatant getParent() {
		return parent;
	}

	public float getRadius() {
		return radius;
	}

	public @NotNull XivCombatant walkParentChain() {
		if (parent == null) {
			return this;
		}
		else {
			return parent.walkParentChain();
		}
	}

	public void setParent(XivCombatant parent) {
		this.parent = parent;
	}

	public boolean isCombative() {
		CombatantType type = getType();
		return !(type == CombatantType.OTHER || type == CombatantType.NONCOM || type == CombatantType.GP);
	}

	public static final XivCombatant ENVIRONMENT
			= new XivCombatant(
			0xE0000000L,
			"ENVIRONMENT",
			false,
			false,
			0,
			null,
			null,
			null,
			0,
			0,
			0,
			0,
			0, 0);


	public long getShieldAmount() {
		return shieldAmount;
	}

	public short getTransformationId() {
		return transformationId;
	}

	public short getWeaponId() {
		return weaponId;
	}

	public long getTargetId() {
		return targetId;
	}

	public RawXivCombatantInfo toRaw() {
		HitPoints hp = getHp();
		if (hp == null) {
			hp = new HitPoints(50_000, 50_000);
		}
		Position pos = getPos();
		if (pos == null) {
			pos = new Position(100, 100, 100, 0.0);
		}
		ManaPoints mp = getMp();
		if (mp == null) {
			mp = new ManaPoints(10_000, 10_000);
		}
		return new RawXivCombatantInfo(getId(), getName(), 0, getRawType(), hp.current(), hp.max(), mp.current(), mp.max(), getLevel(), pos.x(), pos.y(), pos.z(), pos.heading(), 0, "TODO", getbNpcId(), getbNpcNameId(), getPartyType(), getOwnerId(), getTransformationId(), getWeaponId(), getRadius(), getTargetId());
	}

}
