package gg.xp.xivdata.jobs;

import static gg.xp.xivdata.jobs.Job.AST;
import static gg.xp.xivdata.jobs.Job.BRD;
import static gg.xp.xivdata.jobs.Job.DNC;
import static gg.xp.xivdata.jobs.Job.DRG;
import static gg.xp.xivdata.jobs.Job.DRK;
import static gg.xp.xivdata.jobs.Job.GNB;
import static gg.xp.xivdata.jobs.Job.MCH;
import static gg.xp.xivdata.jobs.Job.MNK;
import static gg.xp.xivdata.jobs.Job.NIN;
import static gg.xp.xivdata.jobs.Job.PLD;
import static gg.xp.xivdata.jobs.Job.RDM;
import static gg.xp.xivdata.jobs.Job.SCH;
import static gg.xp.xivdata.jobs.Job.SMN;
import static gg.xp.xivdata.jobs.Job.WAR;
import static gg.xp.xivdata.jobs.Job.WHM;
import static gg.xp.xivdata.jobs.JobType.CASTER;
import static gg.xp.xivdata.jobs.JobType.MELEE_DPS;
import static gg.xp.xivdata.jobs.JobType.TANK;

public enum Cooldown {
	// List of ALL buffs to track - WL/BL will be done by user settings
	// JLS/javac being dumb, had to put the L there to make it a long
	Sentinel(PLD, 120.0, "Sentinel", CooldownType.PERSONAL_MIT, 0x11, 74),
	Cover(PLD, 120.0, "Cover", CooldownType.PERSONAL_MIT, 0x1b, 80),
	HallowedGround(PLD, 420.0, "Hallowed Ground", CooldownType.INVULN, 0x1e, 82),
	ThrillofBattle(WAR, 90.0, "Thrill of Battle", CooldownType.PERSONAL_MIT, 0x28, 87),
	Holmgang(WAR, 240.0, "Holmgang", CooldownType.INVULN, 0x2b, 409),
	Vengeance(WAR, 120.0, "Vengeance", CooldownType.PERSONAL_MIT, 0x2c, 89),
	// TODO: these do not have correct duration - need to fix
	MagesBallad(BRD, 120.0, "Mage's Ballad", CooldownType.PARTY_BUFF, 0x72, 0x8a9),
	ArmysPaeon(BRD, 120.0, "Army's Paeon", CooldownType.PARTY_BUFF, 0x74, 0x8aa),
	WanderersMinuet(BRD, 120.0, "Wanderer's Minuet", CooldownType.PARTY_BUFF, 0xde7, 0x8a8),
	BattleVoice(BRD, 120.0, "Battle Voice", CooldownType.PARTY_BUFF, 0x76, 0x8d),
	Benediction(WHM, 180.0, "Benediction", CooldownType.HEAL, 0x8c),
	SacredSoil(SCH, 30.0, "Sacred Soil", CooldownType.PARTY_MIT, 0xbc, 0x798),
	// TODO - check ability ID
	TrickAttack(NIN, 60.0, "Trick Attack", CooldownType.PARTY_BUFF, 0x8d2, 638),
	DivineVeil(PLD, 90.0, "Divine Veil", CooldownType.PARTY_MIT, 0xdd4, 726, 727),
	RawIntuition(WAR, 25.0, "Raw Intuition", CooldownType.PERSONAL_MIT, 0xddf, 735),
	BattleLitany(DRG, 120.0, "Battle Litany", CooldownType.PARTY_BUFF, 0xde5, 786),
	DarkMind(DRK, 60.0, "Dark Mind", CooldownType.PERSONAL_MIT, 0xe32, 746),
	ShadowWall(DRK, 120.0, "Shadow Wall", CooldownType.PERSONAL_MIT, 0xe34, 747),
	LivingDead(DRK, 300.0, "Living Dead", CooldownType.INVULN, 0xe36, 810),
	PassageofArms(PLD, 120.0, "Passage of Arms", CooldownType.PARTY_MIT, 0x1cd9, 1175),
	ShakeItOff(WAR, 90.0, "Shake It Off", CooldownType.PARTY_MIT, 0x1cdc, 1457),
	TheBlackestNight(DRK, 15.0, "The Blackest Night", CooldownType.PERSONAL_MIT, 0x1ce1, 0x49a),
	Brotherhood(MNK, 90.0, "Brotherhood", CooldownType.PARTY_BUFF, 0x1ce4, 1185),
	// TODO: devotion
	DragonSight(DRG, 120.0, "Dragon Sight", CooldownType.PARTY_BUFF, 0x1ce6, 1183, 1184),
	Troubadour(BRD, 120.0, "Troubadour", CooldownType.PARTY_MIT, 0x1ced, 1934),
	ChainStratagem(SCH, 120.0, "Chain Stratagem", CooldownType.PARTY_BUFF, 0x1d0c, 1221),
	Embolden(RDM, 120.0, "Embolden", CooldownType.PARTY_BUFF, 0x1d60, 1239, 1297),
	Rampart(TANK, 90.0, "Rampart", CooldownType.PERSONAL_MIT, 0x1d6b, 1191),
	Reprisal(TANK, 60.0, "Reprisal", CooldownType.PARTY_MIT, 0x1d6f, 1193, 2101),
	ArmsLength(TANK, 120.0, "Arm's Length", CooldownType.PERSONAL_UTILITY, 0x1d7c, 1209),
	Feint(MELEE_DPS, 90.0, "Feint", CooldownType.PARTY_MIT, 0x1d7d, 1195),
	Addle(CASTER, 90.0, "Addle", CooldownType.PARTY_MIT, 0x1d88, 1203),
	Swiftcast(CASTER, 60.0, "Swiftcast", CooldownType.PERSONAL_UTILITY, 0x1d89, 167, 1325),
	StandardStep(DNC, 30.0, "Standard Step", CooldownType.PARTY_BUFF, 0x3e7d, 1821, 2024),
	TechnicalStep(DNC, 120.0, "Technical Step", CooldownType.PARTY_BUFF, 0x3e7e, 1819, 2049),
	Devilment(DNC, 120.0, "Devilment", CooldownType.PARTY_BUFF, 0x3e8b, 1825),
	ShieldSamba(DNC, 120.0, "Shield Samba", CooldownType.PARTY_MIT, 0x3e8c, 1826),
	Camouflage(GNB, 90.0, "Camouflage", CooldownType.PERSONAL_MIT, 0x3f0c, 1832),
	Nebula(GNB, 120.0, "Nebula", CooldownType.PERSONAL_MIT, 0x3f14, 1834),
	Aurora(GNB, 60.0, "Aurora", CooldownType.HEAL, 0x3f17, 1835, 2065),
	Superbolide(GNB, 360.0, "Superbolide", CooldownType.INVULN, 0x3f18, 1836),
	HeartofLight(GNB, 90.0, "Heart of Light", CooldownType.PARTY_MIT, 0x3f20, 1839),
	HeartofStone(GNB, 25.0, "Heart of Stone", CooldownType.PARTY_MIT, 0x3f21, 1840),
	HeartofCorundum(GNB, 25.0, "Heart of Corundum", CooldownType.PARTY_MIT, 25758, 2683),
	NascentFlash(WAR, 25.0, "Nascent Flash", CooldownType.HEAL, 0x4050, 1857, 1858),
	DarkMissionary(DRK, 90.0, "Dark Missionary", CooldownType.PARTY_MIT, 0x4057, 1894),
	Divination(AST, 120.0, "Divination", CooldownType.PARTY_BUFF, 0x40a8, 1878),
	Tactician(MCH, 120.0, "Tactician", CooldownType.PARTY_MIT, 0x41f9, 1951, 2177),
	SearingLight(SMN, 120.0, "Searing Light", CooldownType.PARTY_BUFF, 25801, 2703);


