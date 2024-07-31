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
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@CalloutRepo(name = "M2S", duty = KnownDuty.M2S)
public class M2S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(M2S.class);

	public M2S(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	private XivState state;
	private StatusEffectRepository buffs;

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.M2S);
	}

	//
	@NpcCastCallout(0x9183)
	private final ModifiableCallout<AbilityCastStart> raidwide = ModifiableCallout.durationBasedCall("Call Me Honey", "Raidwide");
	@NpcCastCallout(0x919B)
	private final ModifiableCallout<AbilityCastStart> stingingSlash = ModifiableCallout.durationBasedCall("Stinging Slash", "Tank Cleaves");
	@NpcCastCallout(0x919C)
	private final ModifiableCallout<AbilityCastStart> killerSting = ModifiableCallout.durationBasedCall("Killer Sting", "Tank Stack");
//
//	private final SequentialTrigger<BaseEvent>

	private enum PoisonBuff {
		SPREAD,
		BUDDY
	}

	private @Nullable PoisonBuff getPoisonBuff() {
		// TODO: this should also call out when you get the buff
		XivCombatant boss = state.npcById(16941);
		// TODO: This is bad! Both abilities place the same debuff ID, just capture the ability.
		BuffApplied status = buffs.findStatusOnTarget(boss, ba -> ba.buffIdMatches(0xF4B, 0xF61));
		if (status == null) {
			log.warn("No debuff");
			return null;
		}
		AbilityUsedEvent preapp = status.getPreAppAbility();
		if (preapp == null) {
			log.warn("Preapp null");
			return null;
		}
		if (preapp.abilityIdMatches(0x9184, 0x9B0F)) {
			return PoisonBuff.SPREAD;
		}
		else if (preapp.abilityIdMatches(0x9185, 0x9B09)) {
			return PoisonBuff.BUDDY;
		}
		else {
			log.warn("Preapp did not match");
			return null;
		}
	}

	private int getPlayerHeartStacks() {
		// F52 is the 0 stack hidden debuff
		var buff = buffs.findStatusOnTarget(state.getPlayer(), ba -> ba.buffIdMatches(0xF52, 0xF53, 0xF54, 0xF55, 0xF56));
		if (buff == null) {
			return 0;
		}
		else {
			return (int) (buff.getBuff().getId() - 0xF52);
		}

	}

	private final ModifiableCallout<AbilityCastStart> temptingTwistInitial = ModifiableCallout.durationBasedCall("Tempting Twist: Initial", "In");
//	private final ModifiableCallout<?> temptingTwistAvoidBlobs = new ModifiableCallout<>("Tempting Twist: Initial", "Avoid Blobs");
	private final ModifiableCallout<AbilityCastStart> beelineInitial = ModifiableCallout.durationBasedCall("Beeline: Initial", "Out of Middle");
//	private final ModifiableCallout<?> beelineAvoidBlobs = new ModifiableCallout<>("Tempting Twist: Initial", "In - Avoid Blobs");
	private final ModifiableCallout<?> spread = new ModifiableCallout<>("Poison: Spread", "Spread", 10_000);
	private final ModifiableCallout<?> buddies = new ModifiableCallout<>("Poison: Stack", "Buddy", 10_000);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> temptingTwist = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9187, 0x9B11),
			(e1, s) -> {
				s.updateCall(temptingTwistInitial, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				// Wait for it to go off
//				s.updateCall(temptingTwistAvoidBlobs);
				// Wait
				PoisonBuff pb = getPoisonBuff();
//				s.waitMs(1000);
				if (pb == PoisonBuff.SPREAD) {
					s.updateCall(spread);
				}
				else {
					s.updateCall(buddies);
				}

			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> beeline = SqtTemplates.sq(30_000,
			// TODO: 9B0C is unconfirmed
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9186, 0x9B0C),
			(e1, s) -> {
				s.updateCall(beelineInitial, e1);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.getPrecursor() == e1);
				// Wait for it to go off
//				s.updateCall(beelineAvoidBlobs);
				// Wait
				PoisonBuff pb = getPoisonBuff();
				if (pb == PoisonBuff.SPREAD) {
					s.updateCall(spread);
				}
				else {
					s.updateCall(buddies);
				}

			});

	@NpcCastCallout(value = 0x91A9, suppressMs = 100)
	private final ModifiableCallout<AbilityCastStart> beeSting = ModifiableCallout.durationBasedCall("Bee Sting", "Light Parties");

	private final ModifiableCallout<?> outCards = new ModifiableCallout<>("Center/Outer Stage: Out+Cardinals", "Out+Cardinals");
