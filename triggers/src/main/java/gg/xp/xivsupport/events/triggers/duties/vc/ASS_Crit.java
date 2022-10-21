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
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.misc.pulls.ForceCombatEnd;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

@ScanMe
@CalloutRepo(name = "Another Sil'Dihn Subterrane (Criterion)", duty = KnownDuty.ASS_Criterion)
public class ASS_Crit extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(ASS_Crit.class);

	private final ModifiableCallout<AbilityCastStart> cardSafe = ModifiableCallout.durationBasedCall("Soap's Up: Cardinal Safe", "Cardinal Safe");
	private final ModifiableCallout<AbilityCastStart> dustBluster = ModifiableCallout.durationBasedCall("Dust Bluster", "Knockback");
	private final ModifiableCallout<AbilityCastStart> squeakyCleanLeftSafe = ModifiableCallout.durationBasedCall("Squeaky Clean: Left Safe", "Left");
	private final ModifiableCallout<AbilityCastStart> squeakyCleanRightSafe = ModifiableCallout.durationBasedCall("Squeaky Clean: Right Safe", "Right");
	private final ModifiableCallout<AbilityCastStart> lineStack = ModifiableCallout.durationBasedCall("Line Stack", "Line Stack");
	private final ModifiableCallout<AbilityCastStart> keepMoving = ModifiableCallout.durationBasedCall("Keep Moving", "Keep Moving");
	private final ModifiableCallout<AbilityCastStart> intercardSafe = ModifiableCallout.durationBasedCall("Intercard Safe", "Intercard Safe");
	private final ModifiableCallout<AbilityCastStart> carpetBeater = ModifiableCallout.durationBasedCall("Carpet Beater", "Buster on {event.target}");
	private final ModifiableCallout<AbilityCastStart> underGreen = ModifiableCallout.durationBasedCall("Under Green", "Get Under Green");
	private final ModifiableCallout<AbilityCastStart> underBoss = ModifiableCallout.durationBasedCall("Under Green", "Get Under Green");
	private final ModifiableCallout<AbilityCastStart> totalWash = ModifiableCallout.durationBasedCall("Total Wash", "Raidwide with Bleed");
	private final ModifiableCallout<TetherEvent> fp2_blueTether = new ModifiableCallout<>("Fresh Puff 2: Blue Tether", "Blue Tether");
	private final ModifiableCallout<TetherEvent> fp2_yellowTether = new ModifiableCallout<>("Fresh Puff 2: Yellow Tether", "Yellow Tether");

	private final XivState state;
	private final StatusEffectRepository buffs;

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
	public void basicCast(EventContext ctx, AbilityCastStart acs) {
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

	@HandleEvents
	public void fakeCasts(EventContext ctx, AbilityCastStart acs) {
		if (acs.abilityIdMatches(0x7755)) {
			ctx.accept(squeakyCleanLeftSafe.getModified(acs));
		}
		else if (acs.abilityIdMatches(0x7756)) {
			ctx.accept(squeakyCleanRightSafe.getModified(acs));
		}
		else if (acs.abilityIdMatches(0x776B) && buffColor(acs.getSource()) == Color.GREEN) {
			ctx.accept(underGreen.getModified(acs));
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
				s.waitMs(2_000);
				s.updateCall(keepMoving.getModified(e1));
				s.waitMs(2_000);
				// TODO: is this always the right call here? Or should this method be split out into other sequentials?
				s.updateCall(intercardSafe.getModified(e1));
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> freshPuff = SqtTemplates.multiInvocation(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7766),
			(e1, s) -> {
				// First invocation: handled by other triggers already
			},
			(e1, s) -> {
				// Second invocation: four puffs, with tethers
				TetherEvent tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				XivCombatant puff = tether.getTargetMatching(cbt -> !cbt.isPc());
				if (puff == null) {
					log.error("Null puff!");
				}
				Color color = buffColor(puff);
				ArenaSector arenaSector = firstBossArena.forCombatant(puff);
				Map<String, Object> params = Map.of("where", arenaSector);
				if (color == Color.BLUE) {
					s.updateCall(fp2_blueTether.getModified(tether, params));
				}
				else if (color == Color.YELLOW) {
					s.updateCall(fp2_yellowTether.getModified(tether, params));
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
				// Eight puffs
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
}