	public enum CooldownType {
		PERSONAL_MIT,
		INVULN,
		PARTY_MIT,
		PERSONAL_BURST,
		PARTY_BUFF,
		PERSONAL_UTILITY,
		PARTY_UTILITY,
		HEAL
	}

	private final JobType jobType;
	private final Job job;
	private final double cooldown;
	private final CooldownType type;
	private final String label;
	private final long abilityId;
	private final long[] buffId;

	Cooldown(Job job, double cooldown, String label, CooldownType cooldownType, long abilityId, long... buffId) {
		this.job = job;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityId = abilityId;
		this.buffId = buffId;
	}

	Cooldown(JobType jobType, double cooldown, String label, CooldownType cooldownType, long abilityId, long... buffId) {
		this.cooldown = cooldown;
		this.job = null;
		this.jobType = jobType;
		this.type = cooldownType;
		this.label = label;
		this.abilityId = abilityId;
		this.buffId = buffId;
	}

	public Job getJob() {
		return job;
	}

	public JobType getJobType() {
		return jobType;
	}

	public String getLabel() {
		return label;
	}

	public boolean abilityIdMatches(long abilityId) {
		return this.abilityId == abilityId;
	}

	public boolean buffIdMatches(long buffId) {
		for (long thisBuffId : this.buffId) {
			if (thisBuffId == buffId) {
				return true;
			}
		}
		return false;
	}

	public double getCooldown() {
		return cooldown;
	}
}
