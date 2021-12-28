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
import static gg.xp.xivdata.jobs.Job.SGE;
import static gg.xp.xivdata.jobs.Job.SMN;
import static gg.xp.xivdata.jobs.Job.WAR;
import static gg.xp.xivdata.jobs.Job.WHM;
import static gg.xp.xivdata.jobs.JobType.CASTER;
import static gg.xp.xivdata.jobs.JobType.HEALER;
import static gg.xp.xivdata.jobs.JobType.MELEE_DPS;
import static gg.xp.xivdata.jobs.JobType.TANK;

public enum Cooldown {

	// DO NOT change enum member names - they are used as settings keys

	// List of ALL buffs to track - WL/BL will be done by user settings
	// TANKS
	Rampart(TANK, true, 90.0, "Rampart", CooldownType.PERSONAL_MIT, 0x1d6b, 1191),
	Reprisal(TANK, true, 60.0, "Reprisal", CooldownType.PARTY_MIT, 0x1d6f, 1193, 2101),
	ArmsLength(TANK, true, 120.0, "Arm's Length", CooldownType.PERSONAL_UTILITY, 0x1d7c, 1209),

	HallowedGround(PLD, true, 420.0, "Hallowed Ground", CooldownType.INVULN, 0x1e, 82),
	Sentinel(PLD, true, 120.0, "Sentinel", CooldownType.PERSONAL_MIT, 0x11, 74),
	Cover(PLD, true, 120.0, "Cover", CooldownType.PERSONAL_MIT, 0x1b, 80),
	// TODO: sheltron/holy sheltron
	FightOrFlight(PLD, true, 60.0, "Fight or Flight", CooldownType.PERSONAL_BURST, 0x14, 0x4c),
	Requiescat(PLD, true, 60.0, "Requiescat", CooldownType.PERSONAL_BURST, 0x1CD7, 0x558),
	DivineVeil(PLD, true, 90.0, "Divine Veil", CooldownType.PARTY_MIT, 0xdd4, 726, 727),
	PassageofArms(PLD, true, 120.0, "Passage of Arms", CooldownType.PARTY_MIT, 0x1cd9, 1175),
	Nebula(GNB, true, 120.0, "Nebula", CooldownType.PERSONAL_MIT, 0x3f14, 1834),
	Aurora(GNB, true, 60.0, "Aurora", CooldownType.HEAL, 0x3f17, 1835, 2065),
	Superbolide(GNB, true, 360.0, "Superbolide", CooldownType.INVULN, 0x3f18, 1836),
	HeartofLight(GNB, true, 90.0, "Heart of Light", CooldownType.PARTY_MIT, 0x3f20, 1839),
	HeartofStone(GNB, true, 25.0, "Heart of Stone", CooldownType.PARTY_MIT, 0x3f21, 1840),
	HeartofCorundum(GNB, true, 25.0, "Heart of Corundum", CooldownType.PARTY_MIT, 25758, 2683),
	Camouflage(GNB, true, 90.0, "Camouflage", CooldownType.PERSONAL_MIT, 0x3f0c, 1832),
//	NascentFlash(WAR, true, 25.0, "Nascent Flash", CooldownType.HEAL, 0x4050, 1857, 1858),
	ThrillofBattle(WAR, true, 90.0, "Thrill of Battle", CooldownType.PERSONAL_MIT, 0x28, 87),
	Holmgang(WAR, true, 240.0, "Holmgang", CooldownType.INVULN, 0x2b, 409),
	Vengeance(WAR, true, 120.0, "Vengeance", CooldownType.PERSONAL_MIT, 0x2c, 89),
	RawIntuition(WAR, true, 25.0, "Raw/Nascent/Bloodwhetting", CooldownType.PERSONAL_MIT, new long[]{0xddfL, 0x4050, 0x6497}, new long[]{735L, 1857, 1858, 0xA76L}),
	DarkMissionary(DRK, true, 90.0, "Dark Missionary", CooldownType.PARTY_MIT, 0x4057, 1894),
	DarkMind(DRK, true, 60.0, "Dark Mind", CooldownType.PERSONAL_MIT, 0xe32, 746),
	ShadowWall(DRK, true, 120.0, "Shadow Wall", CooldownType.PERSONAL_MIT, 0xe34, 747),
	LivingDead(DRK, true, 300.0, "Living Dead", CooldownType.INVULN, 0xe36, 810),
	ShakeItOff(WAR, true, 90.0, "Shake It Off", CooldownType.PARTY_MIT, 0x1cdc, 1457),
	TheBlackestNight(DRK, true, 15.0, "The Blackest Night", CooldownType.PERSONAL_MIT, 0x1ce1, 0x49a),

