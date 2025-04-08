package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.util.MathUtils;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@CalloutRepo(name = "M5S", duty = KnownDuty.M5S)
public class M5S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M5S.class);
	private XivState state;
	private ActiveCastRepository casts;
	private StatusEffectRepository buffs;

	public M5S(XivState state, ActiveCastRepository casts, StatusEffectRepository buffs) {
		this.state = state;
		this.casts = casts;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M5S);
	}

	@NpcCastCallout(0xA721)
	private final ModifiableCallout<AbilityCastStart> deepCut = ModifiableCallout.<AbilityCastStart>durationBasedCall("Deep Cut", "Tankbuster with Bleed").statusIcon(0x8280);

	private final ModifiableCallout<AbilityCastStart> stockingASide = new ModifiableCallout<>("Stocking A-Side", "A Side Stocked");
	private final ModifiableCallout<AbilityCastStart> stockingBSide = new ModifiableCallout<>("Stocking B-Side", "B Side Stocked");

	private enum Stock {
		Roles,
		LightParty
	}

	private Stock lastStock;

	@AutoFeed
	private final SequentialTrigger<BaseEvent> stockSq = SqtTemplates.sq(1_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA780, 0xA781),
			(e1, s) -> {
				if (e1.abilityIdMatches(0xA780)) {
					s.updateCall(stockingASide, e1);
					lastStock = Stock.Roles;
				}
				else {
					s.updateCall(stockingBSide, e1);
					lastStock = Stock.LightParty;
				}
			});

	private final ModifiableCallout<AbilityCastStart> twoSnapWestRoles = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting West First, A-Side", "East then West Role Stacks");
	private final ModifiableCallout<?> twoSnapWestRolesFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting West First, A-Side (Followup)", "West Role Stacks");
	private final ModifiableCallout<AbilityCastStart> twoSnapEastRoles = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting East First, A-Side", "West then East Role Stacks");
	private final ModifiableCallout<?> twoSnapEastRolesFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting East First (Followup), A-Side", "East Role Stacks");
	private final ModifiableCallout<AbilityCastStart> twoSnapWestLp = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting West First, B-Side", "East then West Light Parties");
	private final ModifiableCallout<?> twoSnapWestLpFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting West First (Followup), B-Side", "West Light Parties");
	private final ModifiableCallout<AbilityCastStart> twoSnapEastLp = ModifiableCallout.durationBasedCall("Two-Snap & Drop: Hitting East First, B-Side", "West then East Light Parties");
	private final ModifiableCallout<?> twoSnapEastLpFollowup = new ModifiableCallout<>("Two-Snap & Drop: Hitting East First (Followup), B-Side", "East Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> twoSnapSq = SqtTemplates.sq(15_000,
			/*
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781)
			 *
			 * A728 -> west -> east, facing north
			 * A729 -> west -> east, facing north
			 * A72A -> west -> east, facing north
			 * A72B -> east -> west, facing north
			 * A72C -> east -> west, facing north
			 * A72D -> east -> west, facing north
			 *
			 * A4DB -> west -> east, facing north
			 * A4DC -> east -> west, facing north
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA728, 0xA729, 0xA72A, 0xA72B, 0xA72C, 0xA72D, 0xA4DB, 0xA4DC),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				boolean isRoles = lastStock == Stock.Roles;
				switch (id) {
					case 0xA728, 0xA729, 0xA72A, 0xA4DB -> {
						firstCall = isRoles ? twoSnapEastRoles : twoSnapEastLp;
						secondCall = isRoles ? twoSnapEastRolesFollowup : twoSnapEastLpFollowup;
					}
					case 0xA72B, 0xA72C, 0xA72D, 0xA4DC -> {
						firstCall = isRoles ? twoSnapWestRoles : twoSnapWestLp;
						secondCall = isRoles ? twoSnapWestRolesFollowup : twoSnapWestLpFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA72E));
				s.updateCall(secondCall);
			});

	private final ModifiableCallout<AbilityCastStart> threeSnapWestRoles = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting West First, A-Side", "East then West Role Stacks");
	private final ModifiableCallout<?> threeSnapWestRolesFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting West First, A-Side (Followup)", "West Role Stacks");
	private final ModifiableCallout<AbilityCastStart> threeSnapEastRoles = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting East First, A-Side", "West then East Role Stacks");
	private final ModifiableCallout<?> threeSnapEastRolesFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting East First (Followup), A-Side", "East Role Stacks");
	private final ModifiableCallout<AbilityCastStart> threeSnapWestLp = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting West First, B-Side", "East then West Light Parties");
	private final ModifiableCallout<?> threeSnapWestLpFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting West First (Followup), B-Side", "West Light Parties");
	private final ModifiableCallout<AbilityCastStart> threeSnapEastLp = ModifiableCallout.durationBasedCall("Three-Snap & Drop: Hitting East First, B-Side", "West then East Light Parties");
	private final ModifiableCallout<?> threeSnapEastLpFollowup = new ModifiableCallout<>("Three-Snap & Drop: Hitting East First (Followup), B-Side", "East Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> threeSnapSq = SqtTemplates.sq(15_000,
			/*
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781), but the cast ID of the actual twist/drop seems to also vary, so the precursor
			 * can effectively be ignored. However, we should call it out anyway for convenience/
			 *
			 * A730 -> west -> east, facing north
			 * A731 -> west -> east, facing north
			 * A732 -> west -> east, facing north
			 * A733 -> east -> west, facing north
			 * A734 -> ? east -> west, facing north
			 * A735 -> east -> west, facing north
			 *
			 * More:
			 * A4DD -> west -> east, speculative
			 * A4DE -> east -> west
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA730, 0xA731, 0xA732, 0xA733, 0xA734, 0xA735, 0xA4DD, 0xA4DE),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				boolean isRoles = lastStock == Stock.Roles;
				switch (id) {
					case 0xA730, 0xA731, 0xA732, 0xA4DD -> {
						firstCall = isRoles ? threeSnapEastRoles : threeSnapEastLp;
						secondCall = isRoles ? threeSnapEastRolesFollowup : threeSnapEastLpFollowup;
					}
					case 0xA733, 0xA734, 0xA735, 0xA4DE -> {
						firstCall = isRoles ? threeSnapWestRoles : threeSnapWestLp;
						secondCall = isRoles ? threeSnapWestRolesFollowup : threeSnapWestLpFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA737));
				s.updateCall(secondCall);
			});

	private final ModifiableCallout<AbilityCastStart> fourSnapWestRoles = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting West First, A-Side", "East then West Role Stacks");
	private final ModifiableCallout<?> fourSnapWestRolesFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting West First, A-Side (Followup)", "West Role Stacks");
	private final ModifiableCallout<AbilityCastStart> fourSnapEastRoles = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting East First, A-Side", "West then East Role Stacks");
	private final ModifiableCallout<?> fourSnapEastRolesFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting East First (Followup), A-Side", "East Role Stacks");
	private final ModifiableCallout<AbilityCastStart> fourSnapWestLp = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting West First, B-Side", "East then West Light Parties");
	private final ModifiableCallout<?> fourSnapWestLpFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting West First (Followup), B-Side", "West Light Parties");
	private final ModifiableCallout<AbilityCastStart> fourSnapEastLp = ModifiableCallout.durationBasedCall("Four-Snap & Drop: Hitting East First, B-Side", "West then East Light Parties");
	private final ModifiableCallout<?> fourSnapEastLpFollowup = new ModifiableCallout<>("Four-Snap & Drop: Hitting East First (Followup), B-Side", "East Light Parties");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> fourSnapSq = SqtTemplates.sq(15_000,
			/*
			 * Precursor is Flip to A-side (A780) or Flip to B-side (A781), but the cast ID of the actual twist/drop seems to also vary, so the precursor
			 * can effectively be ignored. However, we should call it out anyway for convenience/
			 *
			 * A730 -> west -> east, facing north
			 * A731 -> west -> east, facing north
			 * A732 -> west -> east, facing north
			 * A733 -> east -> west, facing north
			 * A734 -> ? east -> west, facing north
			 * A735 -> east -> west, facing north
			 *
			 * A4DF and A4E0 are speculative
			 *
			 * Not sure why there are seemingly-redundant entries
			 */
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA739, 0xA73A, 0xA73B, 0xA73C, 0xA73D, 0xA73E, 0xA4DF, 0xA4E0),
			(e1, s) -> {
				int id = (int) e1.getAbility().getId();
				ModifiableCallout<AbilityCastStart> firstCall;
				ModifiableCallout<?> secondCall;
				boolean isRoles = lastStock == Stock.Roles;
				switch (id) {
					case 0xA739, 0xA73A, 0xA73B, 0xA4DF -> {
						firstCall = isRoles ? fourSnapEastRoles : fourSnapEastLp;
						secondCall = isRoles ? fourSnapEastRolesFollowup : fourSnapEastLpFollowup;
					}
					case 0xA73C, 0xA73D, 0xA73E, 0xA4E0 -> {
						firstCall = isRoles ? fourSnapWestRoles : fourSnapWestLp;
						secondCall = isRoles ? fourSnapWestRolesFollowup : fourSnapWestLpFollowup;
					}
					default -> throw new IllegalStateException("Unexpected value: " + id);
				}
				s.updateCall(firstCall, e1);
				// First followup
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA741));
				s.updateCall(secondCall);
			});

	@NpcCastCallout(0xA723)
	private final ModifiableCallout<AbilityCastStart> celebrateGoodTimes = ModifiableCallout.durationBasedCall("Celebrate Good Times", "Raidwide");

	private final ModifiableCallout<AbilityCastStart> discoInfernal = ModifiableCallout.durationBasedCall("Disco Infernal", "Raidwide");
	private final ModifiableCallout<BuffApplied> discoInfernalLong = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Long Timer", "Long Timer").autoIcon();
	private final ModifiableCallout<BuffApplied> discoInfernalShort = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Short Timer", "Short Timer").autoIcon();

	private final ModifiableCallout<AbilityCastStart> outsideIn = ModifiableCallout.durationBasedCall("Outside In", "In then Out");
	private final ModifiableCallout<AbilityCastStart> insideOut = ModifiableCallout.durationBasedCall("Inside Out", "Out then In");
	private final ModifiableCallout<?> outsideIn2 = new ModifiableCallout<>("Outside In, Followup", "Out");
	private final ModifiableCallout<?> insideOut2 = new ModifiableCallout<>("Inside Out, Followup", "In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> outsideInSq = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA77C, 0xA77E),
			(e1, s) -> {
				if (e1.abilityIdMatches(0xA77C)) {
					s.updateCall(insideOut, e1);
				}
				else {
					s.updateCall(outsideIn, e1);
				}
				// These are probably all the followups
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x93C2, 0x93C3, 0x93C4, 0x93C5));
				if (e1.abilityIdMatches(0xA77C)) {
					s.updateCall(insideOut2);
				}
				else {
					s.updateCall(outsideIn2);
				}
			});

	private final ModifiableCallout<BuffApplied> discoInfernalSoak = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal: Soak Now", "Soak").autoIcon();

	private final ModifiableCallout<BuffApplied> discoInfernal2Long = new ModifiableCallout<>("Disco Infernal 2: Long Timer", "Bait");
	private final ModifiableCallout<BuffApplied> discoInfernal2Short = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal 2: Short Timer", "Soak").autoIcon();
	private final ModifiableCallout<BuffApplied> discoInfernal2LongFollowup = ModifiableCallout.<BuffApplied>durationBasedCall("Disco Infernal 2: Long Timer", "Soak").autoIcon();
	private final ModifiableCallout<BuffApplied> discoInfernal2ShortFollowup = new ModifiableCallout<>("Disco Infernal 2: Short Timer", "Bait");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> discoInfernalSq = SqtTemplates.multiInvocation(75_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA756),
			(e1, s) -> {
				s.updateCall(discoInfernal, e1);
				var myBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x116D) && ba.getTarget().isThePlayer());
				boolean iAmLong = myBuff.getInitialDuration().toSeconds() > 28;
				RawModifiedCallout<BuffApplied> buffCall;
				if (iAmLong) {
					buffCall = s.call(discoInfernalLong, myBuff);
				}
				else {
					buffCall = s.call(discoInfernalShort, myBuff);
				}
				// Wait until our buff has 7 seconds left
				s.waitDuration(myBuff.remainingDurationPlus(Duration.ofSeconds(-7)));
				s.call(discoInfernalSoak, myBuff).setReplaces(buffCall);


			}, (e1, s) -> {
				s.updateCall(discoInfernal, e1);
				var myBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x116D) && ba.getTarget().isThePlayer());
				// These are 9.5 and 19.5 seconds
				boolean iAmLong = myBuff.getInitialDuration().toSeconds() > 14;
				// TODO: call where spotlights are (if short) or where to bait cone (if long)
				if (iAmLong) {
					s.updateCall(discoInfernal2Long, myBuff);
				}
				else {
					s.updateCall(discoInfernal2Short, myBuff);
				}
				// Cleave resolving
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0xA777));
				if (iAmLong) {
					s.updateCall(discoInfernal2LongFollowup, myBuff);
				}
				else {
					s.updateCall(discoInfernal2ShortFollowup, myBuff);
				}

			});

	private final ModifiableCallout<AbilityCastStart> arcadyInitial = ModifiableCallout.durationBasedCall("Arcady: Initial", "Out");

	private final ModifiableCallout<?> arcadyIn = new ModifiableCallout<>("Arcady: In", "In");
	private final ModifiableCallout<?> arcadyOut = new ModifiableCallout<>("Arcady: Out", "Out");
	private final ModifiableCallout<?> arcadyInGotHit = new ModifiableCallout<>("Arcady: In, Got Hit", "In, Dodge");
	private final ModifiableCallout<?> arcadyOutGotHit = new ModifiableCallout<>("Arcady: Out, Got Hit", "Out, Dodge");
	private final ModifiableCallout<?> arcadyFinalGotHit = new ModifiableCallout<>("Arcady: Final Cleave, Got Hit", "Dodge");
	private final ModifiableCallout<?> arcadyInGotHitByMistake = new ModifiableCallout<>("Arcady: In, Got Hit By Mistake", "In, Wrong Hit")
			.extendedDescription("""
					This call, and the one below, trigger if you got hit, but the cleave appears to have been intended for another player.""");
	private final ModifiableCallout<?> arcadyOutGotHitByMistake = new ModifiableCallout<>("Arcady: Out, Got Hit By Mistake", "Out, Wrong Hit");
	private final ModifiableCallout<?> arcadyFinalGotHitMistake = new ModifiableCallout<>("Arcady: Final Cleave, Got Hit By Mistake", "Dodge, Wrong Hit");
	private final ModifiableCallout<BuffApplied> arcadyNisi = ModifiableCallout.<BuffApplied>durationBasedCall("Arcady: Nisi", "Touch {partner}").autoIcon();

	@AutoFeed
	private final SequentialTrigger<BaseEvent> arcadySq = SqtTemplates.selfManagedMultiInvocation(75_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9BE4),
			(e1, s, inv) -> {
				// Initial Out call
				s.updateCall(arcadyInitial, e1);
				s.waitCastFinished(casts, e1);
				// Only the first one has the nisi mechanic
				boolean careAboutNisi = inv == 0;
				for (int i = 0; i < 7; i++) {
					List<AbilityUsedEvent> hits = s.collectAoeHits(aue -> aue.abilityIdMatches(0xA764));
					if (hits.get(0).getTarget().isThePlayer()) {
						s.updateCall(i % 2 == 0 ? arcadyInGotHit : arcadyOutGotHit);
					}
					else if (hits.stream().skip(1).anyMatch(hit -> hit.getTarget().isThePlayer())) {
						s.updateCall(i % 2 == 0 ? arcadyInGotHitByMistake : arcadyOutGotHitByMistake);
					}
					else {
						s.updateCall(i % 2 == 0 ? arcadyIn : arcadyOut);
					}
				}
				List<AbilityUsedEvent> hits = s.collectAoeHits(aue -> aue.abilityIdMatches(0xA762, 0xA763));
				if (hits.get(0).getTarget().isThePlayer()) {
					s.updateCall(arcadyFinalGotHit);
				}
				else if (hits.stream().skip(1).anyMatch(hit -> hit.getTarget().isThePlayer())) {
					s.updateCall(arcadyFinalGotHitMistake);
				}
				if (!careAboutNisi) {
					return;
				}
				s.waitMs(2_000);
				// Alpha is 0x116e, Beta is 116f

				BuffApplied myBuff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0x116e, 0x116f));
				if (myBuff == null) {
					log.warn("Player has no nisi!");
					return;
				}
				BuffApplied matchingBuff = buffs.findBuff(
						ba -> !ba.getTarget().isThePlayer()
						      && ba.buffIdMatches(0x116e, 0x116f)
						      // allow up to 1800ms of tolerance
						      && ba.getEstimatedRemainingDuration().minus(myBuff.getEstimatedRemainingDuration()).abs().toMillis() < 1_800);
				if (matchingBuff == null) {
					log.warn("Player has no partner!");
					return;
				}
				s.setParam("partnerBuff", matchingBuff);
				s.setParam("partner", matchingBuff.getTarget());
				s.waitDuration(myBuff.remainingDurationPlus(Duration.ofSeconds(-4)));
				s.updateCall(arcadyNisi, myBuff);
			});


	private final ModifiableCallout<ActorControlExtraEvent> letsDanceFirst = new ModifiableCallout<>("Let's Dance: Initial", "Start {safe}", "{safeSpots[i..-1]}");

	private final ModifiableCallout<ActorControlExtraEvent> letsDanceCross = new ModifiableCallout<>("Let's Dance: Move", "Move {safe}");

	private final ModifiableCallout<ActorControlExtraEvent> letsDanceStay = new ModifiableCallout<>("Let's Dance: Stay", "Stay {safe}");

	private static ArenaSector ensembleToSafeDirection(ActorControlExtraEvent event) {
		return switch ((int) event.getData0()) {
			// 0x05: hitting east
			// 0x07: hitting west
			// 0x1F: hitting south
			// 0x20: hitting north
			case 0x05 -> ArenaSector.WEST;
			case 0x07 -> ArenaSector.EAST;
			case 0x1F -> ArenaSector.NORTH;
			case 0x20 -> ArenaSector.SOUTH;
			default -> {
				log.error("Unknown arena sector: {}", event.getData0());
				yield ArenaSector.UNKNOWN;
			}
		};
	}

	@AutoFeed
	// TODO: this was timing out on the 2nd run
	private final SequentialTrigger<BaseEvent> ensembleFrogsSq = SqtTemplates.sq(120_000,
			// Start on "Ensemble Assemble"
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9A32),
			(e1, s) -> {
				var events = s.waitEvents(8, ActorControlExtraEvent.class, acee -> acee.getCategory() == 0x3f);
				List<ArenaSector> safeSpots = events.stream()
						.map(M5S::ensembleToSafeDirection)
						.toList();

				var cast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA76A, 0xA390));
				var current = events.get(0);
				s.setParam("safe", ensembleToSafeDirection(current));
				s.setParam("safeSpots", safeSpots);
				s.setParam("i", 0);
				s.updateCall(letsDanceFirst, current);
				s.waitCastFinished(casts, cast);
				// TODO: timing out on second instance
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9BDD, 0xA395) && aue.isFirstTarget());
				for (int i = 1; i < safeSpots.size(); i++) {
					log.info("i == {}, SafeSpot: {}", i, safeSpots.get(i));
					s.setParam("i", i);
					ActorControlExtraEvent acee = events.get(i);
					var prev = current;
					current = acee;
					s.setParam("safe", safeSpots.get(i));
					boolean same = current.getData0() == prev.getData0();
					if (same) {
						s.updateCall(letsDanceStay, current);
					}
					else {
						s.updateCall(letsDanceCross, current);
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9BDD, 0xA395) && aue.isFirstTarget());
				}
			});

	@NpcCastCallout(0xA75B)
	private final ModifiableCallout<AbilityCastStart> quarterBeats = ModifiableCallout.durationBasedCall("Quarter Beats", "Partners");
	@NpcCastCallout(0xA75D)
	private final ModifiableCallout<AbilityCastStart> eightBeats = ModifiableCallout.durationBasedCall("Eight Beats", "Spread");

	@NpcCastCallout({0xA76F, 0xA770})
	private final ModifiableCallout<AbilityCastStart> letsPose = ModifiableCallout.durationBasedCall("Let's Pose", "Raidwide");


	private final ModifiableCallout<AbilityCastStart> frogtourage1NsSafe = ModifiableCallout.durationBasedCall("Frogtourage 1: North-South Safe", "North/South Safe");
	private final ModifiableCallout<AbilityCastStart> frogtourage1EwSafe = ModifiableCallout.durationBasedCall("Frogtourage 1: East-West Safe", "East/West Safe");

	private final ModifiableCallout<AbilityCastStart> frogtourage2Hustle1 = ModifiableCallout.durationBasedCall("Frogtourage 2 Hustle 1: Safe Spot", "{safe} Safe");
	private final ModifiableCallout<AbilityCastStart> frogtourage2Hustle2 = ModifiableCallout.durationBasedCall("Frogtourage 2 Hustle 2: Safe Spot", "{safe} Safe");
	private final ModifiableCallout<AbilityCastStart> frogtourage2Hustle3 = ModifiableCallout.<AbilityCastStart>durationBasedCall("Frogtourage 2 Hustle 3: Safe Side", "{safe} Safe")
			.extendedDescription("""
					This is the single cast from the boss.""");

	private final ModifiableCallout<AbilityCastStart> frogtourage2NsInSafe = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 1: North-South Safe", "North/South In");
	private final ModifiableCallout<AbilityCastStart> frogtourage2EwInSafe = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 1: East-West Safe", "East/West In");
	private final ModifiableCallout<AbilityCastStart> frogtourage2NsOutSafe = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 1: North-South Safe", "North/South Out");
	private final ModifiableCallout<AbilityCastStart> frogtourage2EwOutSafe = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 1: East-West Safe", "East/West Out");
	private final ModifiableCallout<AbilityCastStart> frogtourage2NsInSafe2 = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 2: North-South Safe", "North/South In");
	private final ModifiableCallout<AbilityCastStart> frogtourage2EwInSafe2 = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 2: East-West Safe", "East/West In");
	private final ModifiableCallout<AbilityCastStart> frogtourage2NsOutSafe2 = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 2: North-South Safe", "North/South Out");
	private final ModifiableCallout<AbilityCastStart> frogtourage2EwOutSafe2 = ModifiableCallout.durationBasedCall("Frogtourage 2 Part 2: East-West Safe", "East/West Out");

	private final ModifiableCallout<AbilityCastStart> frogtourage2tripleHustle = ModifiableCallout.durationBasedCall("Frogtourage 2 Triple Hustle: Safe Spot", "Between {safe1} and {safe2}");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> frogtourageMoonburnSq = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA75F),
			(e1, s) -> {
				/*
					Frogtourage/Moonburn 1:
					Frogtourage A75F (TODO: make sure this ID isn't reused)
					Two frogs cast a vertical AoE, two frogs cast horizontal AoE
					The aoes can either split the middle (safe spots out on that axis)
						or be offset enough to make a safe spot between them.
					TBD: is it always one in and the other out?
					Frogs are in a fixed position. They cast Moonburn A773 (cleaving right),
						or A774 (cleaving left)
					Quarter/Eighth goes off when it resolves
				*/
				var moonburns = s.waitEvents(4, CastLocationDataEvent.class, clde -> clde.abilityIdMatches(0xA773, 0xA774));
				// N/S can be made unsafe by either horizontal frogs hitting outwards, or vertical frogs hitting inwards
				// However, since they happen in tandem (as either N/S or E/W must be left safe), we only need to consider these.
				boolean nsSafe = moonburns.stream().noneMatch(mb -> {
					double y = mb.getPos().y();
					// If we see something at y==87.5, that makes N/S unsafe, as that represents the north part of the
					// arena getting cleaved.
					return MathUtils.closeTo(y, 87.5, 1.0);
				});

				s.updateCall(nsSafe ? frogtourage1NsSafe : frogtourage1EwSafe, moonburns.get(0).originalEvent());
			}, (e1, s) -> {
				/*
					Moonburn 2:?
					Frogtourage A75F
					Frogs at intercards (is this always the case?)
						Two sets of two 180 degree cleaves
						Frogs cast A775 (cleaving right) or A776 (cleaving left)
						Second set of frogs do the same
						Boss casts A724 (cleaving right) or A725 (cleaving left)

				*/

				// First/Second Do the Hustle set
				{
					for (int i = 1; i <= 2; i++) {
						Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.cardinals);
						// TODO: might bebetter to use cast location
						List<AbilityCastStart> hustles = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA775, 0xA776));
						s.waitThenRefreshCombatants(200);
						for (AbilityCastStart hustle : hustles) {
							ArenaSector frogAt = ArenaPos.combatantFacing(state.getLatestCombatantData(hustle.getSource()));
							// If frog facing SE, and hitting left, we want to eliminate N and E.
							// If hitting right, we want to eliminate S and W
							boolean rightCleave = hustle.abilityIdMatches(0xA775);
							if (rightCleave) {
								safe.remove(frogAt.plusEighths(1));
								safe.remove(frogAt.plusEighths(3));
							}
							else {
								safe.remove(frogAt.plusEighths(-1));
								safe.remove(frogAt.plusEighths(-3));
							}
						}
						if (safe.size() == 1) {
							s.setParam("safe", safe.iterator().next());
						}
						else {
							log.error("Invalid hustle spots: {}", safe);
						}
						s.updateCall(i == 2 ? frogtourage2Hustle2 : frogtourage2Hustle1, hustles.get(0));
					}
				}
				// Third hustle (single from boss)
				{
					AbilityCastStart bossCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0xA724, 0xA725));
					s.setParam("safe", bossCast.abilityIdMatches(0xA724) ? ArenaSector.WEST : ArenaSector.EAST);
					s.updateCall(frogtourage2Hustle3, bossCast);
				}


				// First moonburns
				// Unlike the previous set, we only get one pair of frogs. Thus we can't check only a single position.
				// We also need to differentiate between N/S "in" and "out" as this affects the exact positioning and
				// size of the safe spot.
					/*
						Since we only have one set, there are eight total possiblities (four places the frogs can be,
						plus they can cleave in or out). However, this can be simplified a bit:
						if cleave X is ~97.5 or ~102.5, this is vertical cleave in middle, i.e. E/W safe
						If cleave Y is ~87.5 or ~112.5, this is a horizontal cleave outside, i.e. E/W safe
					*/
				{
					var moonburns = s.waitEvents(2, CastLocationDataEvent.class, clde -> clde.abilityIdMatches(0xA773, 0xA774));
					// If nsSafe == true, then it is either NS in (i.e. W/E out cleaved) or NS out (horizontal cleaves through center)
					// If false, it is EW in (N/S out cleaved) or EW out (vertical cleave through center)
					boolean nsSafe = moonburns.stream().noneMatch(
							// N/S outside cleaves
							mb -> MathUtils.closeTo(mb.getPos().y(), 87.5, 1.0)
							      // Inside vertical cleave
							      || MathUtils.closeTo(mb.getPos().x(), 97.5, 1.0));
					boolean inside = moonburns.stream().noneMatch(
							mb -> MathUtils.closeTo(mb.getPos().y(), 97.5, 1.0)
							      || MathUtils.closeTo(mb.getPos().x(), 97.5, 1.0));
					AbilityCastStart cast = moonburns.get(0).originalEvent();
					if (inside) {
						s.updateCall(nsSafe ? frogtourage2NsInSafe : frogtourage2EwInSafe, cast);
					}
					else {
						s.updateCall(nsSafe ? frogtourage2NsOutSafe : frogtourage2EwOutSafe, cast);
					}
				}
				// Second set now
				{
					var moonburns = s.waitEvents(2, CastLocationDataEvent.class, clde -> clde.abilityIdMatches(0xA773, 0xA774));
					// If nsSafe == true, then it is either NS in (i.e. W/E out cleaved) or NS out (horizontal cleaves through center)
					// If false, it is EW in (N/S out cleaved) or EW out (vertical cleave through center)
					boolean nsSafe = moonburns.stream().noneMatch(
							// N/S outside cleaves
							mb -> MathUtils.closeTo(mb.getPos().y(), 87.5, 1.0)
							      // Inside vertical cleave
							      || MathUtils.closeTo(mb.getPos().x(), 97.5, 1.0));
					boolean inside = moonburns.stream().noneMatch(
							mb -> MathUtils.closeTo(mb.getPos().y(), 97.5, 1.0)
							      || MathUtils.closeTo(mb.getPos().x(), 97.5, 1.0));
					AbilityCastStart cast = moonburns.get(0).originalEvent();
					if (inside) {
						s.updateCall(nsSafe ? frogtourage2NsInSafe2 : frogtourage2EwInSafe2, cast);
					}
					else {
						s.updateCall(nsSafe ? frogtourage2NsOutSafe2 : frogtourage2EwOutSafe2, cast);
					}

				}

				// Finally, triple hustle - two from frogs, one from boss
				// This will need an inter-intercardinal call, i.e. E-SE
				{
					List<CastLocationDataEvent> hustles = s.waitEvents(3, CastLocationDataEvent.class, acs -> acs.abilityIdMatches(0xA724, 0xA725, 0xA775, 0xA776));
					// The way we can calculate this in the existing ArenaSector system is not too difficult.
					// Simply rotate every cast angle by 1/16 of a circle CCW (positive angle), then the safe spot is between the resulting
					// sector and the sector CW that one. This also avoids the awkward half-sector-safe nature of the
					// 180 degree cleaves.
					Set<ArenaSector> safe = EnumSet.copyOf(ArenaSector.all);
					for (CastLocationDataEvent hustle : hustles) {
						double angle = hustle.getBestHeading();
						// Remove the sector where it is facing, the two adjacent to that, and the sector 90 degrees CCW
						safe.remove(ArenaPos.combatantFacing(angle));
						safe.remove(ArenaPos.combatantFacing(angle + Math.PI / 2.0));
						safe.remove(ArenaPos.combatantFacing(angle + Math.PI / 4.0));
						safe.remove(ArenaPos.combatantFacing(angle - Math.PI / 4.0));
					}
					if (safe.size() == 1) {
						ArenaSector mainSafe = safe.iterator().next();
						s.setParam("safe1", mainSafe);
						s.setParam("safe2", mainSafe.plusEighths(1));
						s.updateCall(frogtourage2tripleHustle, hustles.get(0).originalEvent());
					}
					else {
						log.error("Invalid hustles: {}", safe);
					}

				}

			});


	@NpcCastCallout(0xA779)
	private final ModifiableCallout<AbilityCastStart> highNrgFever = ModifiableCallout.durationBasedCall("Hi-NRG Fever", "Enrage");


	/*
		Missing a couple Inside Out/Outside In casts
	*/
}