//	private final ModifiableCallout<?> outInter = new ModifiableCallout<>("Center/Outer Stage: Out+Intercards", "Out+Intercards");
//	private final ModifiableCallout<?> inCards = new ModifiableCallout<>("Center/Outer Stage: In+Cardinals", "In+Cardinals");
	private final ModifiableCallout<?> inInter = new ModifiableCallout<>("Center/Outer Stage: In+Intercards", "In+Intercards");
	private final ModifiableCallout<?> cross = new ModifiableCallout<>("Center/Outer Stage: Cross", "Out+Intercards");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> centerStage = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91AC),
			(e1, s) -> {
				s.updateCall(inInter);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(cross);
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(outCards);
				// TODO: are these always cards/inter?
				// in + intercards
				// out + intercards
				// out + cards
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> outerStage = SqtTemplates.sq(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91AD),
			(e1, s) -> {
				s.updateCall(outCards);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(cross);
				s.waitMs(1000);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x91B1, 0x91B2, 0x91B3, 0x91B4));
				s.updateCall(inInter);
				// TODO: are these always cards/inter?
				// out + cards
				// out + intercards
				// in + intercards
			});

	private static final Duration hblOffset = Duration.ofMillis(8200);
	// TODO: these have extra cast time
	private final ModifiableCallout<AbilityCastStart> hbl1Initial = ModifiableCallout.durationBasedCallWithOffset("Honey B. Live: 1st Beat", "Raidwide", hblOffset);
	private final ModifiableCallout<?> hb1towers = new ModifiableCallout<>("Honey B. Live: 1st Beat: Take Towers", "Take {towers} Towers");
	private final ModifiableCallout<?> hb1noTowers = new ModifiableCallout<>("Honey B. Live: 1st Beat: Avoid Towers", "Avoid Towers");
	private final ModifiableCallout<?> hb1stack = new ModifiableCallout<>("Honey B. Live: 1st Beat: Take Stack", "Take Stack");
	private final ModifiableCallout<AbilityCastStart> hb1noStack = new ModifiableCallout<>("Honey B. Live: 1st Beat: Don't Stack", "Don't Stack");
	private final ModifiableCallout<AbilityCastStart> hbl2Initial = ModifiableCallout.durationBasedCallWithOffset("Honey B. Live: 2nd Beat", "Raidwide", hblOffset);
	private final ModifiableCallout<AbilityCastStart> hbl3Initial = ModifiableCallout.durationBasedCallWithOffset("Honey B. Live: 3rd Beat", "Raidwide", hblOffset);
	@NpcCastCallout(0x918F)
	private final ModifiableCallout<AbilityCastStart> hbFinale = ModifiableCallout.durationBasedCall("Honey B. Finale", "Raidwide");

	@NpcCastCallout(0x9B7E)
	private final ModifiableCallout<AbilityCastStart> loveseeker = ModifiableCallout.durationBasedCall("Loveseeker", "Out");

	// TODO: red color for hard enrages?
	@NpcCastCallout(0x91B7)
	private final ModifiableCallout<AbilityCastStart> enrage = ModifiableCallout.durationBasedCall("Sheer Heart Attack", "Enrage");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> hbl1 = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C24),
			(e1, s) -> {
				s.updateCall(hbl1Initial, e1);
				// Starts with center/outer
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x919F));
				{
					int stacks = getPlayerHeartStacks();
					if (stacks < 2) {
						s.setParam("towers", 2 - stacks);
						s.updateCall(hb1towers);
					}
					else {
						s.updateCall(hb1noTowers);
					}
				}
				s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x919F));
				// Call out to stack if stacks < 3
				{
					int stacks = getPlayerHeartStacks();
					if (stacks < 3) {
						s.updateCall(hb1stack);
					}
					else {
						s.updateCall(hb1noStack);
					}
				}
			});

	private final ModifiableCallout<HeadMarkerEvent> hbl2stackOnYou = new ModifiableCallout<>("HBL2: Stack on You", "Stacks on {stacks.target}");
	private final ModifiableCallout<HeadMarkerEvent> hbl2noStackOnYouNoStar = new ModifiableCallout<>("HBL2: Not Stack, No Stars", "Stack with {stacks.target}");
	private final ModifiableCallout<HeadMarkerEvent> hbl2noStackOnYouHaveStar = new ModifiableCallout<>("HBL2: Not Stack, 1+ Stars", "Avoid Stacks");
	private final ModifiableCallout<HeadMarkerEvent> hbl2spreadOnYou = new ModifiableCallout<>("HBL2: Spread on You", "Spread");
	private final ModifiableCallout<AbilityCastStart> hbl2takeTower = ModifiableCallout.durationBasedCall("HBL2: Take Tower", "Take Tower");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> hbl2 = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C25),
			(e1, s) -> {
				s.updateCall(hbl2Initial, e1);
				// Puddles and stacks and towers
				// Starts with 2 stacks
				List<HeadMarkerEvent> stacks = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getTarget().isPc());
				s.setParam("stacks", stacks);
				var myStack = stacks.stream().filter(hm -> hm.getTarget().isThePlayer()).findFirst().orElse(null);
				if (myStack != null) {
					// If you have a stack marker, you have to stack
					s.updateCall(hbl2stackOnYou, myStack);
				}
				else {
					if (getPlayerHeartStacks() > 0) {
						// If you have heart stacks, you should avoid getting hit by the stack marker
						s.updateCall(hbl2noStackOnYouHaveStar, stacks.get(0));
					}
					else {
						// If you have no heart stacks, you should stack with one of the two people with stack markers
						s.updateCall(hbl2noStackOnYouNoStar, stacks.get(0));
					}
				}
				// Shortly thereafter, there are also 2 spread
				s.waitMs(100);
				List<HeadMarkerEvent> spreads = s.waitEventsQuickSuccession(2, HeadMarkerEvent.class, hme -> hme.getTarget().isPc());
				s.setParam("spreads", spreads);
				HeadMarkerEvent mySpread = spreads.stream().filter(hm -> hm.getTarget().isThePlayer()).findFirst().orElse(null);
				if (mySpread != null) {
					s.updateCall(hbl2spreadOnYou, mySpread);
				}
				else {
					var towerCast = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91A3));
					// Wait for the stacks to go off so we know who can take towers
					s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91A7));
					// It is only safe to take the tower if you do not have more than one heart stack at this point
					if (getPlayerHeartStacks() <= 1) {
						s.updateCall(hbl2takeTower, towerCast);
					}
				}
				// At this point, everyone should have two stacks
				// Beeline/Twist handled by other triggers, and that gives you a 3rd stack
				// Finale handled by other triggers
			});

	private final ModifiableCallout<BuffApplied> hbl3shortInitial = ModifiableCallout.<BuffApplied>durationBasedCall("HBL3: Short Defamation Initial", "Short Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hbl3longInitial = ModifiableCallout.<BuffApplied>durationBasedCall("HBL3: Long Defamation Initial", "Long Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hbl3defamationNow = ModifiableCallout.<BuffApplied>durationBasedCall("HBL3: Defamation Now", "Out").autoIcon();
	private final ModifiableCallout<?> hbl3avoidDefa = new ModifiableCallout<>("HBL3: Avoid Defa", "Avoid Defamation");
	private final ModifiableCallout<?> hbl3avoidTowers = new ModifiableCallout<>("HBL3: Avoid Towers", "Avoid Towers");
	private final ModifiableCallout<?> hbl3soakTowers = new ModifiableCallout<>("HBL3: Soak Towers", "Soak Tower");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> hbl3 = SqtTemplates.sq(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x9C26),
			(e1, s) -> {
				s.updateCall(hbl3Initial, e1);
				// Given that there is a magic vuln, I do not see how there can be different strategies for this mech
				// 4 people have short defa, 4 have long defa
				int defa = 0xF5E;
				List<BuffApplied> all = s.waitEventsQuickSuccession(8, BuffApplied.class, ba -> ba.buffIdMatches(defa));
				BuffApplied playerBuff = buffs.findStatusOnTarget(state.getPlayer(), defa);
//				List<BuffApplied> shorts = all.stream().filter(ba -> ba.getInitialDuration().toSeconds() < 30).toList();
//				List<BuffApplied> longs = all.stream().filter(ba -> ba.getInitialDuration().toSeconds() > 30).toList();
				boolean playerShort;
				if (playerBuff == null) {
					throw new RuntimeException("Player did not have defamation!");
				}
				else if (playerBuff.getInitialDuration().toSeconds() < 30) {
					playerShort = true;
					s.setParam("playerShort", true);
					s.updateCall(hbl3shortInitial, playerBuff);
				}
				else {
					playerShort = false;
					s.setParam("playerShort", false);
					s.updateCall(hbl3longInitial, playerBuff);
				}
				// Outer/center handled elsewhere
				s.waitMs(21_000);
				if (playerShort) {
					s.updateCall(hbl3defamationNow, playerBuff);
				}
				else {
					s.updateCall(hbl3avoidDefa);
				}
				// TODO: these are broken because they don't wait for the defamation to naturally go off. A player dying
				// will also trigger these early.
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(defa));
				if (playerShort) {
					s.updateCall(hbl3avoidTowers);
				}
				else {
					s.updateCall(hbl3soakTowers);
				}
				// Another outer/center handled elsewhere
				if (!playerShort) {
					s.updateCall(hbl3defamationNow, playerBuff);
				}
				else {
					s.updateCall(hbl3avoidDefa);
				}
				s.waitEvent(BuffRemoved.class, br -> br.buffIdMatches(defa));
				if (!playerShort) {
					s.updateCall(hbl3avoidTowers);
				}
				else {
					s.updateCall(hbl3soakTowers);
				}
			});


	private final ModifiableCallout<AbilityCastStart> rottenInitial = ModifiableCallout.durationBasedCallWithOffset("Rotten Heart: Initial", "Raidwide", Duration.ofMillis(3600));
	private final ModifiableCallout<BuffApplied> rottenInitialPartner = ModifiableCallout.<BuffApplied>durationBasedCall("Rotten Heart: Upfront Partner Call", "Group {group} with {buddy}").autoIcon();
	private final ModifiableCallout<BuffApplied> rottenPopNow = ModifiableCallout.<BuffApplied>durationBasedCall("Rotten Heart: Pop Now", "Pop with {buddy}").autoIcon();
	private final ModifiableCallout<BuffApplied> rottenPopOther = ModifiableCallout.<BuffApplied>durationBasedCall("Rotten Heart: Other Groups", "Group {currentGroup}: {first} and {second}")
			.disabledByDefault()
			.extendedDescription("This is an optional callout which will call out every pair to pop.");

	private int nisiBuffGroup(BuffApplied buff) {
		return ((int) buff.getInitialDuration().toSeconds() + 4) / 16;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> rotten = SqtTemplates.sq(120_000,
			// TODO: wrong ID
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x91AA),
			(e1, s) -> {
				s.updateCall(rottenInitial, e1);
				// nisi debuff matching mechanic
				List<BuffApplied> allNisiBuffs = s.waitEventsQuickSuccession(8, BuffApplied.class, ba -> ba.buffIdMatches(0xF5C, 0xF5D));
				BuffApplied playerBuff = allNisiBuffs.stream()
						.filter(ba -> ba.getTarget().isThePlayer())
						.findFirst()
						.orElseThrow(() -> new RuntimeException("Could not find player nisi!"));

				// 1 through 4 inclusive, which group the player is in
				int playerGroup = nisiBuffGroup(playerBuff);
				s.setParam("group", playerGroup);

				BuffApplied buddyBuff = allNisiBuffs.stream()
						.filter(ba -> !ba.getTarget().isThePlayer()
						              && ba.getInitialDuration().toSeconds() == playerBuff.getInitialDuration().toSeconds())
						.findFirst()
						.orElseThrow(() -> new RuntimeException("Could not find buddy nisi!"));
				s.setParam("buddy", buddyBuff.getTarget());
				// Initial partner callout
				s.updateCall(rottenInitialPartner, playerBuff);
				s.waitMs(2500);
				for (int i = 1; i <= 4; i++) {
					int currentGroup = i;
					s.setParam("currentGroup", currentGroup);
					// Iterate through groups
					if (playerGroup == 1) {
						s.updateCall(rottenPopNow, playerBuff);
					}
					else {
						// This should not replace other calls
						var thisGroupBuffs = buffs.findBuffs(ba -> ba.buffIdMatches(0xF5C, 0xF5D) && nisiBuffGroup(ba) == currentGroup);
						if (thisGroupBuffs.size() == 2) {
							s.setParam("first", thisGroupBuffs.get(0).getTarget());
							s.setParam("second", thisGroupBuffs.get(1).getTarget());
							s.call(rottenPopOther, thisGroupBuffs.get(0));
						}
						else {
							log.error("thisGroupBuffs does not have a size of 2: {}", thisGroupBuffs);
						}
					}
					// Wait for raidwide then continue to next group
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x9183));
					s.waitMs(200);


				}
			});

	private final ModifiableCallout<AbilityCastStart> alarm1initial = ModifiableCallout.durationBasedCall("Alarm Pheremones 1: Initial", "Bait Lines");
	private final ModifiableCallout<AbilityCastStart> alarm2initial = ModifiableCallout.durationBasedCall("Alarm Pheremones 2: Initial", "Bait Lines and Puddles");
	private final ModifiableCallout<HeadMarkerEvent> alarm2puddle = new ModifiableCallout<>("Alarm Pheremones 2: Puddle on You", "Drop Puddle");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> alarmPheremones = SqtTemplates.multiInvocation(120_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x917D),
			(e1, s) -> {
				s.updateCall(alarm1initial, e1);
				// First set seems yolo?
			}, (e1, s) -> {
				s.updateCall(alarm2initial, e1);
				// Second set seems to be the one with coordinated stuff
				var hm = s.waitEvent(HeadMarkerEvent.class, hme -> hme.getTarget().isThePlayer());
				s.updateCall(alarm2puddle, hm);
			});
	// all 3 lives have initial raidwides
	// they also end with a finale which is another raidwide
	/*
	Alarm pheremones
	#1 is 917D
	#2 is 917D
	#1 is yolo
	#2 is puddles plus healer stacks
	 */
	/*
	Live 1 0x9C24
	Debuff F53 and up
	Cycle
	Take towers if you can
	Dodge hearts
	Players with <3 take stack
	 */
	/*
	Live 2
	Party stacks with aoes
	Spreads with towers
	Beeline/in
	TODO: are these always spread
	 */
	/*
	Live 3
		Debuffs go out
		Cycle
		Everyone else middle
		Defamations THEN tower
		Cycle
		Defamations THEN tower
		Beeline or in
	 */

	/*
	Rotten:
	Do not be stacked at any point
	It has nisis
	Join alpha to beta based on debuff timers
	There is a magic vuln, so wait for next one to expire
	Call me honey casts during it (raidwides)
	 */
}
