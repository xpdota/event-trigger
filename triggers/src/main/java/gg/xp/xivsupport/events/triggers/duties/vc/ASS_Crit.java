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
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ScanMe
@CalloutRepo(name = "Another Sil'Dihn Subterrane (Criterion)", duty = KnownDuty.ASS_Criterion)
public class ASS_Crit extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(ASS_Crit.class);

	// First boss
	private final ModifiableCallout<AbilityCastStart> cardSafe = ModifiableCallout.durationBasedCall("Soap's Up: Cardinal Safe", "Cardinal Safe");
	private final ModifiableCallout<AbilityCastStart> dustBluster = ModifiableCallout.durationBasedCall("Dust Bluster", "Knockback");
	private final ModifiableCallout<AbilityCastStart> squeakyCleanLeftSafe = ModifiableCallout.durationBasedCall("Squeaky Clean: Left Safe", "Left");
	private final ModifiableCallout<AbilityCastStart> squeakyCleanRightSafe = ModifiableCallout.durationBasedCall("Squeaky Clean: Right Safe", "Right");
	private final ModifiableCallout<AbilityCastStart> lineStack = ModifiableCallout.durationBasedCall("Line Stack", "Line Stack");
	private final ModifiableCallout<AbilityCastStart> keepMoving = ModifiableCallout.durationBasedCall("Keep Moving", "Keep Moving");
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
		if (!(acs.getSource().getbNpcId() == 14834)) {
			return;
		}
		@Nullable Color color = buffColor(acs.getSource());
		ModifiableCallout<AbilityCastStart> call = switch ((int) acs.getAbility().getId()) {
			case 0x775a -> {
				if (color == Color.YELLOW) {
					yield cardSafe;
				}
				yield null;
			}
			case 0x776c -> dustBluster;
			case 0x774F -> carpetBeater;
			case 0x7750 -> totalWash;
			default -> null;
		};
		if (call != null) {
			ctx.accept(call.getModified(acs));
		}
	}

	private final RepeatSuppressor greenSupp = new RepeatSuppressor(Duration.ofSeconds(2));

	@HandleEvents
	public void fakeCasts(EventContext ctx, AbilityCastStart acs) {
		if (acs.abilityIdMatches(0x7755)) {
			ctx.accept(squeakyCleanLeftSafe.getModified(acs));
		}
		else if (acs.abilityIdMatches(0x7756)) {
			ctx.accept(squeakyCleanRightSafe.getModified(acs));
		}
		else if (acs.abilityIdMatches(0x776B) && buffColor(acs.getSource()) == Color.GREEN) {
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
				s.waitMs(3000);
				s.accept(new ForceCombatEnd());
			}
	);

	@AutoFeed
	public SequentialTrigger<BaseEvent> slipperySoap = SqtTemplates.sq(20_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x775E),
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
					s.waitMs(2_000);
					s.updateCall(intercardSafe.getModified(e1));
				}
				else if (color == Color.YELLOW) {
					// Spread call already handled
					s.waitMs(4_000);
					s.updateCall(cardSafe.getModified(e1));
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> freshPuff = SqtTemplates.multiInvocation(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7766),
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
				AbilityCastStart bossMechanic = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7767));
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
			},
			(e1, s) -> {
				s.updateCall(fp4_begin.getModified(e1));
				// Four puffs, with tethers
			}
	);


	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.ASS_Criterion);
	}

