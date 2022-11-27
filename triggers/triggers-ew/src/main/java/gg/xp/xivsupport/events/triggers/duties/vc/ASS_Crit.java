package gg.xp.xivsupport.events.triggers.duties.vc;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.misc.pulls.ForceCombatEnd;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ScanMe
@CalloutRepo(name = "Another Sil'Dihn Subterrane (Criterion/Savage)", duty = KnownDuty.ASS_Criterion)
public class ASS_Crit extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(ASS_Crit.class);

	// First trash
	@NpcCastCallout({0x7960, 0x7978})
	private final ModifiableCallout<AbilityCastStart> atropineSpore = ModifiableCallout.durationBasedCall("Atropine Spore", "Big Donut");
	@NpcCastCallout({0x7962, 0x797A})
	private final ModifiableCallout<AbilityCastStart> deracinator = ModifiableCallout.durationBasedCall("Deracinator", "Buster on {event.target}");
	@NpcCastCallout({0x7963, 0x797B})
	private final ModifiableCallout<AbilityCastStart> rightSweep = ModifiableCallout.durationBasedCall("Right Sweep", "Left");
	@NpcCastCallout({0x7964, 0x797C})
	private final ModifiableCallout<AbilityCastStart> leftSweep = ModifiableCallout.durationBasedCall("Left Sweep", "Right");
	@NpcCastCallout({0x79650, 0x797D})
	private final ModifiableCallout<AbilityCastStart> creepingIvy = ModifiableCallout.durationBasedCall("Creeping Ivy", "Out of Front");
	@NpcCastCallout({0x795B, 0x7973})
	private final ModifiableCallout<AbilityCastStart> honeyedLeft = ModifiableCallout.durationBasedCall("Honeyed Left", "Right");
	@NpcCastCallout({0x795C, 0x7974})
	private final ModifiableCallout<AbilityCastStart> honeyedRight = ModifiableCallout.durationBasedCall("Honeyed Right", "Left");
	@NpcCastCallout({0x795D, 0x7975})
	private final ModifiableCallout<AbilityCastStart> honeyedFront = ModifiableCallout.durationBasedCall("Honeyed Front", "Out of Front");
	@NpcCastCallout({0x7957, 0x796F})
	private final ModifiableCallout<AbilityCastStart> arborealStorm = ModifiableCallout.durationBasedCall("Arboreal Storm", "Out");

	// First boss
	private final ModifiableCallout<AbilityCastStart> cardSafe = ModifiableCallout.durationBasedCall("Soap's Up: Cardinal Safe", "Cardinal Safe");
	private final ModifiableCallout<AbilityCastStart> dustBluster = ModifiableCallout.durationBasedCall("Dust Bluster", "Knockback");
	private final ModifiableCallout<AbilityCastStart> squeakyCleanLeftSafe = ModifiableCallout.durationBasedCall("Squeaky Clean: Left Safe", "Left");
	private final ModifiableCallout<AbilityCastStart> squeakyCleanRightSafe = ModifiableCallout.durationBasedCall("Squeaky Clean: Right Safe", "Right");
	private final ModifiableCallout<AbilityCastStart> lineStack = ModifiableCallout.durationBasedCall("Line Stack", "Line Stack");
	private final ModifiableCallout<AbilityCastStart> keepMoving = ModifiableCallout.durationBasedCall("Keep Moving", "Keep Moving");
	private final ModifiableCallout<AbilityCastStart> slipperyKB = ModifiableCallout.durationBasedCall("Knockback (Slippery Soap)", "Knockback");
	private final ModifiableCallout<AbilityCastStart> intercardSafe = ModifiableCallout.durationBasedCall("Intercard Safe", "Intercard Safe");
	private final ModifiableCallout<AbilityCastStart> carpetBeater = ModifiableCallout.durationBasedCall("Carpet Beater", "Buster on {event.target}");
	private final ModifiableCallout<AbilityCastStart> underGreen = ModifiableCallout.durationBasedCall("Under Green", "Get Under Green");
	private final ModifiableCallout<AbilityCastStart> underBoss = ModifiableCallout.durationBasedCall("Under Green", "Get Under Boss");
	private final ModifiableCallout<AbilityCastStart> totalWash = ModifiableCallout.durationBasedCall("Total Wash", "Raidwide with Bleed");
	private final ModifiableCallout<AbilityCastStart> fp1_begin = new ModifiableCallout<>("Fresh Puff 1: Begin", "Three Puffs");
	private final ModifiableCallout<AbilityCastStart> fp2_begin = new ModifiableCallout<>("Fresh Puff 2: Begin", "Four Puffs and Tethers");
	private final ModifiableCallout<TetherEvent> fp2_blueTether = new ModifiableCallout<>("Fresh Puff 2: Blue Tether", "{where} Blue Tether");
	private final ModifiableCallout<TetherEvent> fp2_yellowTether = new ModifiableCallout<>("Fresh Puff 2: Yellow Tether", "{where} Yellow Tether");
	private final ModifiableCallout<AbilityCastStart> fp3_begin = new ModifiableCallout<>("Fresh Puff 3: Begin", "Eight Puffs");
	private final ModifiableCallout<AbilityCastStart> fp3_dodgeOrbs = new ModifiableCallout<>("Fresh Puff 3: Dodge First Set", "Dodge Glowing Orb Patterns");
	private final ModifiableCallout<AbilityCastStart> fp3_dodgeOrbs2 = new ModifiableCallout<>("Fresh Puff 3: Dodge Second Set", "Under Boss, Dodge Orb Patterns");
	private final ModifiableCallout<AbilityCastStart> fp4_begin = new ModifiableCallout<>("Fresh Puff 3: Begin", "Four Puffs and Tethers");
	private final ModifiableCallout<BuffApplied> forkedLightning = ModifiableCallout.<BuffApplied>durationBasedCall("Forked Lightning", "Spread").autoIcon();


	private final XivState state;
	private final StatusEffectRepository buffs;

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	private XivState getState() {
		return state;
	}

	private final ArenaPos firstBossArena = new ArenaPos(-335.01, -155.02, 5, 5);

	/*
		Notes
		Boss NPC color buffs: Yellow 0xCE3, Blue 0xCE2, Green 0xCE1
		Fake NPC color buffs: Yellow 0xCEB, Blue 0xCEA, Green 0xCE9
	 */
	private static final int bossG = 0xCE1;
	private static final int bossB = 0xCE2;
	private static final int bossY = 0xCE3;
	private static final int fakeG = 0xCE9;
	private static final int fakeB = 0xCEA;
	private static final int fakeY = 0xCEB;

	private enum Color {
		GREEN,
		BLUE,
		YELLOW
	}

	private @Nullable Color buffColor(XivCombatant cbt) {
		return buffs.statusesOnTarget(cbt)
				.stream()
				.map(ba -> switch ((int) ba.getBuff().getId()) {
					case bossG, fakeG -> Color.GREEN;
					case bossB, fakeB -> Color.BLUE;
					case bossY, fakeY -> Color.YELLOW;
					default -> null;
				})
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	public ASS_Crit(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@HandleEvents
	public void basicCastFirstBoss(EventContext ctx, AbilityCastStart acs) {
		if (!acs.getSource().npcIdMatches(14834, 14837)) {
			return;
		}
		@Nullable Color color = buffColor(acs.getSource());
		ModifiableCallout<AbilityCastStart> call = switch ((int) acs.getAbility().getId()) {
			case 0x775a, 0x777d -> {
				if (color == Color.YELLOW) {
					yield cardSafe;
				}
				yield null;
			}
			case 0x776c, 0x778f -> dustBluster;
			case 0x774F, 0x7772 -> carpetBeater;
			case 0x7750, 0x7773 -> totalWash;
			default -> null;
		};
		if (call != null) {
			ctx.accept(call.getModified(acs));
		}
	}

	private final RepeatSuppressor greenSupp = new RepeatSuppressor(Duration.ofSeconds(2));

	@HandleEvents
	public void fakeCasts(EventContext ctx, AbilityCastStart acs) {
		if (acs.abilityIdMatches(0x7755, 0x7778)) {
			ctx.accept(squeakyCleanLeftSafe.getModified(acs));
		}
		else if (acs.abilityIdMatches(0x7756, 0x7779)) {
			ctx.accept(squeakyCleanRightSafe.getModified(acs));
		}
		else if (acs.abilityIdMatches(0x776B, 0x778A) && buffColor(acs.getSource()) == Color.GREEN) {
			if (greenSupp.check(acs)) {
				ctx.accept(underGreen.getModified(acs));
			}
		}
	}

	@HandleEvents
	public void debuffs(EventContext ctx, BuffApplied ba) {
		if (ba.buffIdMatches(0x24B) && ba.getTarget().isThePlayer()) {
			ctx.accept(forkedLightning.getModified(ba));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> combatEndHack = SqtTemplates.sq(10_000,
			InCombatChangeEvent.class, event -> !event.isInCombat(),
			(e1, s) -> {
				// TODO: this doesn't work as well as I'd like
				s.waitMs(3000);
				s.accept(new ForceCombatEnd());
			}
	);

	@AutoFeed
	public SequentialTrigger<BaseEvent> slipperySoap = SqtTemplates.sq(20_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x775E, 7781),
			(e1, s) -> {
				// TODO logic for which one to point the stack at
				// If there's a blue on the bottom row, you need to hit the opposite side so that N is safe
				// If there's a yellow on the bottom row, you need to hit N so that the opposite south corner is afe
				s.updateCall(lineStack.getModified(e1));
				// TODO: is this always the right call here? Or should this method be split out into other sequentials?
				Color color = buffColor(e1.getSource());
				if (color == Color.BLUE) {
					s.waitMs(2_000);
					s.updateCall(keepMoving.getModified(e1));
					s.waitMs(2_800);
					s.updateCall(intercardSafe.getModified(e1));
				}
				else if (color == Color.YELLOW) {
					// Spread call already handled
					s.waitMs(4_000);
					s.updateCall(cardSafe.getModified(e1));
				}
				else if (color == Color.GREEN) {
					s.waitMs(2_000);
					s.updateCall(slipperyKB.getModified(e1));
					s.waitMs(2_800);
					s.updateCall(underBoss.getModified(e1));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> freshPuff = SqtTemplates.multiInvocation(90_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7766, 0x7789),
			(e1, s) -> {
				// First invocation: handled by other triggers already
				s.updateCall(fp1_begin.getModified(e1));
			},
			(e1, s) -> {
				s.updateCall(fp2_begin.getModified(e1));
				// Second invocation: four puffs, with tethers
				TetherEvent tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				XivCombatant puff = tether.getTargetMatching(cbt -> !cbt.isPc());
				if (puff == null) {
					log.error("Null puff!");
				}
				else {
					Color color = buffColor(puff);
					ArenaSector arenaSector = firstBossArena.forCombatant(puff);
					Map<String, Object> params = Map.of("where", arenaSector);
					if (color == Color.BLUE) {
						s.updateCall(fp2_blueTether.getModified(tether, params));
					}
					else if (color == Color.YELLOW) {
						s.updateCall(fp2_yellowTether.getModified(tether, params));
					}
				}
				AbilityCastStart bossMechanic = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7767, 0x778A));
				Color bossColor = buffColor(bossMechanic.getSource());
				if (bossColor == Color.BLUE) {
					s.updateCall(intercardSafe.getModified(bossMechanic));
				}
				else if (bossColor == Color.GREEN) {
					s.updateCall(underBoss.getModified(bossMechanic));
				}
			},
			(e1, s) -> {
				s.updateCall(fp3_begin.getModified(e1));
				// Eight puffs, six get cleaned
				AbilityCastStart bossCast1 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7767, 0x778A));
				// TODO: identify orb casts and give instruction
				// This is currently gated because the green callout is already handled
				if (getState().getCombatantsListCopy().stream()
						.map(this::buffColor)
						.filter(Objects::nonNull)
						.noneMatch(color -> color == Color.GREEN)) {
					s.updateCall(fp3_dodgeOrbs.getModified(bossCast1));
				}
				AbilityCastStart bossCast2 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7767, 0x778A));
				s.updateCall(fp3_dodgeOrbs2.getModified(bossCast2));
			},
			(e1, s) -> {
				s.updateCall(fp4_begin.getModified(e1));
				// Four puffs, with tethers
			}
	);


	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.ASS_Criterion) || state.dutyIs(KnownDuty.ASS_Savage);
	}