	// HEALERS
	LucidDreaming(HEALER, false, 60.0, "Lucid Dreaming", CooldownType.PERSONAL_UTILITY, 0x1D8A, 0x4B4),
	// TODO - check ability ID
	Benediction(WHM, true, 180.0, "Benediction", CooldownType.HEAL, 0x8c),
	Temperance(WHM, true, 120.0, "Temperance", CooldownType.PARTY_MIT, 0x4098, 0x750),
	Pom(WHM, true, 120.0, "Presence of Mind", CooldownType.PERSONAL_BURST, 0x88, 0x9d),
	Asylum(WHM, true, 45.0, "Asylum", CooldownType.HEAL, 0xDF1, 0x777),
	SacredSoil(SCH, true, 30.0, "Sacred Soil", CooldownType.PARTY_MIT, 0xbc, 0x798, 0x12A),
	// Summon order buffs:
	/*
		Fey Blessing: 0x7AE
		Fey Illumination: 0x7AC
		Fey Union: 0x7AD
		Whispering Dawn: 0x77B

		Consolation: 0x7AD
		Whisper: 0x77B
		Seraphic Illumination: 0x7AC
	 */
	WhisperingDawn(SCH, true, 60.0, "Whispering Dawn", CooldownType.HEAL, 0x4099, 0x13b, 0x752),
	FeyIllumination(SCH, true, 120.0, "Fey Blessing", CooldownType.PARTY_MIT, 0x409A, 0x13d, 0x753),
	FeyBlessing(SCH, true, 60.0, "Fey Illumination", CooldownType.HEAL, 0x409F),
	Expedient(SCH, true, 120.0, "Expedient", CooldownType.PARTY_UTILITY, 0x650C, 0xA97, 0xA98),
	ChainStratagem(SCH, true, 120.0, "Chain Stratagem", CooldownType.PARTY_BUFF, 0x1d0c, 1221),
	Protraction(SCH, true, 60.0, "Protraction", CooldownType.HEAL, 0x650b, 0xA96),
	Aetherflow(SCH, true, 60.0, "Aetherflow", CooldownType.PERSONAL_UTILITY, 0xa6),
	Recitation(SCH, true, 90.0, "Recitation", CooldownType.HEAL, 0x409E, 0x768),
	DeploymentTactics(SCH, false, 90.0, "Deployment Tactics", CooldownType.PARTY_MIT, 0xE01),
	EmergencyTactics(SCH, false, 15.0, "Emergency Tactics", CooldownType.PARTY_MIT, 0xE02, 0x318),
	// Sage stuff
	Krasis(SGE, true, 60.0, "Krasis", CooldownType.HEAL, 0x5EFD, 0xA3E),
	Pepsis(SGE, true, 30.0, "Pepsis", CooldownType.HEAL, 0x5EED),
	Rhizomata(SGE, true, 90.0, "Rhizomata", CooldownType.PERSONAL_UTILITY, 0x5EF5),
	Kerachole(SGE, true, 30.0, "Kerachole", CooldownType.PARTY_MIT, 0x5EEA, 0xA3A, 0xB7A),
	Soteria(SGE, true, 90.0, "Soteria", CooldownType.HEAL, 0x5EE6, 0xA32),
	Zoe(SGE, true, 120.0, "Zoe", CooldownType.HEAL, 0x5EEC, 0xA32),
	Ixochole(SGE, true, 30.0, "Pepsis", CooldownType.HEAL, 0x5EEB),
	Icarus(SGE, true, 45.0, "Icarus", CooldownType.PERSONAL_UTILITY, 0x5EE7),
	Taurochole(SGE, true, 45.0, "Taurochole", CooldownType.HEAL, 0x5EEF, 0xA3B),
	Haima(SGE, true, 120.0, "Haima", CooldownType.SINGLE_TARGET_MIT, 0x5EF1, 0xA34),
	Panhaima(SGE, true, 120.0, "Panhaima", CooldownType.PARTY_MIT, 0x5EF7, 0xA35),
	Physis(SGE, true, 60.0, "Physis", CooldownType.HEAL, 0x5EEE, 0xA3C),
	Holos(SGE, true, 120.0, "Holos", CooldownType.PARTY_MIT, 0x5EF6, 0xBBB),

	Divination(AST, true, 120.0, "Divination", CooldownType.PARTY_BUFF, 0x40a8, 1878),


