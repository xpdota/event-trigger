package gg.xp.xivsupport.events.triggers.duties.ewult;

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
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.PlayerStatusCallout;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@CalloutRepo(name = "TOP Triggers", duty = KnownDuty.OmegaProtocol)
public class OmegaUltimate extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(OmegaUltimate.class);

	// Looper
	private final ModifiableCallout<BuffApplied> firstInLineTower = ModifiableCallout.<BuffApplied>durationBasedCall("Loop First: Start/Tower", "One, Take Tower").statusIcon(0xBBC);
	private final ModifiableCallout<?> firstInLineTether = new ModifiableCallout<>("Loop First: Tether", "Take Tether").statusIcon(0xBBC);
	private final ModifiableCallout<?> secondInLineLoop = new ModifiableCallout<>("Loop Second: Start", "Two").statusIcon(0xBBD);
	private final ModifiableCallout<BuffApplied> secondInLineTower = ModifiableCallout.<BuffApplied>durationBasedCall("Loop Second: Tower", "Take Tower").statusIcon(0xBBD);
	private final ModifiableCallout<?> secondInLineTether = new ModifiableCallout<>("Loop Second: Tether", "Take tether").statusIcon(0xBBD);
	;
	private final ModifiableCallout<?> thirdInLineTether = new ModifiableCallout<>("Loop Third: Start/Tether", "Three, Take Tether").statusIcon(0xBBE);
	private final ModifiableCallout<BuffApplied> thirdInLineTower = ModifiableCallout.<BuffApplied>durationBasedCall("Loop Third: Tower", "Take Tower").statusIcon(0xBBE);
	;
	private final ModifiableCallout<?> fourthInLineLoop = new ModifiableCallout<>("Loop Fourth: Start", "Four").statusIcon(0xD7B);
	;
	private final ModifiableCallout<BuffApplied> fourthInLineTower = ModifiableCallout.<BuffApplied>durationBasedCall("Loop Fourth: Tower", "Take tower").statusIcon(0xD7B);
	;
	private final ModifiableCallout<?> fourthInLineTether = new ModifiableCallout<>("Loop Fourth: Tether", "Take tether").statusIcon(0xD7B);
	;
	//Pantokrator
	private final ModifiableCallout<BuffApplied> pantoFirstInLine = new ModifiableCallout<BuffApplied>("Panto First", "One - Missile now then Cannon", "One - Missile ({missile.estimatedRemainingDuration}) then Cannon ({cannon.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xBBC);
	private final ModifiableCallout<BuffApplied> pantoSecondInLine = new ModifiableCallout<BuffApplied>("Panto Second", "Two", "Two - Missile ({missile.estimatedRemainingDuration}) then Cannon ({cannon.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xBBD);
	private final ModifiableCallout<?> pantoSecondInLineOut = new ModifiableCallout<BuffApplied>("Panto Second: Out", "Missile Now then Cannon", "Missile Now Two - Missile ({missile.estimatedRemainingDuration}) then Cannon ({cannon.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD60);
	private final ModifiableCallout<BuffApplied> pantoThirdInLine = new ModifiableCallout<BuffApplied>("Panto Third", "Three", "Three - Cannon ({cannon.estimatedRemainingDuration}) then Missile ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xBBE);
	private final ModifiableCallout<?> pantoThirdInLineOut = new ModifiableCallout<BuffApplied>("Panto Third: Out", "Missile Now", "Missile Now ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD60);
	private final ModifiableCallout<BuffApplied> pantoFourthInLine = new ModifiableCallout<BuffApplied>("Panto Fourth", "Four", "Four - Cannon ({cannon.estimatedRemainingDuration}) then Missile ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD7B);
	private final ModifiableCallout<?> pantoFourthInLineOut = new ModifiableCallout<BuffApplied>("Panto Fourth: Out", "Missile Now", "Missile Now ({missile.estimatedRemainingDuration})", ModifiableCallout.expiresIn(15)).statusIcon(0xD60);
	//TODO: add "move back" trigger

	private final ModifiableCallout<?> pantoBuster1 = new ModifiableCallout<>("Panto Buster 1", "Buster and Baits");
	private final ModifiableCallout<?> pantoBuster2 = new ModifiableCallout<>("Panto Buster 2", "Buster and Baits");

	private final ModifiableCallout<BuffApplied> midGlitch = ModifiableCallout.durationBasedCall("Mid Glitch with Buddy", "Close to {tetherBuddy}");
	private final ModifiableCallout<BuffApplied> remoteGlitch = ModifiableCallout.durationBasedCall("Remote Glitch with Buddy", "Far from {tetherBuddy}");

	// Panto
//	@PlayerStatusCallout({0xDB3, 0xDB4, 0xDB5, 0xDB6})
//	private final ModifiableCallout<BuffApplied> waveCannonKyrios = new ModifiableCallout<BuffApplied>("Condensed Wave Cannon Kyrios", "Wave Cannon on You").autoIcon();
//	@PlayerStatusCallout({0xD60, 0xDA7, 0xDA8, 0xDA9})
//	private final ModifiableCallout<BuffApplied> guidedMissileKyrios = new ModifiableCallout<BuffApplied>("Guided Missile Kyrios", "Missile on You").autoIcon();

	// Mechanics
	@PlayerStatusCallout(0xDC8)
	private final ModifiableCallout<BuffApplied> cascadingLatentDefect = new ModifiableCallout<BuffApplied>("Cascading Latent Defect", "Pick Up Rot").autoIcon();
	@PlayerStatusCallout(0xDC5)
	private final ModifiableCallout<BuffApplied> criticalOverflowBug = new ModifiableCallout<BuffApplied>("Critical Overflow Bug", "Defamation").autoIcon();
	@PlayerStatusCallout(0xDC4)
	private final ModifiableCallout<BuffApplied> criticalSynchronizationBug = new ModifiableCallout<BuffApplied>("Critical Synchronization Bug", "Stack").autoIcon();
	@PlayerStatusCallout(0xDC6)
	private final ModifiableCallout<BuffApplied> criticalUnderflowBug = new ModifiableCallout<BuffApplied>("Critical Underflow Bug", "Rot on You").autoIcon();

	// Get hit by stuff
	@PlayerStatusCallout(0xDC7)
	private final ModifiableCallout<BuffApplied> latentDefect = new ModifiableCallout<BuffApplied>("Latent Defect", "Get Hit by Defamation").autoIcon();
	@PlayerStatusCallout(0xD6A)
	private final ModifiableCallout<BuffApplied> latentSynchronizationBug = new ModifiableCallout<BuffApplied>("Latent Synchronization Bug", "Stack").autoIcon();

	// Soon
	@PlayerStatusCallout(0xD6D)
	private final ModifiableCallout<BuffApplied> overflowCodeSmell = new ModifiableCallout<BuffApplied>("Overflow Code Smell", "Defamation Soon").autoIcon();
	@PlayerStatusCallout(0xD6C)
	private final ModifiableCallout<BuffApplied> synchronizationCodeSmell = new ModifiableCallout<BuffApplied>("Synchronization Code Smell", "Stack Soon").autoIcon();
	@PlayerStatusCallout(0xD6E)
	private final ModifiableCallout<BuffApplied> underflowCodeSmell = new ModifiableCallout<BuffApplied>("Underflow Code Smell", "Rot Soon").autoIcon();

	// Tethers
	@PlayerStatusCallout({0xD70, 0xDAF})
	private final ModifiableCallout<BuffApplied> localCodeSmell = new ModifiableCallout<BuffApplied>("Local Code Smell", "Close Tether Soon").autoIcon();
	@PlayerStatusCallout(0xDC9)
	private final ModifiableCallout<BuffApplied> localRegression = new ModifiableCallout<BuffApplied>("Local Regression", "Close Tether").autoIcon();
	@PlayerStatusCallout({0xD71, 0xDB0})
	private final ModifiableCallout<BuffApplied> remoteCodeSmell = new ModifiableCallout<BuffApplied>("Remote Code Smell", "Far Tether Soon").autoIcon();
	@PlayerStatusCallout(0xDCA)
	private final ModifiableCallout<BuffApplied> remoteRegression = new ModifiableCallout<BuffApplied>("Remote Regression", "Far Tether").autoIcon();

//	@PlayerStatusCallout(0xD62)
//	private final ModifiableCallout<BuffApplied> highPoweredSniperCannon = new ModifiableCallout<BuffApplied>("High-powered Sniper Cannon", "Super sniper soon").autoIcon();
//	@PlayerStatusCallout(0xD61)
//	private final ModifiableCallout<BuffApplied> sniperCannon = new ModifiableCallout<BuffApplied>("Sniper Cannon", "Sniper Soon").autoIcon();

	@PlayerStatusCallout(0xDAC)
	private final ModifiableCallout<BuffApplied> packetFilterF = new ModifiableCallout<BuffApplied>("Packet Filter F", "Attack M").autoIcon();
	@PlayerStatusCallout(0xDAB)
	private final ModifiableCallout<BuffApplied> packetFilterM = new ModifiableCallout<BuffApplied>("Packet Filter M", "Attack F").autoIcon();


	private final XivState state;
	private final StatusEffectRepository buffs;

	public OmegaUltimate(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.OmegaProtocol);
	}

	private static boolean isLineDebuff(BuffApplied ba) {
		return NumberInLine.debuffToLine(ba) != NumberInLine.UNKNOWN;
	}

	private enum NumberInLine {
		FIRST(1, 0xBBC),
		SECOND(2, 0xBBD),
		THIRD(3, 0xBBE),
		FOURTH(4, 0xD7B),
		UNKNOWN;

		private final int id;
		private final int lineNumber;

		NumberInLine(int lineNumber, int id) {
			this.id = id;
			this.lineNumber = lineNumber;
		}

		NumberInLine() {
			this.id = -1;
			this.lineNumber = -1;
		}

		int lineId() {
			return id;
		}

		static NumberInLine debuffToLine(BuffApplied ba) {
			int id = (int) ba.getBuff().getId();
			for (NumberInLine value : values()) {
				if (id == value.id) {
					return value;
				}
			}
			return UNKNOWN;
		}
	}

	//returns first player with line debuff
	private XivPlayerCharacter supPlayerFromLine(NumberInLine il) {
		List<XivPlayerCharacter> players = state.getPartyList().stream().filter(p -> p.getJob().isTank() || p.getJob().isHealer()).toList();
		int lineId = il.lineId();

		return players.stream()
				.filter(xpc -> this.buffs.findStatusOnTarget(xpc, lineId) != null)
				.findFirst()
				.orElse(null);
	}

	//returns first player with line debuff
	private XivPlayerCharacter dpsPlayerFromLine(NumberInLine il) {
		List<XivPlayerCharacter> players = state.getPartyList().stream().filter(p -> p.getJob().isDps()).toList();
		int lineId = il.lineId();

		return players.stream()
				.filter(xpc -> this.buffs.findStatusOnTarget(xpc, lineId) != null)
				.findFirst()
				.orElse(null);
	}

	private XivState getState() {
		return state;
	}

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> programLoopSq = SqtTemplates.sq(50_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x7B03),
			(e1, s) -> {
				log.info("Program Loop: Start");
				BuffApplied lineDebuff = s.waitEvent(BuffApplied.class, ba -> isLineDebuff(ba) && ba.getTarget().isThePlayer());
				s.waitMs(50);
				BuffApplied looperDebuff = getBuffs().findStatusOnTarget(getState().getPlayer(), 0xD80);
				NumberInLine num = NumberInLine.debuffToLine(lineDebuff);
				switch (num) {

					case FIRST -> s.accept(firstInLineTower.getModified(looperDebuff));
					case SECOND -> s.accept(secondInLineLoop.getModified());
					case THIRD -> s.accept(thirdInLineTether.getModified());
					case FOURTH -> s.accept(fourthInLineLoop.getModified());
					case UNKNOWN -> {
						log.error("Unknown number! {}", lineDebuff);
						return;
					}
				}

				//First tower goes off
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				if (num == NumberInLine.SECOND) {
					s.updateCall(secondInLineTower.getModified(looperDebuff));
				}
				else if (num == NumberInLine.FOURTH) {
					s.updateCall(fourthInLineTether.getModified());
				}

				//Second tower goes off
				s.waitMs(100);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				if (num == NumberInLine.THIRD) {
					s.updateCall(thirdInLineTower.getModified(looperDebuff));
				}
				else if (num == NumberInLine.FIRST) {
					s.updateCall(firstInLineTether.getModified());
				}

				//Third tower goes off
				s.waitMs(100);
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B04) && aue.isFirstTarget());
				if (num == NumberInLine.FOURTH) {
					s.updateCall(fourthInLineTower.getModified(looperDebuff));
				}
				else if (num == NumberInLine.SECOND) {
					s.updateCall(secondInLineTether.getModified());
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> pantokratorSq = SqtTemplates.sq(50_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x7B0B),
			(e1, s) -> {
				BuffApplied myLineBuff = s.waitEvent(BuffApplied.class, ba -> isLineDebuff(ba) && ba.getTarget().isThePlayer());
				NumberInLine number = NumberInLine.debuffToLine(myLineBuff);
				s.waitMs(100);
				BuffApplied guidedMissile = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD60, 0xDA7, 0xDA8, 0xDA9));
				BuffApplied waveCannon = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xDB3, 0xDB4, 0xDB5, 0xDB6));
				if (guidedMissile == null) {
					log.error("No guided missile!");
					return;
				}
				else if (waveCannon == null) {
					log.error("No wave cannon!");
					return;
				}

				// Guided missile is LONGER
				// First group is 12 missile, 24

				Map<String, Object> params = Map.of("missile", guidedMissile, "cannon", waveCannon);
				switch (number) {
					case FIRST -> s.updateCall(pantoFirstInLine.getModified(myLineBuff, params));
					case SECOND -> s.updateCall(pantoSecondInLine.getModified(myLineBuff, params));
					case THIRD -> s.updateCall(pantoThirdInLine.getModified(myLineBuff, params));
					case FOURTH -> s.updateCall(pantoFourthInLine.getModified(myLineBuff, params));
					case UNKNOWN -> {
						log.error("Unknown number in line!");
						return;
					}
				}

				for (int i = 1; i <= 4; i++) {
					// TODO doesn't stick on screen

					log.info("Iteration: {} vs {}", number.lineNumber, i);
					if (number.lineNumber == i) {
						switch (number) {
							case SECOND -> s.updateCall(pantoSecondInLineOut.getModified(params));
							case THIRD -> s.updateCall(pantoThirdInLineOut.getModified(params));
							case FOURTH -> s.updateCall(pantoFourthInLineOut.getModified(params));
						}
					}
					s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B0E) && aue.isFirstTarget());
					s.waitMs(100);
				}
				s.waitMs(1000);
				s.updateCall(pantoBuster1.getModified());
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x7B11) && aue.isFirstTarget());
				s.updateCall(pantoBuster2.getModified());
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> midRemoteGlitch = SqtTemplates.sq(50_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7B3F),
			(e1, s) -> {
				// TODO: playstation marker - probably same order as DSR
				// TODO: tether IDs - are there multiple?
//				TetherEvent tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer) && te.tetherIdMatches(0xDE));
				TetherEvent tether = s.waitEvent(TetherEvent.class, te -> te.eitherTargetMatches(XivCombatant::isThePlayer));
				XivCombatant buddy = tether.getTargetMatching(xpc -> !xpc.isThePlayer());
				Map<String, Object> params = Map.of("tetherBuddy", buddy);

				BuffApplied status = getBuffs().findStatusOnTarget(getState().getPlayer(), ba -> ba.buffIdMatches(0xD63, 0xD64));
				if (status.buffIdMatches(0xD63)) {
					s.updateCall(midGlitch.getModified(status, params));
				}
				else {
					s.updateCall(remoteGlitch.getModified(status, params));
				}
			});
