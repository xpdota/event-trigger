package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public enum Cooldown implements ExtendedCooldownDescriptor {
	// List of ALL buffs to track - WL/BL will be done by user settings
	// DO NOT change enum member names - they are used as settings keys!

	/*

	Most properties (cooldown, name, charges, etc) do not need to be set here. They are determined automatically
	from game files. At a minimum, you need the category for the cooldown, whether or not it should be in the
	personal CDs overlay by default, or not, and the ability ID(s). You only need to override parts if:

	1. Traits change the cooldown/charges of the skill. The game files contain the traitless data, and there is no
		currently known way of deciphering the trait data into concrete cd/charge changes.

	2. The cooldown is affected by some other skill. For example, with enhanced unmend, you need to list it as an
		"auxAbility" on Shadowstride.

	*/

	// TANKS
	Rampart(builder(CooldownType.PERSONAL_MIT, true, 0x1d6b)),
	Reprisal(builder(CooldownType.PERSONAL_MIT, true, 0x1d6f)),
	ArmsLength(builder(CooldownType.PERSONAL_UTILITY, true, 0x1d7c)),

	// PLD
	HallowedGround(builder(CooldownType.INVULN, true, 0x1e)),
	Sentinel(builder(CooldownType.PERSONAL_MIT, true, 0x11, 0x9038)),
	Cover(builder(CooldownType.PERSONAL_MIT, true, 0x1b)),
	// TODO: sheltron/holy sheltron
	FightOrFlight(builder(CooldownType.PERSONAL_BURST, true, 0x14)),
	Requiescat(builder(CooldownType.PERSONAL_BURST, true, 0x1CD7, 0x9039)),
	// TODO: check auto with this
	DivineVeil(builder(CooldownType.PARTY_MIT, true, 0xdd4).buffIds(726, 727)),
	//	DivineVeil(PLD, true, 90.0, "Divine Veil", CooldownType.PARTY_MIT, 0xdd4, 726, 727),
	// TODO: buff
	PassageofArms(true, CooldownType.PARTY_MIT, 0x1cd9),
	CircleOfScorn(true, CooldownType.PERSONAL_BURST, 0x17),
	Intervene(true, CooldownType.PERSONAL_BURST, 0x404D),
	Expiacion(true, CooldownType.PERSONAL_BURST, 0x6493),

	// GNB
	Nebula(builder(CooldownType.PERSONAL_MIT, true, 0x3f14, 0x9047)),
	Aurora(true, CooldownType.HEAL, 0x3f17),
	Superbolide(true, CooldownType.INVULN, 0x3f18),
	HeartofLight(true, CooldownType.PARTY_MIT, 0x3f20),
	HeartofStone(true, CooldownType.PARTY_MIT, 0x3f21),
	HeartofCorundum(true, CooldownType.PARTY_MIT, 25758),
	Camouflage(true, CooldownType.PERSONAL_MIT, 0x3f0c),
	Bloodfest(builder(CooldownType.PERSONAL_BURST, true, 0x3F24)),
	BowShock(builder(CooldownType.PERSONAL_BURST, true, 0x3F1F)),
	// Also blasting zone
	DangerZone(builder(CooldownType.PERSONAL_BURST, true, 0x3F10, 0x3F25)),
	DoubleDown(builder(CooldownType.PERSONAL_BURST, true, 0x64A0)),
	NoMercy(builder(CooldownType.PERSONAL_BURST, true, 0x3F0A)),
	Trajectory(builder(CooldownType.PERSONAL_BURST, true, 0x9046)),

	// WAR
	//	NascentFlash(WAR, true, 25.0, "Nascent Flash", CooldownType.HEAL, 0x4050, 1857, 1858),
	InnerRelease(true, CooldownType.PERSONAL_BURST, 0x1CDD),
	ThrillofBattle(true, CooldownType.PERSONAL_MIT, 0x28),
	Holmgang(true, CooldownType.INVULN, 0x2b),
	Vengeance(builder(CooldownType.PERSONAL_MIT, true, 0x2c, 0x903B)),
	RawIntuition(builder(CooldownType.PERSONAL_MIT, true, 0xddfL, 0x4050, 0x6497).buffIds(735L, 1857, 1858, 0xA76L)),
	Upheaval(builder(CooldownType.PERSONAL_BURST, true, 0x1CDB, 0x6498)),
	ShakeItOff(true, CooldownType.PARTY_MIT, 0x1cdc),
	Equilibrium(false, CooldownType.HEAL, 0xDE0),
	Infuriate(builder(CooldownType.PERSONAL_BURST, true, 0x34)
			// Fell cleave
			.auxAbility(0xDDD, -5)
			// Decimate
			.auxAbility(0xDDE, -5)
			// Inner Chaos
			.auxAbility(0x404F, -5)
			// Chaotic Cyclone
			.auxAbility(0x4051, -5)
	),
	// TODO: level based modifications
	Onslaught(builder(CooldownType.PERSONAL_UTILITY, true, 0x1CDA).maxCharges(3)),
//	Onslaught(WAR, true, 30.0, 3, "Onslaught", CooldownType.PERSONAL_UTILITY, 0x1CDA),

	// DRK
	DarkMissionary(true, CooldownType.PARTY_MIT, 0x4057),
	DarkMind(true, CooldownType.PERSONAL_MIT, 0xe32),
	ShadowWall(builder(CooldownType.PERSONAL_MIT, true, 0xe34, 0x903f)),
	LivingDead(true, CooldownType.INVULN, 0xe36),
	TheBlackestNight(true, CooldownType.PERSONAL_MIT, 0x1ce1),
	SaltedEarth(builder(CooldownType.PERSONAL_BURST, true, 0xE37)),
	AbyssalDrain(builder(CooldownType.PERSONAL_BURST, true, 0xE39, 0xE3B)),
	// TODO: maybe abilities like delirium and blood weapon should display remaining stacks rather than remaining duration?
	Delirium(builder(CooldownType.PERSONAL_BURST, true, 0xE29, 0x1CDE)),
	LivingShadow(builder(CooldownType.PERSONAL_BURST, true, 0x4058).noAutoBuffs().duration(24)),
	Oblation(builder(CooldownType.PARTY_MIT, false, 0x649A)),
	Shadowbringer(builder(CooldownType.PERSONAL_BURST, true, 0x649D)),
	Shadowstride(builder(CooldownType.PERSONAL_UTILITY, false, 0x903E)
			// Unmend
			.auxAbility(0xE28, -5)),

	// HEALERS
	LucidDreaming(false, CooldownType.PERSONAL_UTILITY, 0x1D8A),

	// WHM
	Benediction(true, CooldownType.HEAL, 0x8c),
	Temperance(true, CooldownType.PARTY_MIT, 0x4098),
	Pom(true, CooldownType.PERSONAL_BURST, 0x88),
	Asylum(builder(CooldownType.HEAL, true, 0xDF1)),
	Aquaveil(true, CooldownType.PERSONAL_MIT, 0x6505),
	Bell(true, CooldownType.HEAL, 0x6506),
	Assize(true, CooldownType.PERSONAL_BURST, 0xdf3),
	AetherialShift(builder(CooldownType.PERSONAL_UTILITY, false, 0x9090)),
	Plenary(builder(CooldownType.HEAL, false, 0x1D09)),
	Tetra(builder(CooldownType.HEAL, false, 0xDF2).maxCharges(2)),
	ThinAir(builder(CooldownType.PERSONAL_UTILITY, false, 0x1D06)),
	DivineBenison(builder(CooldownType.HEAL, false, 0x1D08).maxCharges(2)),

	// SCH
	SacredSoil(true, CooldownType.PARTY_MIT, 0xbc),
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
	// Pet skills require buff IDs because the pet's ability casts them
	WhisperingDawn(builder(CooldownType.HEAL, true, 0x4099).buffIds(0x13b, 0x752)),
	FeyIllumination(builder(CooldownType.PARTY_MIT, true, 0x409A).buffIds(0x13d, 0x753)),
	FeyBlessing(true, CooldownType.HEAL, 0x409F),
	Expedient(true, CooldownType.PARTY_UTILITY, 0x650C),
	ChainStratagem(builder(CooldownType.PARTY_BUFF, true, 0x1d0c).buffIds(0x4c5)),
	Protraction(true, CooldownType.HEAL, 0x650b),
	Aetherflow(true, CooldownType.PERSONAL_UTILITY, 0xa6),
	Dissipation(true, CooldownType.PERSONAL_UTILITY, 0xe03),
	Recitation(true, CooldownType.HEAL, 0x409E),
	//	DeploymentTactics(SCH, false, 90.0, "Deployment Tactics", CooldownType.PARTY_MIT, 0xE01),
	DeploymentTactics(builder(CooldownType.PARTY_MIT, false, 0xE01).cooldown(90)),
	EmergencyTactics(false, CooldownType.PARTY_MIT, 0xE02),
	Seraphism(builder(CooldownType.HEAL, false, 0x9096)),
	Seraph(builder(CooldownType.HEAL, false, 0x40A1).duration(22)),
	Excogitation(builder(CooldownType.HEAL, false, 0x1D0A)),
	Indomitability(builder(CooldownType.HEAL, false, 0xDFF)),

	// SGE
	// TODO: revisit the automatic naming stuff
	Phlegma(builder(CooldownType.PERSONAL_BURST, true, 24313, 24307, 24289).name("Phlegma")),
	Krasis(true, CooldownType.HEAL, 0x5EFD),
	Pepsis(true, CooldownType.HEAL, 0x5EED),
	Rhizomata(true, CooldownType.PERSONAL_UTILITY, 0x5EF5),
	Kerachole(true, CooldownType.PARTY_MIT, 0x5EEA),
	Soteria(builder(CooldownType.HEAL, true, 0x5EE6).cooldown(60)),
	Zoe(true, CooldownType.HEAL, 0x5EEC),
	Ixochole(true, CooldownType.HEAL, 0x5EEB),
	Icarus(true, CooldownType.PERSONAL_UTILITY, 0x5EE7),
	Taurochole(true, CooldownType.HEAL, 0x5EEF),
	// TODO: Duration override is really just being used as 'ignore refresh' here - might look for a better way
	// The impact of this is that if it does get fully consumed, we'll still show a duration
	Haima(builder(CooldownType.SINGLE_TARGET_MIT, true, 0x5EF1).buffIds(0xA52).duration(15.0)),
	Panhaima(builder(CooldownType.PARTY_MIT, true, 0x5EF7).buffIds(0xA53).duration(15.0)),
	Physis(true, CooldownType.HEAL, 0x5EEE),
	Holos(true, CooldownType.PARTY_MIT, 0x5EF6),
	Pneuma(true, CooldownType.HEAL, 0x5EFE),
	Psyche(true, CooldownType.PERSONAL_BURST, 0x90A9),
	Philosophia(builder(CooldownType.HEAL, false, 0x90AB)),

	// AST
	Divination(true, CooldownType.PARTY_BUFF, 0x40a8),
	AstralUmbralDraw(builder(CooldownType.PARTY_BUFF, true, 0x9099, 0x909A)),
	// TODO: active status on charge-based abilities?
	CelInt(builder(CooldownType.HEAL, true, 0x40AC).maxCharges(2)),
	// Star is an interesting one due to having two stages - perhaps this would be a good use of making a 4th bar color (maybe purple)?
	Copp(true, CooldownType.HEAL, 0x40A9),
	Lightspeed(false, CooldownType.PERSONAL_UTILITY, 0xE16),
	Edig(builder(CooldownType.HEAL, false, 0xE1E).maxCharges(3)),
	Synastry(builder(CooldownType.HEAL, false, 0xE1C)),
	Horoscope(builder(CooldownType.HEAL, false, 0x40AD).buffIds(0x762, 0x763)),
	Star(builder(CooldownType.HEAL, true, 0x1D0F).buffIds(0x4C8, 0x4E0)),
	// Not sure if I should actually put a buff in for this one, since there's three, and they all mean something slightly different
	Cunc(false, CooldownType.PARTY_MIT, 0xE1D),
	Exaltation(false, CooldownType.SINGLE_TARGET_MIT, 0x6511),
	Neutral(false, CooldownType.HEAL, 0x40AF),
	Macrocosmos(false, CooldownType.HEAL, 0x6512),


	// MELEE
	Feint(true, CooldownType.PARTY_MIT, 0x1d7d),

	// NIN
	TrickAttackNew(builder(CooldownType.PERSONAL_BURST, true, 0x905E, 0x8d2).name("Kunai's Bane")),
	Mug(builder(CooldownType.PARTY_BUFF, true, 0x905D, 0x8C8).name("Dokumori")),
	Bunshin(builder(CooldownType.PERSONAL_BURST, true, 0x406D)),
	DreamAssassinate(builder(CooldownType.PERSONAL_BURST, true, 0x8C6, 0xDEE)),
	Kassatsu(builder(CooldownType.PERSONAL_BURST, true, 0x8D8)),
	Meisui(builder(CooldownType.PERSONAL_BURST, true, 0x4069)),
	Shadeshift(builder(CooldownType.PERSONAL_BURST, true, 0x8C1)),
	Shukuchi(builder(CooldownType.PERSONAL_BURST, true, 0x8D6).maxCharges(2)),
	TCJ(builder(CooldownType.PERSONAL_BURST, true, 0x1CEB)),

	// DRG
	BattleLitany(true, CooldownType.PARTY_BUFF, 0xde5),
	DragonfireDive(builder(CooldownType.PERSONAL_BURST, true, 0x60)),
	Jump(builder(CooldownType.PERSONAL_BURST, true, 0x5C, 0x405E)),
	LanceCharge(builder(CooldownType.PERSONAL_BURST, true, 0x55)),
	LifeSurge(builder(CooldownType.HEAL, false, 0x53).maxCharges(2)),
	WingedGlide(builder(CooldownType.PERSONAL_UTILITY, false, 0x9057).maxCharges(2)),


	// MNK
	Brotherhood(true, CooldownType.PARTY_BUFF, 0x1ce4),
	RiddleOfWind(true, CooldownType.PERSONAL_BURST, 0x64A6),
	Mantra(builder(CooldownType.HEAL, false, 0x41)),
	PerfectBalance(builder(CooldownType.PERSONAL_BURST, true, 0x45)),
	RiddleOfEarth(builder(CooldownType.PERSONAL_BURST, false, 0x1CE2)),
	RiddleOfFire(builder(CooldownType.PERSONAL_BURST, false, 0x1CE3)),
	Thunderclap(builder(CooldownType.PERSONAL_BURST, false, 0x64A2).maxCharges(3)),

	// RPR
	ArcaneCircle(true, CooldownType.PARTY_BUFF, 0x5F55),
	ArcaneCrest(true, CooldownType.PERSONAL_MIT, 0x5F54),
	Gluttony(builder(CooldownType.PERSONAL_BURST, true, 0x5F49)),
	SoulSliceScythe(builder(CooldownType.PERSONAL_BURST, true, 0x5F3C, 0x5F3D).maxCharges(2)),
	IngressEgress(builder(CooldownType.PERSONAL_UTILITY, true, 0x5F51, 0x5F52)),

	// SAM
	MeikyoShisui(false, CooldownType.PERSONAL_BURST, 7499),
	Ikishoten(false, CooldownType.PERSONAL_BURST, 16482),
	ThirdEye(builder(CooldownType.PERSONAL_MIT, false, 7498, 0x9062)),
	HissatsuGurenSenei(builder(CooldownType.PERSONAL_BURST, false, 0x1D48, 0x4061).name("Hissatsu: Guren/Senei")),

	// VPR
	// Also pit of dread
	Dreadwinder(builder(CooldownType.PERSONAL_BURST, true, 0x873C, 0x873F).maxCharges(3)),
	SerpentsIre(builder(CooldownType.PERSONAL_BURST, true, 0x8757)),
	Slither(builder(CooldownType.PERSONAL_UTILITY, false, 0x8756)),

	// CASTER
	Addle(true, CooldownType.PARTY_MIT, 0x1d88),
	Swiftcast(builder(CooldownType.PERSONAL_UTILITY, true, 0x1d89).cooldown(40.0)),

	// RDM
	Embolden(true, CooldownType.PARTY_BUFF, 0x1d60),
	Manafication(builder(CooldownType.PERSONAL_BURST, true, 0x1D61).cooldown(110.0)),
	MagicBarrier(true, CooldownType.PARTY_MIT, 0x6501),
	Acceleration(builder(CooldownType.PERSONAL_BURST, true, 0x1D5E).maxCharges(2)),
	//	Acceleration(RDM, true, 55.0, 2, "Acceleration", CooldownType.PERSONAL_BURST, 0x1D5E, 0x4D6),
	Fleche(true, CooldownType.PERSONAL_BURST, 0x1D5D),
	ContreSixte(builder(CooldownType.PERSONAL_BURST, true, 0x1D5F).cooldown(35.0)),
	//	ContreSixte(RDM, true, 35.0, "Contre Sixte", CooldownType.PERSONAL_BURST, 0x1D5F),
	CorpsACorps(builder(CooldownType.PERSONAL_BURST, true, 0x1D52)),
	DisplacementEngagement(builder(CooldownType.PERSONAL_BURST, true, 0x1D5B, 0x408F)),
//	Displacement(RDM, true, 35.0, "Displace/Engage", CooldownType.PERSONAL_BURST, new long[]{0x1D5B, 0x408F}, new long[]{}),

	// SMN
	SearingLight(true, CooldownType.PARTY_BUFF, 25801),
	EnergyDrainSiphon(builder(CooldownType.PERSONAL_BURST, true, 0x407C, 0x407E)),
	RadiantAegis(builder(CooldownType.PERSONAL_MIT, true, 0x64C7).maxCharges(2)),

	// BLM
	Amplifier(builder(CooldownType.PERSONAL_BURST, true, 0x64C4)),
	LeyLines(builder(CooldownType.PERSONAL_BURST, true, 0xDF5)),
	Manafont(builder(CooldownType.PERSONAL_BURST, true, 0x9E).cooldown(100)),
	Manaward(builder(CooldownType.PERSONAL_MIT, false, 0x9D)),
	Retrace(builder(CooldownType.PERSONAL_BURST, true, 0x907C)),
	Triplecast(builder(CooldownType.PERSONAL_BURST, true, 0x1CFD)),

	// PCT
	StarryMuse(builder(CooldownType.PARTY_BUFF, true, 34675)),
	SteelMuse(builder(CooldownType.PERSONAL_BURST, false, 34685, 34675).name("Steel/Striking Muse").maxCharges(2)),
	LivingMuse(builder(CooldownType.PERSONAL_BURST, false, 35347, 34670, 34671, 34672, 34673).name("Living Muse").maxCharges(3)),
	Smudge(builder(CooldownType.PERSONAL_UTILITY, false, 0x877C)),
	// TODO: Cannot do Tempera Coat (yet) because of the CD reduction mechanic

	// PHYS RANGED

	// DNC
	// Specifically NOT providing buff IDs for standard step, since you'd want to use it off CD for damage, and
	// the duration is longer than the CD, so you'd never actually see when you need to use it.
	StandardStep(builder(CooldownType.PARTY_BUFF, true, 0x3e7d).noAutoBuffs()),
	// TODO: there's a lvl 82 trait that might affect this
	// TODO: tech step is weird. The buff isn't applied by step, but rather by finish, but the cooldown starts when
	// you step.
	// It looks like if you fail to complete all the steps, it's still the same buff ID.
	TechnicalStep(builder(CooldownType.PARTY_BUFF, true, 0x3e7e).buffIds(0x71e)),
	Devilment(true, CooldownType.PARTY_BUFF, 0x3e8b),
	ShieldSamba(builder(CooldownType.PARTY_MIT, true, 0x3e8c).cooldown(90)),
	CuringWaltz(builder(CooldownType.HEAL, false, 0x3E8F)),
	EnAvant(builder(CooldownType.PERSONAL_UTILITY, false, 0x3E8A).maxCharges(3)),
	Flourish(builder(CooldownType.PERSONAL_BURST, true, 0x3E8D)),

	// BRD
	Troubadour(builder(CooldownType.PARTY_MIT, true, 0x1ced).cooldown(90)),
	MagesBallad(builder(CooldownType.PARTY_BUFF, true, 0x72).buffIds(0x8a9).duration(45.0)),
	ArmysPaeon(builder(CooldownType.PARTY_BUFF, true, 0x74).buffIds(0x8aa).duration(45.0)),
	WanderersMinuet(builder(CooldownType.PARTY_BUFF, true, 0xde7).buffIds(0x8a8).duration(45.0)),
	Barrage(builder(CooldownType.PERSONAL_BURST, true, 0x6B)),
	Minne(builder(CooldownType.HEAL, false, 0x1CF0)),
	RagingStrikes(builder(CooldownType.PERSONAL_BURST, true, 0x65)),
	Sidewinder(builder(CooldownType.PERSONAL_BURST, true, 0xDEA)),
	Empyreal(builder(CooldownType.PERSONAL_BURST, true, 0xDE6)),

	//	MagesBallad(BRD, true, 120.0, 45.0, "Mage's Ballad", CooldownType.PARTY_BUFF, 0x72, 0x8a9),
//	ArmysPaeon(BRD, true, 120.0, 45.0, "Army's Paeon", CooldownType.PARTY_BUFF, 0x74, 0x8aa),
//	WanderersMinuet(BRD, true, 120.0, 45.0, "Wanderer's Minuet", CooldownType.PARTY_BUFF, 0xde7, 0x8a8),
	BattleVoice(true, CooldownType.PARTY_BUFF, 0x76),
	RadiantFinale(true, CooldownType.PARTY_BUFF, 0x64B9),

	// MCH
	Tactician(builder(CooldownType.PARTY_MIT, true, 0x41f9).cooldown(90)),
	BarrelStabilizer(builder(CooldownType.PERSONAL_BURST, true, 0x1CF6)),
	Chainsaw(builder(CooldownType.PERSONAL_BURST, true, 0x64BC)),
	DrillBio(builder(CooldownType.PERSONAL_BURST, true, 0x4072, 0x4073).maxCharges(2)),
	Flamethrower(builder(CooldownType.PERSONAL_BURST, false, 0x1CFA)),
	HotShotAirAnchor(builder(CooldownType.PERSONAL_BURST, true, 0xB38, 0x4074)),
	Reassemble(builder(CooldownType.PERSONAL_BURST, true, 0xB3C)),
	Wildfire(builder(CooldownType.PERSONAL_BURST, true, 0xB3E)),

	;

	private final ExtendedCooldownDescriptor delegate;

	Cooldown(CdBuilder builder) {
		this.delegate = builder.build();
	}

	Cooldown(boolean defaultPersOverlay, CooldownType type, long abilityId) {
		this(builder(type, defaultPersOverlay, abilityId));
	}

	private static CdBuilder builder(CooldownType type, boolean defaultPersOverlay, long... abilityIds) {
		return new CdBuilder(type, defaultPersOverlay, abilityIds);
	}

	@Override
	public @Nullable Job getJob() {
		return delegate.getJob();
	}

	@Override
	public @Nullable JobType getJobType() {
		return delegate.getJobType();
	}

	@Override
	public boolean defaultPersOverlay() {
		return delegate.defaultPersOverlay();
	}

	@Override
	public String getSettingKeyStub() {
		return name();
	}

	@Override
	public Collection<Long> getAllRelevantAbilityIds() {
		return delegate.getAllRelevantAbilityIds();
	}

	@Override
	public String getLabel() {
		return delegate.getLabel();
	}

	@Override
	public boolean abilityIdMatches(long abilityId) {
		return delegate.abilityIdMatches(abilityId);
	}

	@Override
	public List<CdAuxAbility> getAuxAbilities() {
		return delegate.getAuxAbilities();
	}

	@Override
	public @Nullable CdAuxAbility auxMatch(long abilityId) {
		return delegate.auxMatch(abilityId);
	}

	@Override
	public boolean buffIdMatches(long buffId) {
		return delegate.buffIdMatches(buffId);
	}

	@Override
	public double getCooldown() {
		return delegate.getCooldown();
	}

	@Override
	public Duration getCooldownAsDuration() {
		return delegate.getCooldownAsDuration();
	}

	@Override
	public long getPrimaryAbilityId() {
		return delegate.getPrimaryAbilityId();
	}

	@Override
	public int getMaxCharges() {
		return delegate.getMaxCharges();
	}

	@Override
	public @Nullable Double getDurationOverride() {
		return delegate.getDurationOverride();
	}

	@Override
	public boolean autoBuffs() {
		return delegate.autoBuffs();
	}

	@Override
	public boolean noStatusEffect() {
		return delegate.noStatusEffect();
	}
}
