package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static gg.xp.xivdata.data.Job.AST;
import static gg.xp.xivdata.data.Job.BRD;
import static gg.xp.xivdata.data.Job.DNC;
import static gg.xp.xivdata.data.Job.DRG;
import static gg.xp.xivdata.data.Job.DRK;
import static gg.xp.xivdata.data.Job.GNB;
import static gg.xp.xivdata.data.Job.MCH;
import static gg.xp.xivdata.data.Job.MNK;
import static gg.xp.xivdata.data.Job.NIN;
import static gg.xp.xivdata.data.Job.PLD;
import static gg.xp.xivdata.data.Job.RDM;
import static gg.xp.xivdata.data.Job.RPR;
import static gg.xp.xivdata.data.Job.SCH;
import static gg.xp.xivdata.data.Job.SGE;
import static gg.xp.xivdata.data.Job.SMN;
import static gg.xp.xivdata.data.Job.WAR;
import static gg.xp.xivdata.data.Job.WHM;
import static gg.xp.xivdata.data.JobType.CASTER;
import static gg.xp.xivdata.data.JobType.HEALER;
import static gg.xp.xivdata.data.JobType.MELEE_DPS;
import static gg.xp.xivdata.data.JobType.TANK;

public enum Cooldown {

	// DO NOT change enum member names - they are used as settings keys

	// List of ALL buffs to track - WL/BL will be done by user settings
	// TANKS
	Rampart(builder(CooldownType.PERSONAL_MIT, true, 0x1d6b).buffIds(1191)),
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
	Expiacion(PLD, true, 30.0, "Expiacion", CooldownType.PERSONAL_BURST, 0x6493),
	Nebula(GNB, true, 120.0, "Nebula", CooldownType.PERSONAL_MIT, 0x3f14, 1834),
	Aurora(GNB, true, 60.0, "Aurora", CooldownType.HEAL, 0x3f17, 1835, 2065),
	Superbolide(GNB, true, 360.0, "Superbolide", CooldownType.INVULN, 0x3f18, 1836),
	HeartofLight(GNB, true, 90.0, "Heart of Light", CooldownType.PARTY_MIT, 0x3f20, 1839),
	HeartofStone(GNB, true, 25.0, "Heart of Stone", CooldownType.PARTY_MIT, 0x3f21, 1840),
	HeartofCorundum(GNB, true, 25.0, "Heart of Corundum", CooldownType.PARTY_MIT, 25758, 2683),
	Camouflage(GNB, true, 90.0, "Camouflage", CooldownType.PERSONAL_MIT, 0x3f0c, 1832),
	//	NascentFlash(WAR, true, 25.0, "Nascent Flash", CooldownType.HEAL, 0x4050, 1857, 1858),
	InnerRelease(WAR, true, 60.0, "Inner Release", CooldownType.PERSONAL_BURST, 0x1CDD, 0x499),
	ThrillofBattle(WAR, true, 90.0, "Thrill of Battle", CooldownType.PERSONAL_MIT, 0x28, 87),
	Holmgang(WAR, true, 240.0, "Holmgang", CooldownType.INVULN, 0x2b, 409),
	Vengeance(WAR, true, 120.0, "Vengeance", CooldownType.PERSONAL_MIT, 0x2c, 89),
	RawIntuition(WAR, true, 25.0, "Raw/Nascent/Bloodwhetting", CooldownType.PERSONAL_MIT, new long[]{0xddfL, 0x4050, 0x6497}, new long[]{735L, 1857, 1858, 0xA76L}),
	Upheaval(WAR, true, 30.0, "Upheaval/Orogeny", CooldownType.PERSONAL_BURST, new long[]{0x1CDB, 0x6498}, new long[]{}),
	ShakeItOff(WAR, true, 90.0, "Shake It Off", CooldownType.PARTY_MIT, 0x1cdc, 1457),
	Equilibrium(WAR, false, 60.0, "Equilibrium", CooldownType.HEAL, 0xDE0),
	// TODO: infuriate requires CD reduction support
	Onslaught(WAR, true, 30.0, 3, "Onslaught", CooldownType.PERSONAL_UTILITY, 0x1CDA),