//	@HandleEvents
//	public void handleWipes(EventContext ctx, InCombatChangeEvent icce) {
//		// Need a bit of a workaround here because the normal wipes don't work
//		if (!icce.isInCombat()) {
//		}
//	}
//

	// Second boss
	private final ModifiableCallout<AbilityCastStart> flashOfSteel = ModifiableCallout.durationBasedCall("Flash of Steel", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> rushOfMight = ModifiableCallout.durationBasedCall("Rush of Might Windup", "Inside Lines, Watch Charges");
	private final ModifiableCallout<?> rushOfMightFollowup = new ModifiableCallout<>("Rush of Might Followup", "Move Out");
	private final ModifiableCallout<AbilityCastStart> sculptorsPassion = ModifiableCallout.durationBasedCall("Sculptor's Passion", "Line Stack, Tank in Front");
	private final ModifiableCallout<AbilityCastStart> mightySmite = ModifiableCallout.durationBasedCall("Mighty Smite", "Buster on {event.target}");

	private final ModifiableCallout<BuffApplied> curseOfTheFallen_stackSpread = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Stack then Spread", "Stack on {stack} then Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_spreadStack = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Spread then Stack", "Spread then Stack on {stack}").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_baitSpread = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Bait then Spread", "Bait then Spread").autoIcon();
	// Is this possible?
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_spreadBait = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Spread then Bait", "Spread then Bait").autoIcon();

	private final ModifiableCallout<BuffApplied> curseOfTheFallen_thenStack = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Stack Followup", "Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_thenSpread = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Spread Followup", "Spread on {stack}").autoIcon();
	private final ModifiableCallout<BuffApplied> curseOfTheFallen_thenBait = ModifiableCallout.<BuffApplied>durationBasedCall("CotF: Bait Followup", "Bait").autoIcon();

	private final ModifiableCallout<AbilityCastStart> checkerboard1 = ModifiableCallout.durationBasedCall("Wrath of Ruin 1", "Avoid Lines and Orbs");
	private final ModifiableCallout<AbilityCastStart> checkerboard2_2gold = ModifiableCallout.<AbilityCastStart>durationBasedCall("Wrath of Ruin 2: 2 Gold", "Get Hit By Two Silver Orbs").statusIcon(0xCDF, 2);
	private final ModifiableCallout<AbilityCastStart> checkerboard2_2silver = ModifiableCallout.<AbilityCastStart>durationBasedCall("Wrath of Ruin 2: 2 Silver", "Get Hit By Two Gold Orbs").statusIcon(0xCE0, 2);
	private final ModifiableCallout<AbilityCastStart> checkerboard2_1each = ModifiableCallout.<AbilityCastStart>durationBasedCall("Wrath of Ruin 2: 1 of Each", "Get Hit By Silver and Gold").statusIcon(0xCE0, 1);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> rushOfMightSq = SqtTemplates.beginningAndEndingOfCast(
			// TODO: Find IDs, see if there's any additional data from them. However, it's likely that these three alone are short/medium/long
			acs -> acs.abilityIdMatches(0x7658, 0x7659, 0x765A),
			rushOfMight, rushOfMightFollowup);

	@HandleEvents
	public void secondBossBasicCasts(EventContext context, AbilityCastStart acs) {
		ModifiableCallout<AbilityCastStart> call;
		switch ((int) acs.getAbility().getId()) {
			case 0x7671 -> call = flashOfSteel;
			case 0x766c -> call = sculptorsPassion;
			case 0x7672 -> call = mightySmite;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(acs));
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> curseOfTheFallenSq = SqtTemplates.sq(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7674),
			(e1, s) -> {
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7674));
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
	private final SequentialTrigger<BaseEvent> wrathOfRuin = SqtTemplates.multiInvocation(20_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7663),
			(e1, s) -> {
				// First checkerboard, just dodge lines and orbs
				checkerboard1.getModified(e1);
			}, (e1, s) -> {
				XivPlayerCharacter player = getState().getPlayer();
				AbilityCastStart orbCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7670));
				// Second checkerboard, have to look at debuff
				int goldBuffStacks = getBuffs().buffStacksOnTarget(player, 0xCDF);
				int silverBuffStacks = getBuffs().buffStacksOnTarget(player, 0xCE0);
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

	// Third Boss
	private final ModifiableCallout<BuffApplied> brand1 = ModifiableCallout.<BuffApplied>durationBasedCall("Infern Brand: #1", "First Brand").autoIcon();
	private final ModifiableCallout<BuffApplied> brand2 = ModifiableCallout.<BuffApplied>durationBasedCall("Infern Brand: #2", "Second Brand").autoIcon();
	private final ModifiableCallout<BuffApplied> brand3 = ModifiableCallout.<BuffApplied>durationBasedCall("Infern Brand: #3", "Third Brand").autoIcon();
	private final ModifiableCallout<BuffApplied> brand4 = ModifiableCallout.<BuffApplied>durationBasedCall("Infern Brand: #4", "Fourth Brand").autoIcon();

	@HandleEvents
	public void brandBuffs(EventContext context, BuffApplied ba) {
		if (ba.getTarget().isThePlayer()) {
			ModifiableCallout<BuffApplied> call;
			switch ((int) ba.getBuff().getId()) {
				case 0xCC4 -> call = brand1;
				case 0xCC5 -> call = brand2;
				case 0xCC6 -> call = brand3;
				case 0xCC7 -> call = brand4;
				default -> {
					return;
				}
			}
			context.accept(call.getModified(ba));
		}
	}

}
