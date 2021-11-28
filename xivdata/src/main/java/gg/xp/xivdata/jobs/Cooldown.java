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
	Sentinel(PLD, 120.0, "Sentinel", CooldownType.DEFENSIVE, 0x11, -1),
	Cover(PLD, 120.0, "Cover", CooldownType.DEFENSIVE, 0x1b, -1),
	HallowedGround(PLD, 420.0, "Hallowed Ground", CooldownType.DEFENSIVE, 0x1e, -1),
	ThrillofBattle(WAR, 90.0, "Thrill of Battle", CooldownType.DEFENSIVE, 0x28, -1),
	Holmgang(WAR, 240.0, "Holmgang", CooldownType.DEFENSIVE, 0x2b, -1),
	Vengeance(WAR, 120.0, "Vengeance", CooldownType.DEFENSIVE, 0x2c, -1),
	MagesBallad(BRD, 80.0, "Mage's Ballad", CooldownType.OFFENSIVE, 0x72, -1),
	ArmysPaeon(BRD, 80.0, "Army's Paeon", CooldownType.OFFENSIVE, 0x74, -1),
	BattleVoice(BRD, 180.0, "Battle Voice", CooldownType.OFFENSIVE, 0x76, -1),
	Benediction(WHM, 180.0, "Benediction", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x8c, -1),
	SacredSoil(SCH, 30.0, "Sacred Soil", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0xbc, -1),
	TrickAttack(NIN, 60.0, "Trick Attack", CooldownType.OFFENSIVE, 0x8d2, -1),
	DivineVeil(PLD, 90.0, "Divine Veil", CooldownType.DEFENSIVE, 0xdd4, -1),
	RawIntuition(WAR, 25.0, "Raw Intuition", CooldownType.DEFENSIVE, 0xddf, -1),
	BattleLitany(DRG, 180.0, "Battle Litany", CooldownType.OFFENSIVE, 0xde5, -1),
	theWanderersMinuet(BRD, 80.0, "the Wanderer's Minuet", CooldownType.OFFENSIVE, 0xde7, -1),
	DarkMind(DRK, 60.0, "Dark Mind", CooldownType.DEFENSIVE, 0xe32, -1),
	ShadowWall(DRK, 120.0, "Shadow Wall", CooldownType.DEFENSIVE, 0xe34, -1),
	LivingDead(DRK, 300.0, "Living Dead", CooldownType.DEFENSIVE, 0xe36, -1),
	PassageofArms(PLD, 120.0, "Passage of Arms", CooldownType.DEFENSIVE, 0x1cd9, -1),
	ShakeItOff(WAR, 90.0, "Shake It Off", CooldownType.DEFENSIVE, 0x1cdc, -1),
	TheBlackestNight(DRK, 15.0, "The Blackest Night", CooldownType.DEFENSIVE, 0x1ce1, 0x49a),
	Brotherhood(MNK, 90.0, "Brotherhood", CooldownType.OFFENSIVE, 0x1ce4, -1),
	DragonSight(DRG, 120.0, "Dragon Sight", CooldownType.OFFENSIVE, 0x1ce6, -1),
	Troubadour(BRD, 120.0, "Troubadour", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x1ced, -1),
	Aetherpact(SMN, 180.0, "Aetherpact", CooldownType.OFFENSIVE, 0x1cff, -1),
	ChainStratagem(SCH, 120.0, "Chain Stratagem", CooldownType.OFFENSIVE, 0x1d0c, -1),
	Embolden(RDM, 120.0, "Embolden", CooldownType.OFFENSIVE, 0x1d60, -1),
	Rampart(TANK, 90.0, "Rampart", CooldownType.DEFENSIVE, 0x1d6b, -1),
	Reprisal(TANK, 60.0, "Reprisal", CooldownType.DEFENSIVE, 0x1d6f, -1),
	ArmsLength(TANK, 120.0, "Arm's Length", CooldownType.DEFENSIVE, 0x1d7c, -1),
	Feint(MELEE_DPS, 90.0, "Feint", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x1d7d, -1),
	Addle(CASTER, 90.0, "Addle", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x1d88, -1),
	Swiftcast(CASTER, 60.0, "Swiftcast", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x1d89, -1),
	StandardStep(DNC, 30.0, "Standard Step", CooldownType.OFFENSIVE, 0x3e7d, -1),
	TechnicalStep(DNC, 120.0, "Technical Step", CooldownType.OFFENSIVE, 0x3e7e, -1),
	Devilment(DNC, 120.0, "Devilment", CooldownType.OFFENSIVE, 0x3e8b, -1),
	ShieldSamba(DNC, 120.0, "Shield Samba", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x3e8c, -1),
	Camouflage(GNB, 90.0, "Camouflage", CooldownType.DEFENSIVE, 0x3f0c, -1),
	Nebula(GNB, 120.0, "Nebula", CooldownType.DEFENSIVE, 0x3f14, -1),
	Aurora(GNB, 60.0, "Aurora", CooldownType.DEFENSIVE, 0x3f17, -1),
	Superbolide(GNB, 360.0, "Superbolide", CooldownType.DEFENSIVE, 0x3f18, -1),
	HeartofLight(GNB, 90.0, "Heart of Light", CooldownType.DEFENSIVE, 0x3f20, -1),
	HeartofStone(GNB, 25.0, "Heart of Stone", CooldownType.DEFENSIVE, 0x3f21, -1),
	NascentFlash(WAR, 25.0, "Nascent Flash", CooldownType.DEFENSIVE, 0x4050, -1),
	DarkMissionary(DRK, 90.0, "Dark Missionary", CooldownType.DEFENSIVE, 0x4057, -1),
	Divination(AST, 120.0, "Divination", CooldownType.OFFENSIVE, 0x40a8, -1),
	Tactician(MCH, 120.0, "Tactician", CooldownType.PARTY_TODO_CATEGORIZE_ME, 0x41f9, -1),



	FOO(JobType.TANK, 30.0d, "Foo Buff", CooldownType.DEFENSIVE, 0x123, 0x456);

	public enum CooldownType {
		DEFENSIVE,
		OFFENSIVE,
		PERSONAL,
		@SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
		PARTY_TODO_CATEGORIZE_ME
	}

	private final JobType jobType;
	private final Job job;
	private final double cooldown;
	private final CooldownType type;
	private final String label;
	private final long abilityId;
	private final long buffId;

	Cooldown(Job job, double cooldown, String label, CooldownType cooldownType, long abilityId, long buffId) {
		this.job = job;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityId = abilityId;
		this.buffId = buffId;
	}

	Cooldown(JobType jobType, double cooldown, String label, CooldownType cooldownType, long abilityId, long buffId) {
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
		return this.buffId == buffId;
	}

	public double getCooldown() {
		return cooldown;
	}
}