	DarkMissionary(DRK, true, 90.0, "Dark Missionary", CooldownType.PARTY_MIT, 0x4057, 1894),
	DarkMind(DRK, true, 60.0, "Dark Mind", CooldownType.PERSONAL_MIT, 0xe32, 746),
	ShadowWall(DRK, true, 120.0, "Shadow Wall", CooldownType.PERSONAL_MIT, 0xe34, 747),
	LivingDead(DRK, true, 300.0, "Living Dead", CooldownType.INVULN, 0xe36, 810),
	TheBlackestNight(DRK, true, 15.0, "The Blackest Night", CooldownType.PERSONAL_MIT, 0x1ce1, 0x49a),
	BloodWeapon(builder(CooldownType.PERSONAL_BURST, true, 0xE29).buffIds(0x2e6)),
	SaltedEarth(builder(CooldownType.PERSONAL_BURST, true, 0xE37).buffIds(0x2ed)),
	Plunge(builder(CooldownType.PERSONAL_BURST, true, 0xE38).maxCharges(2)),
	// Also carve and spit
	AbyssalDrain(builder(CooldownType.PERSONAL_BURST, true, 0xE39, 0xE3B)),
	// TODO: maybe abilities like delirium and blood weapon should display remaining stacks rather than remaining duration?
	Delirium(builder(CooldownType.PERSONAL_BURST, true, 0x1CDE).buffIds(0x7b4)),
	LivingShadow(builder(CooldownType.PERSONAL_BURST, true, 0x4058).duration(24)),
	Oblation(builder(CooldownType.PARTY_MIT, false, 0x649A)),
	Shadowbringer(builder(CooldownType.PERSONAL_BURST, true, 0x649D)),

	// HEALERS
	LucidDreaming(HEALER, false, 60.0, "Lucid Dreaming", CooldownType.PERSONAL_UTILITY, 0x1D8A, 0x4B4),
	// TODO - check ability ID
	Benediction(WHM, true, 180.0, "Benediction", CooldownType.HEAL, 0x8c),
	Temperance(WHM, true, 120.0, "Temperance", CooldownType.PARTY_MIT, 0x4098, 0x750),
	Pom(WHM, true, 120.0, "Presence of Mind", CooldownType.PERSONAL_BURST, 0x88, 0x9d),
	Asylum(WHM, true, 45.0, "Asylum", CooldownType.HEAL, 0xDF1, 0x777),
	Aquaveil(WHM, true, 60.0, "Aquaveil", CooldownType.PERSONAL_MIT, 0x6505, 0xA94),
	Bell(WHM, true, 180.0, "Liturgy of the Bell", CooldownType.HEAL, 0x6506, 0xA95),
	Assize(WHM, true, 45.0, "Assize", CooldownType.PERSONAL_BURST, 0xdf3),
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
//	Phlegma(SGE, true, 45.0, 2, "Phlegma", CooldownType.PERSONAL_BURST, 0x5EF9),
	// TODO: revisit the automatic naming stuff
	Phlegma(builder(CooldownType.PERSONAL_BURST, true, 24313, 24307, 24289).name("Phlegma")),
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
	Draw(AST, true, 30.0, 2, "Draw", CooldownType.PARTY_BUFF, 0xE06),
	MinorArcana(AST, true, 60.0, "Minor Arcana", CooldownType.PARTY_BUFF, 0x1D13),
	// TODO: active status on charge-based abilities?
	CelInt(AST, true, 30.0, 2, "Celestial Intersection", CooldownType.HEAL, 0x40AC),
	// Star is an interesting one due to having two stages - perhaps this would be a good use of making a 4th bar color (maybe purple)?
	Copp(AST, true, 60.0, "Celestial Opposition", CooldownType.HEAL, 0x40A9, 0x757),
	Lightspeed(AST, false, 90.0, "Light Speed", CooldownType.PERSONAL_UTILITY, 0xE16, 0x349),
	// TODO: synastry
	Edig(AST, false, 40.0, 2, "Essential Dignity", CooldownType.HEAL, 0xE1E),
	Horoscope(AST, false, 60.0, "Horoscope", CooldownType.HEAL, 0x40AD, 0x762, 0x763),
	Star(AST, true, 60.0, "Earthly Star", CooldownType.HEAL, 0x1D0F, 0x4C8, 0x4E0),
	// Not sure if I should actually put a buff in for this one, since there's three, and they all mean something slightly different
	Cunc(AST, false, 60.0, "Collective Unconscious", CooldownType.PARTY_MIT, 0xE1D),
	Exaltation(AST, false, 60.0, "Exaltation", CooldownType.SINGLE_TARGET_MIT, 0x6511, 0xA9D),
	Neutral(AST, false, 120.0, "Neutral Sect", CooldownType.HEAL, 0x40AF, 0x764),
	Macrocosmos(AST, false, 180.0, "Macrocosmos", CooldownType.HEAL, 0x6512, 0xA9E),