//	@HandleEvents
//	public void handleWipes(EventContext ctx, InCombatChangeEvent icce) {
//		// Need a bit of a workaround here because the normal wipes don't work
//		if (!icce.isInCombat()) {
//		}
//	}
//

	// Second trash
	@NpcCastCallout({0x796C, 0x7984})
	private final ModifiableCallout<AbilityCastStart> hellsNebula = ModifiableCallout.durationBasedCall("Hells' Nebula", "1 HP");
	@NpcCastCallout({0x796B, 0x7983})
	private final ModifiableCallout<AbilityCastStart> infernalWeight = ModifiableCallout.durationBasedCall("Infernal Weight", "Raidwide and Heavy");
	@NpcCastCallout({0x796A, 0x7982})
	private final ModifiableCallout<AbilityCastStart> dominionSlash = ModifiableCallout.durationBasedCall("Dominion Slash", "Out of Front");
	@NpcCastCallout({0x7969, 0x7981})
	private final ModifiableCallout<AbilityCastStart> infernalPain = ModifiableCallout.durationBasedCall("Infernal Pain", "Raidwide with Bleed");
	@NpcCastCallout({0x7966, 0x797E})
	private final ModifiableCallout<AbilityCastStart> blightedGloom = ModifiableCallout.durationBasedCall("Blighted Gloom", "Out");
	@NpcCastCallout({0x7968, 0x7980})
	private final ModifiableCallout<AbilityCastStart> kingsWill = ModifiableCallout.durationBasedCall("King's Will", "Heavy Autos");

	// Second boss
	@NpcCastCallout({0x7671, 0x77B3})
	private final ModifiableCallout<AbilityCastStart> flashOfSteel = ModifiableCallout.durationBasedCall("Flash of Steel", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> rushOfMight = ModifiableCallout.durationBasedCall("Rush of Might Windup", "Inside Lines, Watch Charges");
	private final ModifiableCallout<?> rushOfMightFollowup = new ModifiableCallout<>("Rush of Might Followup", "Move Out");
	@NpcCastCallout({0x766c, 0x77AE})
	private final ModifiableCallout<AbilityCastStart> sculptorsPassion = ModifiableCallout.durationBasedCall("Sculptor's Passion", "Line Stack, Tank in Front");
	@NpcCastCallout({0x7672, 0x77B4})
	private final ModifiableCallout<AbilityCastStart> mightySmite = ModifiableCallout.durationBasedCall("Mighty Smite", "Buster on {event.target}");

	private final ModifiableCallout<BuffApplied> curseOfTheFallen_stackSpread = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Stack then Spread", "Stack on {stack} then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_spreadStack = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Spread then Stack", "Spread then Stack on {stack}").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_baitSpread = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Bait then Spread", "Bait then Spread").autoIcon();
	// Is this possible?
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_spreadBait = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Spread then Bait", "Spread then Bait").autoIcon();

	private final ModifiableCallout<BuffApplied> curseOfTheFallen_thenStack = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Stack Followup", "Stack on {stack}").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_thenSpread = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Spread Followup", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_thenBait = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Bait Followup", "Bait").autoIcon();

	private final ModifiableCallout<AbilityCastStart> checkerboard1 = ModifiableCallout.durationBasedCall("Wrath of Ruin 1", "Avoid Lines and Orbs");
	private final ModifiableCallout<AbilityCastStart> checkerboard2_2gold = ModifiableCallout.<AbilityCastStart>durationBasedCall("Wrath of Ruin 2: 2 Gold", "Get Hit By Two Silver Orbs").statusIcon(0xCDF, 2);
	private final ModifiableCallout<AbilityCastStart> checkerboard2_2silver = ModifiableCallout.<AbilityCastStart>durationBasedCall("Wrath of Ruin 2: 2 Silver", "Get Hit By Two Gold Orbs").statusIcon(0xCE0, 2);
	private final ModifiableCallout<AbilityCastStart> checkerboard2_1each = ModifiableCallout.<AbilityCastStart>durationBasedCall("Wrath of Ruin 2: 1 of Each", "Get Hit By Silver and Gold").statusIcon(0xCE0, 1);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> rushOfMightSq = SqtTemplates.beginningAndEndingOfCast(
			// TODO: Find IDs, see if there's any additional data from them. However, it's likely that these three alone are short/medium/long
			acs -> acs.abilityIdMatches(0x7658, 0x7659, 0x765A, 0x779A, 0x779B, 0x779C),
			rushOfMight, rushOfMightFollowup);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> curseOfTheFallenSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7674, 0x77B6),
			(e1, s) -> {
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7674, 0x77B6));
				// Curse of the fallen cast itself does nothing
				EventCollector<BuffApplied> spreads = new EventCollector<>(ba -> ba.buffIdMatches(0xCDA));
				EventCollector<BuffApplied> stacks = new EventCollector<>(ba -> ba.buffIdMatches(0xCDD));
				EventCollector<BuffApplied> baits = new EventCollector<>(ba -> ba.buffIdMatches(0xCDC));
				s.collectEvents(6, 2_500, BuffApplied.class, true, List.of(spreads, stacks, baits));

				BuffApplied stack = stacks.getEvents().get(0);

				boolean spreadFirst = stack.getInitialDuration().toMillis() > 15_000;
				BuffApplied timingBasis = spreadFirst ? spreads.getEvents().get(0) : stack;

				RawModifiedCallout<BuffApplied> first;
				RawModifiedCallout<BuffApplied> second;

				Map<String, Object> params = Map.of("stack", stack.getTarget());

				if (baits.anyMatch(ba -> ba.getTarget().isThePlayer())) {
					if (spreadFirst) {
						first = curseOfTheFallen_spreadBait.getModified(spreads.getEvents().get(0), params);
						second = curseOfTheFallen_thenBait.getModified(baits.getEvents().get(0), params);
					}
					else {
						first = curseOfTheFallen_baitSpread.getModified(baits.getEvents().get(0), params);
						second = curseOfTheFallen_thenSpread.getModified(spreads.getEvents().get(0), params);
					}
				}
				else {
					if (spreadFirst) {
						first = curseOfTheFallen_spreadStack.getModified(spreads.getEvents().get(0), params);
						second = curseOfTheFallen_thenStack.getModified(stacks.getEvents().get(0), params);
					}
					else {
						first = curseOfTheFallen_stackSpread.getModified(stacks.getEvents().get(0), params);
						second = curseOfTheFallen_thenSpread.getModified(spreads.getEvents().get(0), params);
					}
				}
				s.updateCall(first);
				s.waitMs(timingBasis.getEstimatedRemainingDuration().toMillis());
				s.updateCall(second);
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> wrathOfRuin = SqtTemplates.multiInvocation(20_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7663, 0x77A5),
			(e1, s) -> {
				// First checkerboard, just dodge lines and orbs
				s.updateCall(checkerboard1.getModified(e1));
			}, (e1, s) -> {
				XivPlayerCharacter player = getState().getPlayer();
				AbilityCastStart orbCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7670, 0x77B2));
				// Second checkerboard, have to look at debuff
				int goldBuffStacks = getBuffs().buffStacksOnTarget(player, 0xCDF);
				int silverBuffStacks = getBuffs().buffStacksOnTarget(player, 0xCE0);
				// TODO: identify *where* someone can stand for each of these
				if (goldBuffStacks == 2) {
					// get hit by two silver
					s.updateCall(checkerboard2_2gold.getModified(orbCast));
				}
				else if (silverBuffStacks == 2) {
					s.updateCall(checkerboard2_2silver.getModified(orbCast));
				}
				else if (silverBuffStacks == 1 && goldBuffStacks == 1) {
					s.updateCall(checkerboard2_1each.getModified(orbCast));
				}
				else {
					s.updateCall(checkerboard1.getModified(e1));
				}
			});

	private final ModifiableCallout<AbilityCastStart> monument_centerForChain = ModifiableCallout.durationBasedCall("Monument: Wait Center for Chain", "Wait In Middle for Chain");
	private final ModifiableCallout<BuffApplied> monument_breakChain = ModifiableCallout.<BuffApplied>durationBasedCall("Monument: Break Chain", "Break Chain").autoIcon();
	private final ModifiableCallout<BuffApplied> monument_1out = new ModifiableCallout<BuffApplied>("Monument: First in Line, Out", "Stay Out").autoIcon();
	private final ModifiableCallout<BuffApplied> monument_1in = new ModifiableCallout<BuffApplied>("Monument: First in Line, In", "Soak Tower").autoIcon();
	private final ModifiableCallout<BuffApplied> monument_2in = new ModifiableCallout<BuffApplied>("Monument: Second in Line, In", "Soak Tower then Out").autoIcon();
	private final ModifiableCallout<BuffApplied> monument_2out = new ModifiableCallout<BuffApplied>("Monument: Second in Line, Out", "Out").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> curseOfTheMonumentSq = SqtTemplates.sq(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7666, 0x77A8),
			(e1, s) -> {
				s.updateCall(monument_centerForChain.getModified(e1));
				// Wait for chain
				BuffApplied chain = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xCDE));
				s.updateCall(monument_breakChain.getModified(chain));
				// Wait for first explosion
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7668, 0x77AA));
				@Nullable BuffApplied firstInLine = getBuffs().findStatusOnTarget(getState().getPlayer(), 0xBBC);
				@Nullable BuffApplied secondInLine = getBuffs().findStatusOnTarget(getState().getPlayer(), 0xBBD);
				if (firstInLine != null) {
					s.updateCall(monument_1out.getModified(firstInLine));
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x766A, 0x77AC));
					s.updateCall(monument_1in.getModified(firstInLine));
				}
				else if (secondInLine != null) {
					s.updateCall(monument_2in.getModified(secondInLine));
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x766A, 0x77AC));
					s.updateCall(monument_2out.getModified(secondInLine));
				}
			});

	// Third Boss
	@NpcCastCallout({0x74AF, 0x76C5})
	private final ModifiableCallout<AbilityCastStart> showOfStrength = ModifiableCallout.durationBasedCall("Show of Strength", "Raidwide");
	@NpcCastCallout({0x74AD, 0x76C4})
	private final ModifiableCallout<AbilityCastStart> firesteelFracture = ModifiableCallout.durationBasedCall("Firesteel Fracture", "Buster on {event.target}");
	private final ModifiableCallout<AbilityUsedEvent> firesteel_standInFront = new ModifiableCallout<>("Firesteel Strike: Cover", "Stand in Front");
	private final ModifiableCallout<AbilityUsedEvent> firesteel_standBehind = new ModifiableCallout<>("Firesteel Strike: Get Covered", "Stand Behind");
	private final ModifiableCallout<AbilityCastStart> infernBrand1 = ModifiableCallout.durationBasedCallWithoutDurationText("Infern Brand 1", "Watch Portals");
	private final ModifiableCallout<AbilityCastStart> infernBrand2 = ModifiableCallout.durationBasedCallWithoutDurationText("Infern Brand 2", "Cut Wires in Debuff Order");
	private final ModifiableCallout<BuffApplied> brand1 = new ModifiableCallout<BuffApplied>("Infern Brand First Set: #1", "First Brand", 40_000).autoIcon();
	private final ModifiableCallout<BuffApplied> brand2 = new ModifiableCallout<BuffApplied>("Infern Brand First Set: #2", "Second Brand", 40_000).autoIcon();
	private final ModifiableCallout<BuffApplied> brand3 = new ModifiableCallout<BuffApplied>("Infern Brand First Set: #3", "Third Brand", 40_000).autoIcon();
	private final ModifiableCallout<BuffApplied> brand4 = new ModifiableCallout<BuffApplied>("Infern Brand First Set: #4", "Fourth Brand", 40_000).autoIcon();
	private final ModifiableCallout<?> wireCut1_startingSpot = new ModifiableCallout<>("Wire Cutting: Starting Spot", "Start {startingCorner}");
	private final ModifiableCallout<AbilityCastStart> infernBrand3 = ModifiableCallout.durationBasedCallWithoutDurationText("Infern Brand 3", "Portals and Baits");
	private final ModifiableCallout<BuffApplied> portals_westCCW = new ModifiableCallout<>("Self Portal West CCW", "West Counterclockwise", 10_000);
	private final ModifiableCallout<BuffApplied> portals_westCW = new ModifiableCallout<>("Self Portal West CW", "West Clockwise", 10_000);
	private final ModifiableCallout<BuffApplied> portals_eastCCW = new ModifiableCallout<>("Self Portal East CCW", "East Counterclockwise", 10_000);
	private final ModifiableCallout<BuffApplied> portals_eastCW = new ModifiableCallout<>("Self Portal East CW", "East Clockwise", 10_000);
	private final ModifiableCallout<AbilityCastStart> infernBrand4 = ModifiableCallout.durationBasedCallWithoutDurationText("Infern Brand 4", "Watch Portals, Find Safe");
	private final ModifiableCallout<AbilityCastStart> infernBrand5 = ModifiableCallout.durationBasedCallWithoutDurationText("Infern Brand 5", "Wires and Baits");
	private final ModifiableCallout<BuffApplied> secondBrand1 = new ModifiableCallout<BuffApplied>("Infern Brand Second Set: #1", "Cut Wire 1 then Bait", 40_000).autoIcon();
	private final ModifiableCallout<BuffApplied> secondBrand2 = new ModifiableCallout<BuffApplied>("Infern Brand Second Set: #2", "Cut Wire 2 then Bait", 40_000).autoIcon();
	private final ModifiableCallout<BuffApplied> secondBrand3 = new ModifiableCallout<BuffApplied>("Infern Brand Second Set: #3", "Bait then Cut Wire 3", 40_000).autoIcon();
	private final ModifiableCallout<BuffApplied> secondBrand4 = new ModifiableCallout<BuffApplied>("Infern Brand Second Set: #4", "Bait then Cut Wire 4", 40_000).autoIcon();

	@AutoFeed
	public final SequentialTrigger<BaseEvent> firesteelStrikeSq = SqtTemplates.sq(20_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x74b0),
			(e1, s) -> {
				// If you got hit with the magic vuln, stand behind someone else.
				AbilityUsedEvent firstHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x74B1, 0x74B2, 0x76C6, 0x76C7) && aue.isFirstTarget());
				if (firstHit.getTarget().isThePlayer()) {
					s.updateCall(firesteel_standBehind.getModified(firstHit));
				}
				else {
					AbilityUsedEvent secondHit = s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x74B1, 0x74B2, 0x76C6, 0x76C7) && aue.isFirstTarget());
					if (secondHit.getTarget().isThePlayer()) {
						s.updateCall(firesteel_standBehind.getModified(secondHit));
					}
					else {
						s.updateCall(firesteel_standInFront.getModified(secondHit));
					}
				}
			});

	private enum WirePosition {
		// North/south from left to right
		NS1(ArenaSector.WEST), NS2(ArenaSector.WEST), NS3(ArenaSector.EAST), NS4(ArenaSector.EAST),
		// East/west from north to south
		EW1(ArenaSector.NORTH), EW2(ArenaSector.NORTH), EW3(ArenaSector.SOUTH), EW4(ArenaSector.SOUTH);

		final ArenaSector sector;

		WirePosition(ArenaSector sector) {
			this.sector = sector;
		}
	}

	private List<XivCombatant> getWiresRaw() {
		return state.getCombatantsListCopy().stream().filter(cbt -> (cbt.getbNpcId() == 14764 || cbt.getbNpcId() == 14818))
				.filter(cbt -> getWireNumber(cbt) > 0)
				.toList();
	}

	private int getWireNumber(XivCombatant wire) {
		int stacks = buffs.rawBuffStacksOnTarget(wire, 0x95D);
		return switch (stacks) {
			case 450, 454 -> 1;
			case 451, 455 -> 2;
			case 452, 456 -> 3;
			case 453, 457 -> 4;
			default -> -1;
		};
	}

	private Map<WirePosition, Integer> getWires1() {
		List<XivCombatant> wires = getWiresRaw();
		Map<WirePosition, Integer> wireNumbers = new EnumMap<>(WirePosition.class);
		for (XivCombatant wire : wires) {
			int num = getWireNumber(wire);
			WirePosition pos = switch ((int) Math.round(wire.getPos().x())) {
				case 286 -> WirePosition.NS1;
				case 288 -> WirePosition.NS2;
				case 290 -> WirePosition.NS3;
				case 292 -> WirePosition.NS4;
				default -> switch ((int) Math.round(wire.getPos().y())) {
					case -108 -> WirePosition.EW1;
					case -106 -> WirePosition.EW2;
					case -104 -> WirePosition.EW3;
					case -102 -> WirePosition.EW4;
					default -> null;
				};
			};
			if (pos != null) {
				Integer existing = wireNumbers.put(pos, num);
				if (existing != null) {
					throw new IllegalStateException("Duplicate wire position: " + wire.getPos());
				}
			}
		}
		return wireNumbers;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> infernBrandSq = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7491),
			(e1, s) -> {
				// Spinny no work in log :(
				s.updateCall(infernBrand1.getModified(e1));
			}, (e1, s) -> {
				s.updateCall(infernBrand2.getModified(e1));
				BuffApplied brandBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xCC4, 0xCC5, 0xCC6, 0xCC7) && ba.getTarget().isThePlayer());
				int brandNumber = (int) (brandBuff.getBuff().getId() - 0xCC3);
				log.info("IB2: Brand number: {}", brandNumber);
				ModifiableCallout<BuffApplied> brandCall;
				switch (brandNumber) {
					case 1 -> brandCall = brand1;
					case 2 -> brandCall = brand2;
					case 3 -> brandCall = brand3;
					case 4 -> brandCall = brand4;
					default -> {
						log.error("Invalid brand number {}! Buff: {}", brandBuff, brandBuff);
						return;
					}
				}
				s.accept(brandCall.getModified(brandBuff));
				s.waitThenRefreshCombatants(1_000);
				Map<WirePosition, Integer> wires = getWires1();
				if (wires.size() != 8) {
					throw new IllegalStateException("Did not find 8 wires! Found: " + wires);
				}
				log.info("IB2: Wires: {}", wires);
				List<ArenaSector> neededSides = wires.entrySet().stream()
						.filter(e -> e.getValue() == brandNumber)
						.map(Map.Entry::getKey)
						.peek(wp -> log.info("IB2: Need to cut {}", wp))
						.map(wp -> wp.sector).toList();
				log.info("IB2: Needed sides: {}", neededSides);
				ArenaSector combined = ArenaSector.tryCombineTwoCardinals(neededSides);
				s.updateCall(wireCut1_startingSpot.getModified(Map.of("startingCorner", combined == null ? ArenaSector.UNKNOWN : combined)));
			}, (e1, s) -> {
				s.updateCall(infernBrand3.getModified(e1));
				BuffApplied portalBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xB9A) && ba.getTarget().isThePlayer());
				ModifiableCallout<BuffApplied> call = switch ((int) portalBuff.getRawStacks()) {
					case 467 -> portals_westCCW;
					case 462 -> portals_westCW;
					case 461 -> portals_eastCCW;
					case 466 -> portals_eastCW;
					default -> throw new IllegalArgumentException("Not a valid buff stack count: " + portalBuff.getRawStacks());
				};
				s.updateCall(call.getModified(portalBuff));
			}, (e1, s) -> {
				s.updateCall(infernBrand4.getModified(e1));
			}, (e1, s) -> {
				s.updateCall(infernBrand5.getModified(e1));
				BuffApplied brandBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xCC4, 0xCC5, 0xCC6, 0xCC7) && ba.getTarget().isThePlayer());
				int brandNumber = (int) (brandBuff.getBuff().getId() - 0xCC3);
				log.info("IB2: Brand number: {}", brandNumber);
				ModifiableCallout<BuffApplied> brandCall;
				switch (brandNumber) {
					case 1 -> brandCall = secondBrand1;
					case 2 -> brandCall = secondBrand2;
					case 3 -> brandCall = secondBrand3;
					case 4 -> brandCall = secondBrand4;
					default -> {
						log.error("Invalid brand number {}! Buff: {}", brandBuff, brandBuff);
						return;
					}
				}
				s.accept(brandCall.getModified(brandBuff));
			});

	/*
		Wire cutting:
		Status effect 0x95D's stack count indicates the number on that wire
		Orange 1 = 450
		Orange 2 = 451
		Orange 3 = 452
		Orange 4 = 453
		Blue 1 = 454
		Blue 2 = 455
		Blue 3 = 456
		Blue 4 = 457
		Uncuttable = 449
		Radial bait = 460

		X Positions:
		286, 288, 290, 292

		Y Positions (north to south):
		-108, -106, -104, -102
	 */

	/*
		Portals and brands:
		Status effect 0xB9A on players:
		467 => West CCW
		462 => West CW
		466 => East CW
		461 => East CCW

		First set of brands has lower ID maybe?
	 */

}
