package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public enum Cooldown implements ExtendedCooldownDescriptor {

	// DO NOT change enum member names - they are used as settings keys

	// List of ALL buffs to track - WL/BL will be done by user settings
	// TANKS
	Rampart(builder(CooldownType.PERSONAL_MIT, true, 0x1d6b)),
	Reprisal(builder(CooldownType.PERSONAL_MIT, true, 0x1d6f)),
	ArmsLength(builder(CooldownType.PERSONAL_UTILITY, true, 0x1d7c)),
//	ArmsLength(TANK, true, 120.0, "Arm's Length", CooldownType.PERSONAL_UTILITY, 0x1d7c, 1209),

	//	HallowedGround(PLD, true, 420.0, "Hallowed Ground", CooldownType.INVULN, 0x1e, 82),
	HallowedGround(builder(CooldownType.INVULN, true, 0x1e)),
	Sentinel(builder(CooldownType.PERSONAL_MIT, true, 0x11)),
	//	Sentinel(PLD, true, 120.0, "Sentinel", CooldownType.PERSONAL_MIT, 0x11, 74),
//	Sentinel(builder(CooldownType.PERSONAL_MIT, true, 120.0, "Sentinel", CooldownType.PERSONAL_MIT, 0x11, 74),
	Cover(builder(CooldownType.PERSONAL_MIT, true, 0x1b)),
	//Cover(PLD, true, 120.0, "Cover", CooldownType.PERSONAL_MIT, 0x1b, 80),
	// TODO: sheltron/holy sheltron
	FightOrFlight(builder(CooldownType.PERSONAL_BURST, true, 0x14)),
	//	FightOrFlight(PLD, true, 60.0, "Fight or Flight", CooldownType.PERSONAL_BURST, 0x14, 0x4c),
	Requiescat(builder(CooldownType.PERSONAL_BURST, true, 0x1CD7)),
	//	Requiescat(PLD, true, 60.0, "Requiescat", CooldownType.PERSONAL_BURST, 0x1CD7, 0x558),
	// TODO: check auto with this
	DivineVeil(builder(CooldownType.PARTY_MIT, true, 0xdd4).buffIds(726, 727)),
	//	DivineVeil(PLD, true, 90.0, "Divine Veil", CooldownType.PARTY_MIT, 0xdd4, 726, 727),
	// TODO: buff
	PassageofArms(true, CooldownType.PARTY_MIT, 0x1cd9),
	CircleOfScorn(true, CooldownType.PERSONAL_BURST, 0x17),
	Intervene(true, CooldownType.PERSONAL_BURST, 0x404D),
	Expiacion(true, CooldownType.PERSONAL_BURST, 0x6493),
	Nebula(true, CooldownType.PERSONAL_MIT, 0x3f14),
	Aurora(true, CooldownType.HEAL, 0x3f17),
	Superbolide(true, CooldownType.INVULN, 0x3f18),
	HeartofLight(true, CooldownType.PARTY_MIT, 0x3f20),
	HeartofStone(true, CooldownType.PARTY_MIT, 0x3f21),
	HeartofCorundum(true, CooldownType.PARTY_MIT, 25758),
	Camouflage(true, CooldownType.PERSONAL_MIT, 0x3f0c),
	//	NascentFlash(WAR, true, 25.0, "Nascent Flash", CooldownType.HEAL, 0x4050, 1857, 1858),
	InnerRelease(true, CooldownType.PERSONAL_BURST, 0x1CDD),
	ThrillofBattle(true, CooldownType.PERSONAL_MIT, 0x28),
	Holmgang(true, CooldownType.INVULN, 0x2b),
	Vengeance(true, CooldownType.PERSONAL_MIT, 0x2c),
	RawIntuition(builder(CooldownType.PERSONAL_MIT, true, 0xddfL, 0x4050, 0x6497).buffIds(735L, 1857, 1858, 0xA76L)),
	Upheaval(builder(CooldownType.PERSONAL_BURST, true, 0x1CDB, 0x6498)),
	ShakeItOff(true, CooldownType.PARTY_MIT, 0x1cdc),
	Equilibrium(false, CooldownType.HEAL, 0xDE0),
	// TODO: infuriate requires CD reduction support
	// TODO: level based modifications
	Onslaught(builder(CooldownType.PERSONAL_UTILITY, true, 0x1CDA).maxCharges(3)),
//	Onslaught(WAR, true, 30.0, 3, "Onslaught", CooldownType.PERSONAL_UTILITY, 0x1CDA),

	DarkMissionary(true, CooldownType.PARTY_MIT, 0x4057),
	DarkMind(true, CooldownType.PERSONAL_MIT, 0xe32),
	ShadowWall(true, CooldownType.PERSONAL_MIT, 0xe34),
	LivingDead(true, CooldownType.INVULN, 0xe36),
	TheBlackestNight(true, CooldownType.PERSONAL_MIT, 0x1ce1),
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
	LucidDreaming(false, CooldownType.PERSONAL_UTILITY, 0x1D8A),
	// TODO - check ability ID
	Benediction(true, CooldownType.HEAL, 0x8c),
	Temperance(true, CooldownType.PARTY_MIT, 0x4098),
	Pom(true, CooldownType.PERSONAL_BURST, 0x88),
	// TODO: check auto buffs
	Asylum(builder(CooldownType.HEAL, true, 0xDF1)),
	//	Asylum(WHM, true, 45.0, "Asylum", CooldownType.HEAL, 0xDF1, 0x777),
	Aquaveil(true, CooldownType.PERSONAL_MIT, 0x6505),
	Bell(true, CooldownType.HEAL, 0x6506),
	Assize(true, CooldownType.PERSONAL_BURST, 0xdf3),
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
	ChainStratagem(true, CooldownType.PARTY_BUFF, 0x1d0c),
	Protraction(true, CooldownType.HEAL, 0x650b),
	Aetherflow(true, CooldownType.PERSONAL_UTILITY, 0xa6),
	Recitation(true, CooldownType.HEAL, 0x409E),
	//	DeploymentTactics(SCH, false, 90.0, "Deployment Tactics", CooldownType.PARTY_MIT, 0xE01),
	DeploymentTactics(builder(CooldownType.PARTY_MIT, false, 0xE01).cooldown(90)),
	EmergencyTactics(false, CooldownType.PARTY_MIT, 0xE02),
	// Sage stuff
//	Phlegma(SGE, true, 45.0, 2, "Phlegma", CooldownType.PERSONAL_BURST, 0x5EF9),
	// TODO: revisit the automatic naming stuff
	Phlegma(builder(CooldownType.PERSONAL_BURST, true, 24313, 24307, 24289).name("Phlegma")),
	Krasis(true, CooldownType.HEAL, 0x5EFD),
	Pepsis(true, CooldownType.HEAL, 0x5EED),
	Rhizomata(true, CooldownType.PERSONAL_UTILITY, 0x5EF5),
	Kerachole(true, CooldownType.PARTY_MIT, 0x5EEA),
	Soteria(true, CooldownType.HEAL, 0x5EE6),
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

	Divination(true, CooldownType.PARTY_BUFF, 0x40a8),
	Draw(builder(CooldownType.PARTY_BUFF, true, 0xE06).maxCharges(2).noAutoBuffs()),
	//	Draw(AST, true, 30.0, 2, "Draw", CooldownType.PARTY_BUFF, 0xE06),
	MinorArcana(true, CooldownType.PARTY_BUFF, 0x1D13),
	//	MinorArcana(AST, true, 60.0, "Minor Arcana", CooldownType.PARTY_BUFF, 0x1D13),
	// TODO: active status on charge-based abilities?
	CelInt(builder(CooldownType.HEAL, true, 0x40AC).maxCharges(2)),
	//	CelInt(AST, true, 30.0, 2, "Celestial Intersection", CooldownType.HEAL, 0x40AC),
	// Star is an interesting one due to having two stages - perhaps this would be a good use of making a 4th bar color (maybe purple)?
	Copp(true, CooldownType.HEAL, 0x40A9),
	Lightspeed(false, CooldownType.PERSONAL_UTILITY, 0xE16),
	// TODO: synastry
	Edig(builder(CooldownType.HEAL, false, 0xE1E).maxCharges(2)),
	//	Edig(AST, false, 40.0, 2, "Essential Dignity", CooldownType.HEAL, 0xE1E),
	Horoscope(builder(CooldownType.HEAL, false, 0x40AD).buffIds(0x762, 0x763)),
	Star(builder(CooldownType.HEAL, true, 0x1D0F).buffIds(0x4C8, 0x4E0)),
	// Not sure if I should actually put a buff in for this one, since there's three, and they all mean something slightly different
	Cunc(false, CooldownType.PARTY_MIT, 0xE1D),
	Exaltation(false, CooldownType.SINGLE_TARGET_MIT, 0x6511),
	Neutral(false, CooldownType.HEAL, 0x40AF),
	Macrocosmos(false, CooldownType.HEAL, 0x6512),


	// MELEE
	Feint(true, CooldownType.PARTY_MIT, 0x1d7d),
	//	TrickAttack(NIN, true, 60.0, "Trick Attack", CooldownType.PARTY_BUFF, 0x8d2, 638),
	// TODO
	TrickAttackNew(true, CooldownType.PERSONAL_BURST, 0x8d2),
	// TODO
	Mug(true, CooldownType.PARTY_BUFF, 0x8C8),
	BattleLitany(true, CooldownType.PARTY_BUFF, 0xde5),
	Brotherhood(true, CooldownType.PARTY_BUFF, 0x1ce4),
	RiddleOfWind(true, CooldownType.PERSONAL_BURST, 0x64A6),
	DragonSight(true, CooldownType.PARTY_BUFF, 0x1ce6),
	ArcaneCircle(true, CooldownType.PARTY_BUFF, 0x5F55),
	ArcaneCrest(true, CooldownType.PERSONAL_MIT, 0x5F54),


	// CASTER
	Addle(true, CooldownType.PARTY_MIT, 0x1d88),
	Swiftcast(true, CooldownType.PERSONAL_UTILITY, 0x1d89),
	Embolden(true, CooldownType.PARTY_BUFF, 0x1d60),
	Manafication(builder(CooldownType.PERSONAL_BURST, true, 0x1D61).cooldown(110.0)),
	//	Manafication(RDM, true, 110.0, "Manafication", CooldownType.PERSONAL_BURST, 0x1D61, 0x7b3),
	MagicBarrier(true, CooldownType.PARTY_MIT, 0x6501),
	Acceleration(builder(CooldownType.PERSONAL_BURST, true, 0x1D5E).maxCharges(2)),
	//	Acceleration(RDM, true, 55.0, 2, "Acceleration", CooldownType.PERSONAL_BURST, 0x1D5E, 0x4D6),
	Fleche(true, CooldownType.PERSONAL_BURST, 0x1D5D),
	ContreSixte(builder(CooldownType.PERSONAL_BURST, true, 0x1D5F).cooldown(35.0)),
	//	ContreSixte(RDM, true, 35.0, "Contre Sixte", CooldownType.PERSONAL_BURST, 0x1D5F),
	// TODO: can't add these yet because they are charges
//	CorpsACorps(RDM, true, 35.0, "Corps-a-Corps", CooldownType.PERSONAL_BURST, 0x1D52),
//	Displacement(RDM, true, 35.0, "Displace/Engage", CooldownType.PERSONAL_BURST, new long[]{0x1D5B, 0x408F}, new long[]{}),
	SearingLight(true, CooldownType.PARTY_BUFF, 25801),


	// PHYS RANGED
	// Specifically NOT providing buff IDs for standard step, since you'd want to use it off CD for damage, and
	// the duration is longer than the CD, so you'd never actually see when you need to use it.
	StandardStep(builder(CooldownType.PARTY_BUFF, true, 0x3e7d).noAutoBuffs()),
	// TODO: there's a lvl 82 trait that might affect this
	// TODO: tech step is weird. The buff isn't applied by step, but rather by finish, but the cooldown starts when
	// you step.
	// It looks like if you fail to complete all the steps, it's still the same buff ID.
	TechnicalStep(builder(CooldownType.PARTY_BUFF, true, 0x3e7e).buffIds(0x71e)),
	Devilment(true, CooldownType.PARTY_BUFF, 0x3e8b),
	ShieldSamba(true, CooldownType.PARTY_MIT, 0x3e8c),
	Troubadour(true, CooldownType.PARTY_MIT, 0x1ced),
	MagesBallad(builder(CooldownType.PARTY_BUFF, true, 0x72).buffIds(0x8a9).duration(45.0)),
	ArmysPaeon(builder(CooldownType.PARTY_BUFF, true, 0x74).buffIds(0x8aa).duration(45.0)),
	WanderersMinuet(builder(CooldownType.PARTY_BUFF, true, 0xde7).buffIds(0x8a8).duration(45.0)),

	//	MagesBallad(BRD, true, 120.0, 45.0, "Mage's Ballad", CooldownType.PARTY_BUFF, 0x72, 0x8a9),
//	ArmysPaeon(BRD, true, 120.0, 45.0, "Army's Paeon", CooldownType.PARTY_BUFF, 0x74, 0x8aa),
//	WanderersMinuet(BRD, true, 120.0, 45.0, "Wanderer's Minuet", CooldownType.PARTY_BUFF, 0xde7, 0x8a8),
	BattleVoice(true, CooldownType.PARTY_BUFF, 0x76),
	RadiantFinale(true, CooldownType.PARTY_BUFF, 0x64B9),
	Tactician(true, CooldownType.PARTY_MIT, 0x41f9);


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
	public String getLabel() {
		return delegate.getLabel();
	}

	@Override
	public boolean abilityIdMatches(long abilityId) {
		return delegate.abilityIdMatches(abilityId);
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
}