	// MELEE
	Feint(MELEE_DPS, true, 90.0, "Feint", CooldownType.PARTY_MIT, 0x1d7d, 1195),
	//	TrickAttack(NIN, true, 60.0, "Trick Attack", CooldownType.PARTY_BUFF, 0x8d2, 638),
	// TODO
	TrickAttackNew(NIN, true, 60.0, "Trick Attack", CooldownType.PERSONAL_BURST, 0x8d2, 3254),
	// TODO
	Mug(NIN, true, 120.0, "Mug", CooldownType.PARTY_BUFF, 0x8C8, 638),
	BattleLitany(DRG, true, 120.0, "Battle Litany", CooldownType.PARTY_BUFF, 0xde5, 786),
	Brotherhood(MNK, true, 120.0, "Brotherhood", CooldownType.PARTY_BUFF, 0x1ce4, 1185),
	RiddleOfWind(MNK, true, 90.0, "Riddle of Wind", CooldownType.PERSONAL_BURST, 0x64A6, 2687),
	DragonSight(DRG, true, 120.0, "Dragon Sight", CooldownType.PARTY_BUFF, 0x1ce6, 1183, 1184),
	ArcaneCircle(RPR, true, 120.0, "Arcane Circle", CooldownType.PARTY_BUFF, 0x5F55, 0xA27),
	ArcaneCrest(RPR, true, 30.0, "Arcane Crest", CooldownType.PERSONAL_MIT, 0x5F54, 0xA24),


	// CASTER
	Addle(CASTER, true, 90.0, "Addle", CooldownType.PARTY_MIT, 0x1d88, 1203),
	Swiftcast(CASTER, true, 60.0, "Swiftcast", CooldownType.PERSONAL_UTILITY, 0x1d89, 167, 1325),
	Embolden(RDM, true, 120.0, "Embolden", CooldownType.PARTY_BUFF, 0x1d60, 1239, 1297),
	Manafication(RDM, true, 110.0, "Manafication", CooldownType.PERSONAL_BURST, 0x1D61, 0x7b3),
	MagicBarrier(RDM, true, 120.0, "Magic Barrier", CooldownType.PARTY_MIT, 0x6501, 0xA93),
	Acceleration(RDM, true, 55.0, 2, "Acceleration", CooldownType.PERSONAL_BURST, 0x1D5E, 0x4D6),
	Fleche(RDM, true, 25.0, "Fleche", CooldownType.PERSONAL_BURST, 0x1D5D),
	ContreSixte(RDM, true, 35.0, "Contre Sixte", CooldownType.PERSONAL_BURST, 0x1D5F),
	// TODO: can't add these yet because they are charges
//	CorpsACorps(RDM, true, 35.0, "Corps-a-Corps", CooldownType.PERSONAL_BURST, 0x1D52),
//	Displacement(RDM, true, 35.0, "Displace/Engage", CooldownType.PERSONAL_BURST, new long[]{0x1D5B, 0x408F}, new long[]{}),
	SearingLight(SMN, true, 120.0, "Searing Light", CooldownType.PARTY_BUFF, 25801, 2703),


