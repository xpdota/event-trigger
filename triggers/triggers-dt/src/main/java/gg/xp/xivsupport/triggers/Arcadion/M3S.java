package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@CalloutRepo(name = "M3S", duty = KnownDuty.M3S)
public class M3S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M3S.class);

	public M3S(XivState state, StatusEffectRepository buffs, ActiveCastRepository casts) {
		this.state = state;
		this.buffs = buffs;
		this.casts = casts;
	}

	private XivState state;
	private StatusEffectRepository buffs;
	private ActiveCastRepository casts;
	private static final ArenaPos ap = new ArenaPos(100, 100, 5, 5);

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M3S);
	}

	@NpcCastCallout(0x93EB)
	private final ModifiableCallout<AbilityCastStart> quadrupleLariatIn = ModifiableCallout.durationBasedCall("Quadruple Lariat In", "In and Partners");
	@NpcCastCallout(0x93EA)
	private final ModifiableCallout<AbilityCastStart> quadrupleLariatOut = ModifiableCallout.durationBasedCall("Quadruple Lariat Out", "Out and Partners");
	// Boss cast is 93D8 but it is shorter duration
	@NpcCastCallout(0x93E9)
	private final ModifiableCallout<AbilityCastStart> octupleLariatIn = ModifiableCallout.durationBasedCall("Octuple Lariat In", "In and Spread");
	@NpcCastCallout(0x93E8)
	private final ModifiableCallout<AbilityCastStart> octupleLariatOut = ModifiableCallout.durationBasedCall("Octuple Lariat Out", "Out and Spread");
	@NpcCastCallout(0x9425)
	private final ModifiableCallout<AbilityCastStart> brutalImpact = ModifiableCallout.durationBasedCall("Brutal Impact", "Raidwide - Multi Hit");
	@NpcCastCallout(0x9423)
	private final ModifiableCallout<AbilityCastStart> knuckleSandwich = ModifiableCallout.durationBasedCall("Knuckle Sandwich", "Tank Buster - Multi Hit");

	// Real is 93E0
	@NpcCastCallout(0x93F5)
	private final ModifiableCallout<AbilityCastStart> quadroboomDiveOut = ModifiableCallout.durationBasedCall("Quadroboom Dive (Out)", "Out into Role Pairs");
	@NpcCastCallout(0x93F6)
	private final ModifiableCallout<AbilityCastStart> quadroboomDiveKb = ModifiableCallout.durationBasedCall("Quadroboom Dive (KB)", "Knockback into Role Pairs");
	// Real is 93EF
	@NpcCastCallout(0x93EC)
	private final ModifiableCallout<AbilityCastStart> octoboomDiveOut = ModifiableCallout.durationBasedCall("Quadroboom Dive (Out)", "Out into Spreads");
	@NpcCastCallout(0x93ED)
	private final ModifiableCallout<AbilityCastStart> octoboomDiveKb = ModifiableCallout.durationBasedCall("Quadroboom Dive (KB)", "Knockback into Spreads");

	private final ModifiableCallout<AbilityCastStart> barbarousBarrageStart = ModifiableCallout.durationBasedCall("Barbarous Barrage: Initial", "Knockback Towers");
	private final ModifiableCallout<?> barbarousFirstTower = new ModifiableCallout<>("Barbarous Barrage: First Towers", "Side Towers to Corners");
	private final ModifiableCallout<?> barbarousSecondTower = new ModifiableCallout<>("Barbarous Barrage: Second Towers", "Corner Towers to Center");
	private final ModifiableCallout<?> barbarousThirdTower = new ModifiableCallout<>("Barbarous Barrage: Third Towers + Mist", "Center Tower to {safe}");
	private final ModifiableCallout<AbilityCastStart> barbarousBarrage2Start = ModifiableCallout.durationBasedCall("Barbarous Barrage 2: Initial", "Knockback Towers");
	private final ModifiableCallout<?> barbarous2FirstTower = new ModifiableCallout<>("Barbarous Barrage 2: First Towers", "Side Towers to Corners");
	private final ModifiableCallout<?> barbarous2SecondTower = new ModifiableCallout<>("Barbarous Barrage 2: Second Towers", "Corner Towers to Center");
	private final ModifiableCallout<?> barbarous2ThirdTower = new ModifiableCallout<>("Barbarous Barrage 2: Third Towers + Lariat", "Center Tower to {safe}");
	private final ModifiableCallout<?> barbarous2SecondLariat = new ModifiableCallout<>("Barbarous Barrage 2: Second Lariat", "{safe} Safe");

	// TODO: test this
	@AutoFeed
	private final SequentialTrigger<BaseEvent> barbarousBarrage = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x93FB),
			(e1, s) -> {
				// In the first instance, you need to end up behind the boss so that you avoid his 270deg cleave.
				s.updateCall(barbarousBarrageStart, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				s.updateCall(barbarousFirstTower);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x968E, 0x93FC));
				s.updateCall(barbarousSecondTower);
				s.waitMs(100);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x968E, 0x93FC));
				s.waitThenRefreshCombatants(100);
				var mistCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x93FE), false);
				var safe = ap.forCombatant(mistCast.getSource());
				s.setParam("safe", safe);
				s.updateCall(barbarousThirdTower);
			}, (e1, s) -> {
				// In the second instance, you get a lariat combo from an add.
				// Unlike Tag Team, this one can is not guaranteed to switch safe sides, i.e. you might stay on the
				// same side.
				// The only difference as far as the towers are concerned is that the final tower should knock you into
				// the safe spot, rather than behind the boss.
				s.updateCall(barbarousBarrage2Start, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				s.updateCall(barbarous2FirstTower);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x968E, 0x93FC));
				s.updateCall(barbarous2SecondTower);
				s.waitMs(100);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x968E, 0x93FC));
				s.waitThenRefreshCombatants(100);

				/*
				Initial casts:
				9AE8 - left safe then left safe
				9AE9 - left safe then right safe
				9AEA - right safe then right safe
				9AEB - right safe then left safe
				 */

				// TODO: boss can come from either direction
				var lariatCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9AE8, 0x9AE9, 0x9AEA, 0x9AEB), false);
				ArenaSector bossAt = ap.forCombatant(lariatCast.getSource());

				var firstSafe = bossAt.plusQuads(lariatCast.abilityIdMatches(0x9AE8, 0x9AE9) ? 1 : -1);
				var secondSafe = bossAt.opposite().plusQuads(lariatCast.abilityIdMatches(0x9AE8, 0x9AEB) ? 1 : -1);

				s.setParam("safe", firstSafe);
				s.updateCall(barbarous2ThirdTower);
				s.waitMs(5_000);
				s.setParam("safe", secondSafe);
				s.updateCall(barbarous2SecondLariat);
			});


	private final ModifiableCallout<?> tagTeamSafeSpot = new ModifiableCallout<>("Tag Team 1: Safe Spot", "{safe} safe");
	private final ModifiableCallout<?> tagTeamSafeSpot2 = new ModifiableCallout<>("Tag Team 1: Second Safe Spot", "{safe} safe");
	private final ModifiableCallout<?> tagTeam2bombSafe = new ModifiableCallout<>("Tag Team 2: Bomb Safe Spot", "{bombSafe} safe");
	private final ModifiableCallout<?> tagTeam2bombMove = new ModifiableCallout<>("Tag Team 2: Bomb Move", "Move");
	private final ModifiableCallout<?> tagTeam2SafeSpot = new ModifiableCallout<>("Tag Team 2: Safe Spot", "{safe} safe, Get Hit By Boss");
	private final ModifiableCallout<?> tagTeam2SafeSpot2 = new ModifiableCallout<>("Tag Team 2: Second Safe Spot", "{safe} safe");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> tagTeam = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x93E7),
			(e1, s) -> {
				log.info("Tag team: start");
				// The tether units do not move into place until after the "Chain Deathmatch" cast starts
//				var tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
//				var tetherAdd = tether.getTargetMatching(cbt -> !cbt.isThePlayer());
//				log.info("Tag team: Tethered to: {}", tetherAdd);
//				var otherAdd = state.npcsById(tetherAdd.getbNpcId()).stream().filter(cbt -> !(cbt.equals(tetherAdd))).findFirst().orElseThrow(() -> new RuntimeException("Could not find other add"));
//				log.info("Tag team: Other add: {} at {}", otherAdd, otherAdd.getPos());

				// The tether comes out earlier, but we can't do anything with the information anyway, so just use the
				// buff instead since we don't have to figure out what the real unit is.
				BuffApplied myBuff = s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xFB3) && ba.getTarget().isThePlayer());
				// TODO: test this correction
				List<AbilityCastStart> allCasts = s.waitEvents(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9b2c, 0x9b2e));

				// The one that we want to get hit by
				AbilityCastStart goodCast = allCasts.stream().filter(acs -> acs.getSource().equals(myBuff.getSource())).findFirst().orElseThrow();
				// The one that we don't want to get hit by
				AbilityCastStart badCast = allCasts.stream().filter(acs -> !acs.getSource().equals(myBuff.getSource())).findFirst().orElseThrow();

				ArenaSector goodArea = ap.forCombatant(goodCast.getSource()).plusQuads(
						// If the enemy is hitting the right, that is 1 quarter CCW
						goodCast.abilityIdMatches(0x9b2c) ? -1 : 1
				);
				ArenaSector badArea = ap.forCombatant(badCast.getSource()).plusQuads(
						// If the enemy is hitting the right, that is 1 quarter CCW
						badCast.abilityIdMatches(0x9b2c) ? -1 : 1
				);

				// If good is N, and bad is E, then the spot we want is NW. So we take the opposite of E and combine it
				// with the good area.
				ArenaSector neededSpot = ArenaSector.tryCombineTwoCardinals(List.of(goodArea, badArea.opposite()));

				s.setParam("safe", neededSpot);
				s.updateCall(tagTeamSafeSpot);

				s.waitCastFinished(casts, goodCast);

				// The cleaves always change direction (they hit with the same arm, but from the opposite side of the
				// arena). Therefore, the safe spot for the second round is the quadrant where both attacks had
				// originally hit.
				ArenaSector secondSafeSpot = ArenaSector.tryCombineTwoCardinals(List.of(goodArea, badArea));

				s.setParam("safe", secondSafeSpot);
				s.updateCall(tagTeamSafeSpot2);

				// Next part is to avoid both hits - is this always the spot that got double hit initially?
				// The hits aren't obvious - maybe it's 9B34 lariat combo?
				// 9b34/9b2c might just be right?
				// 9b35/9b2e might be left?
				// At least one seems to be missing its cast location data
				// 2c/2e seem to come from the same units that the buff comes from
			}, (e1, s) -> {
				// Double tether version
				// Starts with Fuses of Fury 93FF before the Tag Team 93E7 cast
				try {
					s.setParam("bombSafe", getBombSafeQuadrant(s));
				}
				catch (Throwable t) {
					log.error("Error finding safe spot for bombs", t);
				}
				s.updateCall(tagTeam2bombSafe);
				// wait for explosions
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9401, 0x9402));
				s.updateCall(tagTeam2bombMove);

				BuffApplied myBuff = s.findOrWaitForBuff(buffs,
						ba -> ba.buffIdMatches(0xFB3)
						      && ba.getTarget().isThePlayer()
						      // We only want the one from an add, not from the boss
						      && ba.getSource().npcIdMatches(17099));

				List<AbilityCastStart> allCasts = s.waitEventsQuickSuccession(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9BD8, 0x9BDA));

				// The one that we want to get hit by
				AbilityCastStart goodCast = allCasts.stream().filter(acs -> acs.getSource().equals(myBuff.getSource())).findFirst().orElseThrow();
				// The one that we don't want to get hit by
				AbilityCastStart badCast = allCasts.stream().filter(acs -> !acs.getSource().equals(myBuff.getSource())).findFirst().orElseThrow();

				ArenaSector goodArea = ap.forCombatant(goodCast.getSource()).plusQuads(
						// If the enemy is hitting the right, that is 1 quarter CCW
						goodCast.abilityIdMatches(0x9bd8) ? -1 : 1
				);
				ArenaSector badArea = ap.forCombatant(badCast.getSource()).plusQuads(
						// If the enemy is hitting the right, that is 1 quarter CCW
						badCast.abilityIdMatches(0x9bd8) ? -1 : 1
				);

				// TODO: might be nice to call out which way to lean if you're in a corner that is only half-good

				// If good is N, and bad is E, then the spot we want is NW. So we take the opposite of E and combine it
				// with the good area.
				ArenaSector neededSpot = ArenaSector.tryCombineTwoCardinals(List.of(goodArea, badArea.opposite()));

				s.setParam("safe", neededSpot);
				s.updateCall(tagTeam2SafeSpot);

				s.waitCastFinished(casts, goodCast);

				// The cleaves always change direction (they hit with the same arm, but from the opposite side of the
				// arena). Therefore, the safe spot for the second round is the quadrant where both attacks had
				// originally hit.
				ArenaSector secondSafeSpot = ArenaSector.tryCombineTwoCardinals(List.of(goodArea, badArea));

				s.setParam("safe", secondSafeSpot);
				s.updateCall(tagTeam2SafeSpot2);

			});

	private final ModifiableCallout<?> finalFusedownLong = new ModifiableCallout<>("Final Fusedown: Long Fuse", "{safe} Safe, Long Fuse");
	private final ModifiableCallout<?> finalFusedownShort = new ModifiableCallout<>("Final Fusedown: Short Fuse", "Short Fuse, Spread {safe}")
			.extendedDescription("""
					This calls out the large safe spot by default. You can have it mention the small safe spot (opposite \
					the large safe spot with {safe.opposite()}.""");
	private final ModifiableCallout<?> finalFusedownLong2 = new ModifiableCallout<>("Final Fusedown: Long Fuse, Second Set", "Spread");
	private final ModifiableCallout<?> finalFusedownShort2 = new ModifiableCallout<>("Final Fusedown: Short Fuse", "Move");

	private ArenaSector getBombSafeQuadrant(SequentialTriggerController<?> stc) {
		List<BuffApplied> fuseBuffs = buffs.findBuffs(ba -> ba.getTarget().npcIdMatches(17095)
		                                                    // FAF short, FB0 long
		                                                    && ba.buffIdMatches(0xFAF, 0xFB0));
		// Value true indicates long fuse
		Map<ArenaSector, Boolean> bombMap = new EnumMap<>(ArenaSector.class);
		for (BuffApplied fuseBuff : fuseBuffs) {
			XivCombatant fuseNpc = fuseBuff.getTarget();
			Position location = state.getLatestCombatantData(fuseNpc).getPos();
			ArenaSector where = ap.forPosition(location);
			if (!where.isOutside()) {
				log.error("Invalid bomb location! {}", location);
				continue;
			}
			boolean isLong = fuseBuff.buffIdMatches(0xFB0);
			bombMap.put(where, isLong);
		}
		if (bombMap.size() == 8) {
			// If the corner is safe, as well as its adjacent cardinals, that is the large safe spot.
			// The small safe spot in the corner is always opposite of that, but we don't need to deal with
			// that in the trigger logic.
			return ArenaSector.quadrants
					.stream()
					.filter(quadrant -> bombMap.get(quadrant)
					                    && bombMap.get(quadrant.plusEighths(1))
					                    && bombMap.get(quadrant.plusEighths(-1)))
					.findFirst()
					.orElseThrow(() -> new RuntimeException("No safe spot found! %s".formatted(bombMap)));
		}
		else {
			throw new RuntimeException("Invalid bomb mapping! %s".formatted(bombMap));
		}

	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> finalFusedown = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9406),
			(e1, s) -> {
				log.info("Final fusedown: start");
				s.waitCastFinished(casts, e1);
				s.waitMs(1000);
				// Old, slower
//				var playerBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xFAF, 0xFB0));
				// FB8 = short, FB9 = long
				var playerBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xFB8, 0xFB9));
				s.waitThenRefreshCombatants(100);
				// Player has long
				boolean playerLong = playerBuff.buffIdMatches(0xFB9);
				if (state.playerJobMatches(Job::isDps)) {
					// DPS has long
					log.info("DPS long");
				}
				else {
					// Supports have long
					log.info("Supports long");
				}
				// TODO: determine safe spot
				try {
					s.setParam("safe", getBombSafeQuadrant(s));
				}
				catch (Throwable t) {
					log.error("Error finding safe spot for bombs", t);
				}

				if (playerLong) {
					s.updateCall(finalFusedownLong);
				}
				else {
					s.updateCall(finalFusedownShort);
				}
				// Wait for an explosion to go off.
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9401, 0x9402));
				if (playerLong) {
					s.updateCall(finalFusedownLong2);
				}
				else {
					s.updateCall(finalFusedownShort2);
				}
			});

	private final ModifiableCallout<AbilityCastStart> fuseFieldInitial = ModifiableCallout.durationBasedCall("Fuse Field: Initial", "Pop Fuses Sequentially");
	private final ModifiableCallout<BuffApplied> fuseFieldShort = ModifiableCallout.<BuffApplied>durationBasedCall("Fuse Field: Short", "Short Fuse").autoIcon();
	private final ModifiableCallout<BuffApplied> fuseFieldLong = ModifiableCallout.<BuffApplied>durationBasedCall("Fuse Field: Long", "Long Fuse").autoIcon();
	@AutoFeed
	private final SequentialTrigger<BaseEvent> fuseField = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x93EE),
			(e1, s) -> {
				s.updateCall(fuseFieldInitial, e1);
				var buff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xFB4));
				RawModifiedCallout<BuffApplied> call;
				if (buff.getInitialDuration().toSeconds() > 30) {
					call = s.updateCall(fuseFieldLong, buff);
				}
				else {
					call = s.updateCall(fuseFieldShort, buff);
				}
				s.waitBuffRemoved(buffs, buff);
				call.forceExpire();
			});

	// Bombs: role spread + avoid bombs

	/*
	Octoboom Bombarian Special: 9752
	Quadroboom Bombarian Special: 940A
	 */

	private final ModifiableCallout<AbilityCastStart> quadroboomBombarianSpecialInitial = ModifiableCallout.durationBasedCall("Quadroboom Bombarian Special", "Multiple Raidwides");
	private final ModifiableCallout<AbilityCastStart> quadroboomBombarianSpecialOut = ModifiableCallout.durationBasedCall("Quadroboom Bombarian Special: Out", "Out");
	private final ModifiableCallout<AbilityCastStart> quadroboomBombarianSpecialIn = ModifiableCallout.durationBasedCall("Quadroboom Bombarian Special: In", "In");
	private final ModifiableCallout<AbilityCastStart> quadroboomBombarianSpecialKb = ModifiableCallout.durationBasedCall("Quadroboom Bombarian Special: Knockback", "Knockback into Buddies");
	private final ModifiableCallout<?> quadroboomBombarianSpecialBuddies = new ModifiableCallout<>("Quadroboom Bombarian Special: Buddies", "Buddies");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> quadroboomBombarianSpecialSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x940A),
			(e1, s) -> {
				// Raidwides, out, in, kb, buddies
				s.updateCall(quadroboomBombarianSpecialInitial, e1);
				var outCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9410), false);
				s.waitDuration(outCast.getEstimatedRemainingDuration().minusSeconds(3));
				s.updateCall(quadroboomBombarianSpecialOut, outCast);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == outCast);
				var inCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9411), false);
				s.updateCall(quadroboomBombarianSpecialIn, inCast);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == inCast);
				var kbCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9414), false);
				s.updateCall(quadroboomBombarianSpecialKb, kbCast);
				s.waitCastFinished(casts, kbCast);
				s.updateCall(quadroboomBombarianSpecialBuddies);
			});

	private final ModifiableCallout<AbilityCastStart> octoboomBombarianSpecialInitial = ModifiableCallout.durationBasedCall("Octoboom Bombarian Special", "Multiple Raidwides");
	private final ModifiableCallout<AbilityCastStart> octoboomBombarianSpecialOut = ModifiableCallout.durationBasedCall("Octoboom Bombarian Special: Out", "Out");
	private final ModifiableCallout<AbilityCastStart> octoboomBombarianSpecialIn = ModifiableCallout.durationBasedCall("Octoboom Bombarian Special: In", "In");
	private final ModifiableCallout<AbilityCastStart> octoboomBombarianSpecialKb = ModifiableCallout.durationBasedCall("Octoboom Bombarian Special: Knockback", "Knockback into Spread");
	private final ModifiableCallout<?> octoboomBombarianSpecialSpread = new ModifiableCallout<>("Octoboom Bombarian Special: Spread", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> octoboomBombarianSpecialSq = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9752),
			(e1, s) -> {
				// Similar to quadroboom. Raidwides, out, in, knockback, spread
				s.updateCall(octoboomBombarianSpecialInitial, e1);
				var outCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9410), false);
				s.waitDuration(outCast.getEstimatedRemainingDuration().minusSeconds(3));
				s.updateCall(octoboomBombarianSpecialOut, outCast);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == outCast);
				var inCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9411), false);
				s.updateCall(octoboomBombarianSpecialIn, inCast);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == inCast);
				var kbCast = s.findOrWaitForCast(casts, acs -> acs.abilityIdMatches(0x9414), false);
				s.updateCall(octoboomBombarianSpecialKb, kbCast);
				s.waitCastFinished(casts, kbCast);
				s.updateCall(octoboomBombarianSpecialSpread);

			});

	private final ModifiableCallout<AbilityCastStart> fuseOrFoeInitial = ModifiableCallout.durationBasedCall("Fuse or Foe: Initial", "Spinning");
	private final ModifiableCallout<?> fuseOrFoeLongFuse = new ModifiableCallout<>("Fuse or Foe: Long Fuse", "Long Fuse");
	private final ModifiableCallout<?> fuseOrFoeShortFuse = new ModifiableCallout<>("Fuse or Foe: Short Fuse", "Short Fuse");
	private final ModifiableCallout<AbilityCastStart> fuseOrFoeCW = ModifiableCallout.durationBasedCall("Fuse or Foe: Clockwise Spin", "Clockwise");
	private final ModifiableCallout<AbilityCastStart> fuseOrFoeCCW = ModifiableCallout.durationBasedCall("Fuse or Foe: Counter-Clockwise Spin", "Counter-Clockwise");
	private final ModifiableCallout<?> fuseOrFoeSpreadShortWithLong = new ModifiableCallout<>("Fuse or Foe: Short Fuse Exploding (With Long)", "Spread");
	private final ModifiableCallout<?> fuseOrFoeSpreadShortWithShort = new ModifiableCallout<>("Fuse or Foe: Short Fuse Exploding (With Short)", "Spread");
	private final ModifiableCallout<?> fuseOrFoeSpreadLongWithLong = new ModifiableCallout<>("Fuse or Foe: Long Fuse Exploding (With Long)", "Spread");
	private final ModifiableCallout<?> fuseOrFoeSpreadLongWithShort = new ModifiableCallout<>("Fuse or Foe: Long Fuse Exploding (With Short)", "Spread");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> fuseOrFoe = SqtTemplates.sq(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9403),
			(e1, s) -> {
				s.updateCall(fuseOrFoeInitial, e1);
				var playerBuff = s.findOrWaitForBuff(buffs, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xFB8, 0xFB9));
				boolean playerLong = playerBuff.buffIdMatches(0xFB9);
				if (playerLong) {
					s.updateCall(fuseOrFoeLongFuse);
				}
				else {
					s.updateCall(fuseOrFoeShortFuse);
				}

				var spinCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x941C, 0x941D));
				if (spinCast.abilityIdMatches(0x941C)) {
					s.updateCall(fuseOrFoeCW, spinCast);
				}
				else {
					s.updateCall(fuseOrFoeCCW, spinCast);
				}

				s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0xFBA, 0xFBB));
				// 5 seconds on short, 10 seconds on long at this point
				s.waitMs(1_800);
				s.updateCall(playerLong ? fuseOrFoeSpreadShortWithLong : fuseOrFoeSpreadShortWithShort);
				s.waitMs(5_000);
				s.updateCall(playerLong ? fuseOrFoeSpreadLongWithLong : fuseOrFoeSpreadLongWithShort);
			});

	@NpcCastCallout(0x942B)
	private final ModifiableCallout<AbilityCastStart> specialBombarianSpecial = ModifiableCallout.durationBasedCall("Special Bombarian Special", "Multiple Raidwides");

	// debuffs + bombs
	// seems to be role assigned
	// FB1, FB2 and FBA, FBB debuffs seem to be related
	// FAF = 5 second FB1 on NPC
	// FB0 = 10 second FB2 on NPC
	// FB8 = 5 second FBA on player
	// FB9 = 10 second FBB on player
	// short debuff to short fuse
	// It's curtain call but with 2 debuff timers instead of four


	/*
	Knockback towers:
	Some towers go off earlier and you need to get knocked into the next set of towers
	Boss does a 270% cleave, behind him is safe? so get knocked back to him
	Pattern 1: sides, corners, center
	Pattern 2:
	 */
	/*
	Second chain - chained to boss and add, so need to get hit by boss's 270 degree cleave (then still dodge second set)

	 */
	/*
	Spinny mechanic TBD
	need to look at rotation and initial position, plus the cross-tthrough
	 */

	/*
	TODO: bombarian specials

	There is a towers + kb mechanic, but you dodge a side cleave instead of 270 at the end
	 */
}


