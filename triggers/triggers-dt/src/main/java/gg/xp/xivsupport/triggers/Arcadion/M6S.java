package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.EventCollector;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CalloutRepo(name = "M6S", duty = KnownDuty.M6S)
public class M6S extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(M6S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M6S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M6S);
	}

	@NpcCastCallout(0xA6BC)
	private final ModifiableCallout<AbilityCastStart> mousseMural = ModifiableCallout.durationBasedCall("Mousse Mural", "Raidwide");
	// TODO: differentiate colors
	@NpcCastCallout({0xA691, 0xA692})
	private final ModifiableCallout<AbilityCastStart> colorRiot = ModifiableCallout.durationBasedCall("Color Riot", "Double Tankbuster");

	private static final ArenaPos pos = new ArenaPos(100, 100, 5, 5);

	private final ModifiableCallout<AbilityCastStart> doubleStyle1 = ModifiableCallout.durationBasedCall("Double Style 1: KB Safe Spot", "KB from {kbFrom}");
	private final ModifiableCallout<?> doubleStyle1colorClashPartners = new ModifiableCallout<>("Double Style 1: Partners", "Partners");
	private final ModifiableCallout<?> doubleStyle1colorClashLP = new ModifiableCallout<>("Double Style 1: Light Parties", "Light Parties");

	private enum ColorClash {
		Partners,
		LightParties
	}

	private final ModifiableCallout<?> colorClashPartnerStock = new ModifiableCallout<>("Color Clash: Stocking Partners", "Partners Stocked");
	private final ModifiableCallout<?> colorClashLpStock = new ModifiableCallout<>("Color Clash: Stocking Light Parties", "Light Parties Stocked");

	private ColorClash lastClash;

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stockSq = SqtTemplates.sq(1_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA68B, 0xA68D),
			(e1, s) -> {
				if (e1.abilityIdMatches(0xA68D)) {
					s.updateCall(colorClashPartnerStock);
					lastClash = ColorClash.Partners;
				}
				else {
					s.updateCall(colorClashLpStock);
					lastClash = ColorClash.LightParties;
				}
			});

	/*
		Morbol (18340): Cone. Two adjacent sectors are safe.
		Succubus (18341): Big AoE. Three opposite sectors are safe.
		Paint Bomb: (18336): Small AoE, occupied sector unsafe.
		Heaven Bomb: (18337): Small AoE. Opposite sector is unsafe. Seems to come in sets of 3.
	 */

	@AutoFeed
	private final SequentialTrigger<BaseEvent> doubleStyle = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA67D, 0xA67E, 0xA67F, 0xA680, 0xA681, 0xA682),
			(e1, s) -> {
				var tethers = s.waitEventsQuickSuccession(4, TetherEvent.class, te -> te.tetherIdMatches(0x13F, 0x140));
				// Start with everything safe and eliminate possibilities
				Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.quadrants);

				for (TetherEvent tether : tethers) {
					// One of the targets is the boss, but do this just in case it uses a fake
					XivCombatant tgt = tether.getTargetMatching(npc -> npc.getbNpcNameId() != 13822);
					ArenaSector originalPos = pos.forCombatant(state.getLatestCombatantData(tgt));
					// Succubus
					if (tgt.npcIdMatches(18341)) {
						// This is a big aoe. The three sectors opposite the succubus are safe.
						log.info("Succubus {}", originalPos);
						safe.retainAll(List.of(originalPos.opposite(), originalPos.plusEighths(1).opposite(), originalPos.plusEighths(-1).opposite()));
					}
					// Morbol
					else if (tgt.npcIdMatches(18340)) {
						log.info("Morbol {}", originalPos);
						// This is a big cone. The two adjacent sectors are safe.
						safe.retainAll(List.of(originalPos.plusEighths(1), originalPos.plusEighths(-1)));
					}
					// Bomb (fixed)
					else if (tgt.npcIdMatches(18336)) {
						log.info("Bomb {}", originalPos);
						// Technically, the bomb doesn't jump to the opposite side, but they always come in a row of
						// three, so it is effectively the same.
						safe.remove(originalPos);
					}
					// Bomb (jumping)
					else if (tgt.npcIdMatches(18337)) {
						ArenaSector jumpPos = originalPos.opposite();
						log.info("Bomb {} -> {}", originalPos, jumpPos);
						// Technically, the bomb doesn't jump to the opposite side, but they always come in a row of
						// three, so it is effectively the same.
						safe.remove(jumpPos);
					}
					else {
						log.error("Unknown npc ID: {}", tgt.getbNpcId());
					}
				}
				if (safe.size() == 1) {
					// You need to start opposite of the safe spot and get KB'd
					ArenaSector safeSpot = safe.iterator().next();
					s.setParam("safe", safeSpot);
					s.setParam("kbFrom", safeSpot.opposite());
				}
				else {
					log.error("Bad safe spots: {}", safe);
					s.setParam("safe", ArenaSector.UNKNOWN);
					s.setParam("kbFrom", ArenaSector.UNKNOWN);
				}

				s.updateCall(doubleStyle1, e1);

				// Wait for the stun to apply
				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x1043));

				s.updateCall(lastClash == ColorClash.Partners ? doubleStyle1colorClashPartners : doubleStyle1colorClashLP);
			});

	private final ModifiableCallout<AbilityCastStart> stickyMousseCast = ModifiableCallout.durationBasedCall("Sticky Mousse Cast", "Spread");
	private final ModifiableCallout<?> stickyMousseTargets = new ModifiableCallout<>("Sticky Mousse Targets", "Mousse on {targets}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stickyMousseSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA695),
			(e1, s) -> {
				s.updateCall(stickyMousseCast, e1);
				List<AbilityUsedEvent> hits = s.waitEventsQuickSuccession(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA696));

				List<XivCombatant> stuck = hits.stream()
						.map(AbilityUsedEvent::getTarget)
						// Sort the player first
						.sorted(Comparator.comparing(c -> c.isThePlayer() ? 0 : 1))
						.toList();
				s.setParam("targets", stuck);
				s.updateCall(stickyMousseTargets);
			});

	private final ModifiableCallout<TetherEvent> doubleStyle2normalBomb = new ModifiableCallout<>("Double Style 2: Normal Bomb Tether", "Place Bomb in Bad");
	private final ModifiableCallout<TetherEvent> doubleStyle2flyingBomb = new ModifiableCallout<>("Double Style 2: Flying Bomb Tether", "Place Bomb in Safe");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> doubleStyle2 = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA683),
			(e1, s) -> {
				TetherEvent myTether = s.waitEvent(TetherEvent.class, te -> te.tetherIdMatches(0x13F, 0x140) && te.eitherTargetMatches(XivCombatant::isThePlayer));
				if (myTether.tetherIdMatches(0x13F)) {
					s.updateCall(doubleStyle2flyingBomb, myTether);
				}
				else {
					s.updateCall(doubleStyle2normalBomb, myTether);
				}
			});

	@NpcCastCallout(0xA6A5)
	private final ModifiableCallout<?> adds = new ModifiableCallout<>("Soul Sugar (Adds)", "Adds");

	@NpcCastCallout(0xA6AA)
	private final ModifiableCallout<AbilityCastStart> readyOreNot = ModifiableCallout.durationBasedCall("Ready Ore Not", "Raidwide");

	private final ModifiableCallout<BuffApplied> sugarScapeShortSpreadLater = ModifiableCallout.<BuffApplied>durationBasedCall("Sugarscape: Short Spread Later", "Later: Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> sugarScapeShortStackLater = ModifiableCallout.<BuffApplied>durationBasedCall("Sugarscape: Short Stack Later", "Later: Stack on {stackOn}").autoIcon();
	private final ModifiableCallout<BuffApplied> sugarScapeLongSpreadLater = ModifiableCallout.<BuffApplied>durationBasedCall("Sugarscape: Long Spread Later", "Later: Stack on {stackOn} then spread").autoIcon();

	private final ModifiableCallout<BuffApplied> sugarScapeShortSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Sugarscape: Short Spread Now", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> sugarScapeShortStack = ModifiableCallout.<BuffApplied>durationBasedCall("Sugarscape: Short Stack Now", "Stack on {stackOn}").autoIcon();

	private final ModifiableCallout<BuffApplied> sugarScapeLongSpread = ModifiableCallout.<BuffApplied>durationBasedCall("Sugarscape: Long Spread Now", "Spread").autoIcon();
	private final ModifiableCallout<BuffApplied> sugarScapeAvoidLongSpread = ModifiableCallout.durationBasedCall("Sugarscape: Avoid Long Spread", "Avoid Fire on {longSpreadOn}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> sugarScapeSq = SqtTemplates.multiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA668),
			(e1, s) -> {
				// TODO: can the stack be long instead of short?
				// TODO: some weird stuff going on here
				EventCollector<BuffApplied> shortSpread = new EventCollector<>(ba -> ba.buffIdMatches(0x1166) && ba.getInitialDuration().toSeconds() < 60);
				EventCollector<BuffApplied> longSpread = new EventCollector<>(ba -> ba.buffIdMatches(0x1166) && ba.getInitialDuration().toSeconds() >= 60);
				EventCollector<BuffApplied> shortStack = new EventCollector<>(ba -> ba.buffIdMatches(0x1160) && ba.getInitialDuration().toSeconds() < 60);
				s.collectEvents(5, 10_000, BuffApplied.class, true, List.of(shortSpread, longSpread, shortStack));

				Optional<BuffApplied> anyShortStackOpt = shortStack.findAny(ignored -> true);
				anyShortStackOpt.ifPresent(stack -> {
					s.setParam("stackOn", stack.getTarget());
				});
				List<XivCombatant> longSpreadTargets = longSpread.getEvents().stream().map(longSpreads -> longSpreads.getTarget()).toList();
				s.setParam("longSpreadOn", longSpreadTargets);
				Optional<BuffApplied> myLongSpreadOpt = longSpread.findAny(ls -> ls.getTarget().isThePlayer());
				myLongSpreadOpt.ifPresent(myLs -> {
					s.updateCall(sugarScapeLongSpreadLater, myLs);
					// The rest is handled further down
				});
				shortSpread.findAny(ba -> ba.getTarget().isThePlayer()).ifPresentOrElse(mySpread -> {
					s.updateCall(sugarScapeShortSpreadLater, mySpread);
					s.waitDuration(mySpread.getEstimatedRemainingDuration().minus(Duration.ofSeconds(8)));
					s.updateCall(sugarScapeShortSpread, mySpread);
				}, () -> {
					// You also hit this branch if you have a long spread
					anyShortStackOpt.ifPresent(ss -> {
						if (myLongSpreadOpt.isEmpty()) {
							s.updateCall(sugarScapeShortStackLater, ss);
						}
						s.waitDuration(ss.getEstimatedRemainingDuration().minus(Duration.ofSeconds(8)));
						s.updateCall(sugarScapeShortStack, ss);
					});
				});
				longSpread.findAny(ignored -> true).ifPresent(anyLs -> {
					s.waitDuration(anyLs.getEstimatedRemainingDuration().minus(Duration.ofSeconds(8)));
					myLongSpreadOpt.ifPresentOrElse(myLs -> {
						s.updateCall(sugarScapeLongSpread, myLs);
					}, () -> {
						s.updateCall(sugarScapeAvoidLongSpread, anyLs);
					});
				});

			});

	private final ModifiableCallout<AbilityUsedEvent> rayWater = new ModifiableCallout<>("Adds: Ray Water", "Avoid Water");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> rayWaterSq = SqtTemplates.sq(30_000,
			AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA6AF) && aue.getTarget().isThePlayer(),
			(e1, s) -> {
				s.updateCall(rayWater, e1);
				// Refire suppression
				s.waitMs(300);
			});

	/*
	Next mechs:
	Double style II (0xA683): can be tethered to regular or flying bomb
	Tether ID is 0x13F (purple, flying) or 0x140 (blue, normal)

	Corner safe with cacti
	The two people with long spreads take two opposite safe spots, while the party takes the middle safe spot
	One corner is unsafe.

	adds spawn waves? one callout per wave?
	 */
}