	// PHYS RANGED
	// Specifically NOT providing buff IDs for standard step, since you'd want to use it off CD for damage, and
	// the duration is longer than the CD, so you'd never actually see when you need to use it.
	StandardStep(DNC, true, 30.0, "Standard Step", CooldownType.PARTY_BUFF, 0x3e7d),
	// TODO: there's a lvl 82 trait that might affect this
	// TODO: tech step is weird. The buff isn't applied by step, but rather by finish, but the cooldown starts when
	// you step.
	// It looks like if you fail to complete all the steps, it's still the same buff ID.
	TechnicalStep(DNC, true, 120.0, "Technical Step", CooldownType.PARTY_BUFF, 0x3e7e, 0x71e),
	Devilment(DNC, true, 120.0, "Devilment", CooldownType.PARTY_BUFF, 0x3e8b, 1825),
	ShieldSamba(DNC, true, 120.0, "Shield Samba", CooldownType.PARTY_MIT, 0x3e8c, 1826),
	Troubadour(BRD, true, 120.0, "Troubadour", CooldownType.PARTY_MIT, 0x1ced, 1934),
	MagesBallad(BRD, true, 120.0, 45.0, "Mage's Ballad", CooldownType.PARTY_BUFF, 0x72, 0x8a9),
	ArmysPaeon(BRD, true, 120.0, 45.0, "Army's Paeon", CooldownType.PARTY_BUFF, 0x74, 0x8aa),
	WanderersMinuet(BRD, true, 120.0, 45.0, "Wanderer's Minuet", CooldownType.PARTY_BUFF, 0xde7, 0x8a8),
	BattleVoice(BRD, true, 120.0, "Battle Voice", CooldownType.PARTY_BUFF, 0x76, 0x8d),
	RadiantFinale(BRD, true, 110.0, "Radiant Finale", CooldownType.PARTY_BUFF, 0x64B9, 0xB94),
	Tactician(MCH, true, 120.0, "Tactician", CooldownType.PARTY_MIT, 0x41f9, 1951, 2177);


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
	private final int maxCharges;
	private final boolean defaultPersOverlay;
	private final @Nullable Double durationOverride;

	private static class CdBuilder {
		// Required
		private JobType jobType;
		private Job job;
		private final CooldownType type;
		private final long[] abilityIds;
		private final boolean defaultPersOverlay;

		// Optional
		private long[] buffIds = {};
		private Integer maxCharges;
		private Double cooldown;
		private Double durationOverride;
		private String name;

		private CdBuilder buffIds(long... buffIds) {
			this.buffIds = buffIds;
			return this;
		}

		private CdBuilder maxCharges(int maxCharges) {
			this.maxCharges = maxCharges;
			return this;
		}

		private CdBuilder cooldown(double cd) {
			this.cooldown = cd;
			return this;
		}

		private CdBuilder duration(double durationOverride) {
			this.durationOverride = durationOverride;
			return this;
		}

		private CdBuilder name(String name) {
			this.name = name;
			return this;
		}

		// TODO: technically, Job/JobType is also in the CSV...
		private CdBuilder(CooldownType type, boolean defaultPersOverlay, long[] abilityIds) {
			this.type = type;
			this.defaultPersOverlay = defaultPersOverlay;
			this.abilityIds = abilityIds;
		}

		private ActionInfo getActionInfo() {
			return getActionInfo(abilityIds[0]);
		}

		private static ActionInfo getActionInfo(long id) {
			ActionInfo actionInfo = ActionLibrary.forId(id);
			if (actionInfo == null) {
				throw new RuntimeException(String.format("Could not find ActionInfo for action %X", id));
			}
			return actionInfo;
		}

		private List<ActionInfo> getAllActionInfo() {
			return Arrays.stream(abilityIds).mapToObj(CdBuilder::getActionInfo).toList();
		}

		public double getCooldown() {
			if (cooldown == null) {
				return getActionInfo().getCd();
			}
			else {
				return cooldown;
			}
		}