//
//	@AutoFeed
//	public SequentialTrigger<BaseEvent> pantokratorSqOther = SqtTemplates.sq(50_000, AbilityCastStart.class,
//			acs -> acs.abilityIdMatches(0x7B0B),
//			(e1, s) -> {
//				EventCollector<BuffApplied> sup = new EventCollector<>(ba -> isLineDebuff(ba) && ((XivPlayerCharacter) ba.getTarget()).getJob().isSupport());
//				EventCollector<BuffApplied> dps = new EventCollector<>(ba -> isLineDebuff(ba) && ((XivPlayerCharacter) ba.getTarget()).getJob().isDps());
//				s.collectEvents(8, 30_000, BuffApplied.class, true, List.of(sup, dps));
//
//				List<NumberInLine> supBuffs = sup.getEvents().stream().map(NumberInLine::debuffToLine).sorted(Comparator.naturalOrder()).toList();
//				List<NumberInLine> dpsBuffs = dps.getEvents().stream().map(NumberInLine::debuffToLine).sorted(Comparator.naturalOrder()).toList();
//
//				NumberInLine priorSup = null;
//				for (NumberInLine n : supBuffs) {
//					if (priorSup == n) {
//						s.accept(new SpecificAutoMarkRequest(supPlayerFromLine(n), MarkerSign.BIND2));
//						break;
//					}
//					priorSup = n;
//				}
//
//				NumberInLine priorDps = null;
//				for (NumberInLine n : dpsBuffs) {
//					if (priorDps == n) {
//						s.accept(new SpecificAutoMarkRequest(dpsPlayerFromLine(n), MarkerSign.BIND1));
//						break;
//					}
//					priorDps = n;
//				}
//			});

}