	// MELEE
	Feint(MELEE_DPS, true, 90.0, "Feint", CooldownType.PARTY_MIT, 0x1d7d, 1195),
	TrickAttack(NIN, true, 60.0, "Trick Attack", CooldownType.PARTY_BUFF, 0x8d2, 638),
	BattleLitany(DRG, true, 120.0, "Battle Litany", CooldownType.PARTY_BUFF, 0xde5, 786),
	Brotherhood(MNK, true, 90.0, "Brotherhood", CooldownType.PARTY_BUFF, 0x1ce4, 1185),
	DragonSight(DRG, true, 120.0, "Dragon Sight", CooldownType.PARTY_BUFF, 0x1ce6, 1183, 1184),



	// CASTER
	// TODO: devotion
	Addle(CASTER, true, 90.0, "Addle", CooldownType.PARTY_MIT, 0x1d88, 1203),
	Swiftcast(CASTER, true, 60.0, "Swiftcast", CooldownType.PERSONAL_UTILITY, 0x1d89, 167, 1325),
	Embolden(RDM, true, 120.0, "Embolden", CooldownType.PARTY_BUFF, 0x1d60, 1239, 1297),
	SearingLight(SMN, true, 120.0, "Searing Light", CooldownType.PARTY_BUFF, 25801, 2703),



	// PHYS RANGED
	StandardStep(DNC, true, 30.0, "Standard Step", CooldownType.PARTY_BUFF, 0x3e7d, 1821, 2024),
	TechnicalStep(DNC, true, 120.0, "Technical Step", CooldownType.PARTY_BUFF, 0x3e7e, 1819, 2049),
	Devilment(DNC, true, 120.0, "Devilment", CooldownType.PARTY_BUFF, 0x3e8b, 1825),
	ShieldSamba(DNC, true, 120.0, "Shield Samba", CooldownType.PARTY_MIT, 0x3e8c, 1826),
	Troubadour(BRD, true, 120.0, "Troubadour", CooldownType.PARTY_MIT, 0x1ced, 1934),
	// TODO: these do not have correct duration - need to fix
	MagesBallad(BRD, true, 120.0, "Mage's Ballad", CooldownType.PARTY_BUFF, 0x72, 0x8a9),
	ArmysPaeon(BRD, true, 120.0, "Army's Paeon", CooldownType.PARTY_BUFF, 0x74, 0x8aa),
	WanderersMinuet(BRD, true, 120.0, "Wanderer's Minuet", CooldownType.PARTY_BUFF, 0xde7, 0x8a8),
	BattleVoice(BRD, true, 120.0, "Battle Voice", CooldownType.PARTY_BUFF, 0x76, 0x8d),
	Tactician(MCH, true, 120.0, "Tactician", CooldownType.PARTY_MIT, 0x41f9, 1951, 2177);



	private final boolean defaultPersOverlay;

	public boolean defaultPersOverlay() {
		return this.defaultPersOverlay;
	}

	public enum CooldownType {
		PERSONAL_MIT,
		INVULN,
		PARTY_MIT,
		SINGLE_TARGET_MIT,
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
	private final long[] abilityIds;
	private final long[] buffIds;

	Cooldown(Job job, boolean defaultPersOverlay, double cooldown, String label, CooldownType cooldownType, long[] abilityIds, long[] buffIds) {
		this.job = job;
		this.defaultPersOverlay = defaultPersOverlay;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityIds = abilityIds;
		this.buffIds = buffIds;
	}

	Cooldown(Job job, boolean defaultPersOverlay, double cooldown, String label, CooldownType cooldownType, long abilityId, long... buffId) {
		this.job = job;
		this.defaultPersOverlay = defaultPersOverlay;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityIds = new long[]{abilityId};
		this.buffIds = buffId;
	}

	Cooldown(JobType jobType, boolean defaultPersOverlay, double cooldown, String label, CooldownType cooldownType, long abilityId, long... buffId) {
		this.defaultPersOverlay = defaultPersOverlay;
		this.cooldown = cooldown;
		this.job = null;
		this.jobType = jobType;
		this.type = cooldownType;
		this.label = label;
		this.abilityIds = new long[]{abilityId};
		this.buffIds = buffId;
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
		for (long id : abilityIds) {
			if (id == abilityId) {
				return true;
			}
		}
		return false;
	}

	public boolean buffIdMatches(long buffId) {
		for (long thisBuffId : this.buffIds) {
			if (thisBuffId == buffId) {
				return true;
			}
		}
		return false;
	}

	public double getCooldown() {
		return cooldown;
	}

	// Purposefully saying "primary" here - as some might require multiple CDs (see: Raw/Nascent)
	public long getPrimaryAbilityId() {
		return abilityIds[0];
	}

}