		public String getName() {
			if (name == null) {
				return getAllActionInfo().stream()
						.map(ActionInfo::name)
						.map(ActionUtils.adjustNameReverse())
						.collect(Collectors.joining("/"));
			}
			else {
				return name;
			}
		}

		public JobType getJobType() {
			if (jobType == null) {
				return switch (Integer.parseInt(getActionInfo().categoryRaw())) {
					case 113 -> TANK;
					// It's actually not but I don't have a category for DoW/DoM yet
					case 161 -> TANK;
					// This is actually DoM
					case 120 -> CASTER;
					case 114 -> MELEE_DPS;
					case 116 -> CASTER;
					default -> null;
				};
			}
			else {
				return jobType;
			}
		}

		public Job getJob() {
			if (job == null) {
				return switch (Integer.parseInt(getActionInfo().categoryRaw())) {
					case 20, 38 -> PLD;
					case 22, 44 -> WAR;
					case 98 -> DRK;
					case 149 -> GNB;
					case 25 -> WHM;
					case 29 -> SCH;
					case 99 -> AST;
					case 181 -> SGE;
					case 21 -> MNK;
					case 23 -> DRG;
					case 93 -> NIN;
					case 180 -> RPR;
					case 24 -> BRD;
					case 96 -> MCH;
					case 150 -> DNC;
					case 28 -> SMN;
					case 112 -> RDM;
					default -> null;
				};
			}
			else {
				return job;
			}
		}

		public int getMaxCharges() {
			if (maxCharges == null) {
				return getActionInfo().maxCharges();
			}
			else {
				return maxCharges;
			}
		}
	}

	private static CdBuilder builder(CooldownType type, boolean defaultPersOverlay, long... abilityIds) {
		return new CdBuilder(type, defaultPersOverlay, abilityIds);
	}

	Cooldown(CdBuilder builder) {
		jobType = builder.getJobType();
		job = builder.getJob();
		abilityIds = builder.abilityIds;
		defaultPersOverlay = builder.defaultPersOverlay;
		cooldown = builder.getCooldown();
		type = builder.type;
		label = builder.getName();
		buffIds = builder.buffIds;
		maxCharges = builder.getMaxCharges();
		durationOverride = builder.durationOverride;
		if (job == null && jobType == null) {
			throw new RuntimeException(String.format("Cooldown %s has neither a job nor jobtype", label));
		}
	}

	Cooldown(Job job, boolean defaultPersOverlay, double cooldown, String label, CooldownType cooldownType, long[] abilityIds, long[] buffIds) {
		this.job = job;
		this.defaultPersOverlay = defaultPersOverlay;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityIds = abilityIds;
		this.buffIds = buffIds;
		this.maxCharges = 1;
		durationOverride = null;
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
		this.maxCharges = 1;
		durationOverride = null;
	}

	Cooldown(Job job, boolean defaultPersOverlay, double cooldown, double durationOverride, String label, CooldownType cooldownType, long abilityId, long... buffId) {
		this.job = job;
		this.defaultPersOverlay = defaultPersOverlay;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityIds = new long[]{abilityId};
		this.buffIds = buffId;
		this.maxCharges = 1;
		this.durationOverride = durationOverride;
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
		this.maxCharges = 1;
		durationOverride = null;
	}

	Cooldown(Job job, boolean defaultPersOverlay, double cooldown, int charges, String label, CooldownType cooldownType, long abilityId, long... buffId) {
		this.job = job;
		this.defaultPersOverlay = defaultPersOverlay;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityIds = new long[]{abilityId};
		this.buffIds = buffId;
		this.maxCharges = charges;
		durationOverride = null;
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

	public Duration getCooldownAsDuration() {
		return Duration.ofMillis((long) (cooldown * 1000L));
	}

	// Purposefully saying "primary" here - as some might require multiple CDs (see: Raw/Nascent)
	public long getPrimaryAbilityId() {
		return abilityIds[0];
	}

	public int getMaxCharges() {
		return maxCharges;
	}

	public @Nullable Double getDurationOverride() {
		return durationOverride;
	}
}
