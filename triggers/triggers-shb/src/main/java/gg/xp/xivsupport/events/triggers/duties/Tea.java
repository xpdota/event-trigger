package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class Tea implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(Tea.class);

	private final XivState state;
	private final StatusEffectRepository buffs;

	public Tea(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.TEA);
	}


	@NpcCastCallout(0x4826)
	private final ModifiableCallout<AbilityCastStart> cascade = ModifiableCallout.durationBasedCall("Cascade", "Raidwide");

	@NpcCastCallout(0x4822)
	private final ModifiableCallout<AbilityCastStart> proteanWave = ModifiableCallout.durationBasedCall("Protean Wave", "Protean");

	private final ModifiableCallout<TetherEvent> drainageTether = new ModifiableCallout<>("Drainage Tether", "Tether on You");
	private final RepeatSuppressor tetherSupp = new RepeatSuppressor(Duration.ofSeconds(10));

	@HandleEvents
	public void drainageTether(EventContext context, TetherEvent tether) {
		if (tether.tetherIdMatches(3)
		    && tether.eitherTargetMatches(XivCombatant::isThePlayer)
		    && tetherSupp.check(tether)) {
			context.accept(drainageTether.getModified(tether));
		}
	}

	private final ModifiableCallout<AbilityCastStart> handOfPain = new ModifiableCallout<>("Hand of Pain", "Hand of Pain", "HP Check: {hpcheck}", ModifiableCallout.durationExpiry());

	@AutoFeed
	private final SequentialTrigger<BaseEvent> akhAfahHpCheck = SqtTemplates.sq(20_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x482D) && acs.getSource().getbNpcId() == 12613,
			(e1, s) -> {
				XivState state = getState();
				List<XivCombatant> cbts = state.getCombatantsListCopy();
				// TODO: get proper IDs for TEA
				XivCombatant nidhogg = cbts.stream().filter(cbt -> cbt.getbNpcId() == 12612).findAny().orElseThrow(() -> new RuntimeException("Could not find Nidhogg!"));
				XivCombatant hraes = cbts.stream().filter(cbt -> cbt.getbNpcId() == 12613).findAny().orElseThrow(() -> new RuntimeException("Could not find Hraesvelgr!"));
				Supplier<String> hpCheckSupp = () -> {
					XivCombatant nidNow = state.getLatestCombatantData(nidhogg);
					XivCombatant hraesNow = state.getLatestCombatantData(hraes);

					//noinspection ConstantConditions - just let it error out if null
					double nidPct = nidNow.getHp().getPercent();
					//noinspection ConstantConditions
					double hraesPct = hraesNow.getHp().getPercent();
					double diff = hraesPct - nidPct;
					// Actual percentage is 2.9, but we want a buffer
					if (diff > 0.015) {
						return "Attack Hraesvelgr";
					}
					else if (diff < -0.015) {
						return "Attack Nidhogg";
					}
					else {
						return "Even";
					}
				};
				s.setParam("hpcheck", hpCheckSupp);
				s.updateCall(handOfPain, e1);
			}
	);

	private final ModifiableCallout<BuffApplied> throttle = new ModifiableCallout<BuffApplied>("Throttle", "Cleanse Throttles", "Cleanse Throttles", ignored -> getBuffs().findBuffById(0x2BC) != null).statusIcon(0x2BC);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> throttleSq = SqtTemplates.sq(10_000, BuffApplied.class, ba -> ba.buffIdMatches(0x2BC),
			(e1, s) -> {
				if (!getState().playerJobMatches(Job::caresAboutEsuna)) {
					return;
				}
				s.waitMs(100);
				List<XivCombatant> throttledPlayers = getBuffs()
						.findBuffsById(0x2BC)
						.stream()
						.map(BuffApplied::getTarget)
						.sorted(Comparator.comparing(item -> getState().getPartySlotOf(item)))
						.toList();
				s.setParam("throttles", throttledPlayers);
				s.updateCall(throttle, e1);
			});


	private final ModifiableCallout<?> limitCut1Start = new ModifiableCallout<>("Limit Cut 1 (Start)", "1");
	private final ModifiableCallout<?> limitCut2Start = new ModifiableCallout<>("Limit Cut 2 (Start)", "2");
	private final ModifiableCallout<?> limitCut3Start = new ModifiableCallout<>("Limit Cut 3 (Start)", "3");
	private final ModifiableCallout<?> limitCut4Start = new ModifiableCallout<>("Limit Cut 4 (Start)", "4");
	private final ModifiableCallout<?> limitCut5Start = new ModifiableCallout<>("Limit Cut 5 (Start)", "5");
	private final ModifiableCallout<?> limitCut6Start = new ModifiableCallout<>("Limit Cut 6 (Start)", "6");
	private final ModifiableCallout<?> limitCut7Start = new ModifiableCallout<>("Limit Cut 7 (Start)", "7");
	private final ModifiableCallout<?> limitCut8Start = new ModifiableCallout<>("Limit Cut 8 (Start)", "8");

	private final ModifiableCallout<?> limitCut1You = new ModifiableCallout<>("Limit Cut 1 (Now)", "Cleave On You");
	private final ModifiableCallout<?> limitCut2You = new ModifiableCallout<>("Limit Cut 2 (Now)", "Charge On You");
	private final ModifiableCallout<?> limitCut3You = new ModifiableCallout<>("Limit Cut 3 (Now)", "Cleave On You");
	private final ModifiableCallout<?> limitCut4You = new ModifiableCallout<>("Limit Cut 4 (Now)", "Charge On You");
	private final ModifiableCallout<?> limitCut5You = new ModifiableCallout<>("Limit Cut 5 (Now)", "Cleave On You");
	private final ModifiableCallout<?> limitCut6You = new ModifiableCallout<>("Limit Cut 6 (Now)", "Charge On You");
	private final ModifiableCallout<?> limitCut7You = new ModifiableCallout<>("Limit Cut 7 (Now)", "Cleave On You");
	private final ModifiableCallout<?> limitCut8You = new ModifiableCallout<>("Limit Cut 8 (Now)", "Charge On You");

	private final ModifiableCallout<?> limitCut12NotYou = new ModifiableCallout<>("Limit Cut 1/2 (Not You)", "1 and 2").disabledByDefault();
	private final ModifiableCallout<?> limitCut34NotYou = new ModifiableCallout<>("Limit Cut 3/4 (Not You)", "3 and 4").disabledByDefault();
	private final ModifiableCallout<?> limitCut56NotYou = new ModifiableCallout<>("Limit Cut 5/6 (Not You)", "5 and 6").disabledByDefault();
	private final ModifiableCallout<?> limitCut78NotYou = new ModifiableCallout<>("Limit Cut 7/8 (Not You)", "7 and 8").disabledByDefault();

	private final ModifiableCallout<?> limitCutJKick = new ModifiableCallout<>("J-Kick after Limit Cut", "Raidwide");

	// TODO: separate limit cut and wormhole
	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitCutNumber = SqtTemplates.multiInvocation(60_000, HeadMarkerEvent.class,
			hme -> hme.getMarkerOffset() >= 0 && hme.getMarkerOffset() <= 7 && hme.getTarget().isThePlayer(),
			(e1, s) -> {
				// Limit cut
				int number = e1.getMarkerOffset() + 1;
				s.setParam("number", number);
				switch (number) {
					case 1 -> s.updateCall(limitCut1Start);
					case 2 -> s.updateCall(limitCut2Start);
					case 3 -> s.updateCall(limitCut3Start);
					case 4 -> s.updateCall(limitCut4Start);
					case 5 -> s.updateCall(limitCut5Start);
					case 6 -> s.updateCall(limitCut6Start);
					case 7 -> s.updateCall(limitCut7Start);
					case 8 -> s.updateCall(limitCut8Start);
				}
				;
				// Hawk Blaster
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(4830));
				s.updateCall(switch (number) {
					case 1 -> limitCut1You;
					case 2 -> limitCut2You;
					default -> limitCut12NotYou;
				});
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4B4F) && aue.isFirstTarget());
				s.updateCall(switch (number) {
					case 3 -> limitCut3You;
					case 4 -> limitCut4You;
					default -> limitCut34NotYou;
				});
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4B4F) && aue.isFirstTarget());
				s.updateCall(switch (number) {
					case 5 -> limitCut5You;
					case 6 -> limitCut6You;
					default -> limitCut56NotYou;
				});
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4B4F) && aue.isFirstTarget());
				s.updateCall(switch (number) {
					case 7 -> limitCut7You;
					case 8 -> limitCut8You;
					default -> limitCut78NotYou;
				});
				s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x4B4F) && aue.isFirstTarget());
				s.updateCall(limitCutJKick);
			}, (e1, s) -> {
				// Wormhole

			});

	@NpcCastCallout(0x49C2)
	private final ModifiableCallout<AbilityCastStart> whirlwind = new ModifiableCallout<>("Whirlwind", "Raidwide");

	private enum Nisi {
		Alpha(0x8AE, 0x8B0),
		Beta(0x8AF, 0x8B1),
		Gamma(0x859, 0x85B),
		Delta(0x85A, 0x85C);

		private final long nisiBuffId;
		private final long finalBuffId;

		Nisi(long nisiBuffId, long finalBuffId) {
			this.nisiBuffId = nisiBuffId;
			this.finalBuffId = finalBuffId;
		}

		private static @Nullable Nisi fromBuff(BuffApplied buff) {
			if (buff != null) {
				long id = buff.getBuff().getId();
				for (Nisi value : values()) {
					if (value.nisiBuffId == id) {
						return value;
					}
				}
			}
			return null;
		}

		private static @Nullable Nisi fromFinalBuff(BuffApplied buff) {
			if (buff != null) {
				long id = buff.getBuff().getId();
				for (Nisi value : values()) {
					if (value.finalBuffId == id) {
						return value;
					}
				}
			}
			return null;
		}
	}

	private final ModifiableCallout<?> nisiNotOnYou = new ModifiableCallout<>("No Nisi");
	private final ModifiableCallout<BuffApplied> nisiAlpha = new ModifiableCallout<BuffApplied>("Nisi: Assigned Alpha", "Alpha", "Alpha", ModifiableCallout.durationExpiry()).statusIcon(0x8AE);
	private final ModifiableCallout<BuffApplied> nisiBeta = new ModifiableCallout<BuffApplied>("Nisi: Assigned Beta", "Beta", "Beta", ModifiableCallout.durationExpiry()).statusIcon(0x8AF);
	private final ModifiableCallout<BuffApplied> nisiGamma = new ModifiableCallout<BuffApplied>("Nisi: Assigned Gamma", "Gamma", "Gamma", ModifiableCallout.durationExpiry()).statusIcon(0x859);
	private final ModifiableCallout<BuffApplied> nisiDelta = new ModifiableCallout<BuffApplied>("Nisi: Assigned Delta", "Delta", "Delta", ModifiableCallout.durationExpiry()).statusIcon(0x85A);

	private final ModifiableCallout<BuffApplied> nisiGetAlpha = new ModifiableCallout<BuffApplied>("Nisi: Get Alpha", "Get Alpha from {finalBuddy}", "Get Alpha from {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x8B0);
	private final ModifiableCallout<BuffApplied> nisiGetBeta = new ModifiableCallout<BuffApplied>("Nisi: Get Beta", "Get Beta from {finalBuddy}", "Get Beta from {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x8B1);
	private final ModifiableCallout<BuffApplied> nisiGetGamma = new ModifiableCallout<BuffApplied>("Nisi: Get Gamma", "Get Gamma from {finalBuddy}", "Get Gamma from {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x85B);
	private final ModifiableCallout<BuffApplied> nisiGetDelta = new ModifiableCallout<BuffApplied>("Nisi: Get Delta", "Get Delta from {finalBuddy}", "Get Delta from {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x85C);

	private final ModifiableCallout<BuffApplied> nisiGiveAlpha = new ModifiableCallout<BuffApplied>("Nisi: Give Alpha", "Give Alpha to {finalBuddy}", "Give Alpha to {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x8AE);
	private final ModifiableCallout<BuffApplied> nisiGiveBeta = new ModifiableCallout<BuffApplied>("Nisi: Give Beta", "Give Beta to {finalBuddy}", "Give Beta to {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x8AF);
	private final ModifiableCallout<BuffApplied> nisiGiveGamma = new ModifiableCallout<BuffApplied>("Nisi: Give Gamma", "Give Gamma to {finalBuddy}", "Give Gamma to {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x859);
	private final ModifiableCallout<BuffApplied> nisiGiveDelta = new ModifiableCallout<BuffApplied>("Nisi: Give Delta", "Give Delta to {finalBuddy}", "Give Delta to {finalBuddy}", ModifiableCallout.durationExpiry()).statusIcon(0x85A);

	private @Nullable BuffApplied getMyNisiBuff() {
		return buffs.findBuff(buff -> buff.getTarget().isThePlayer() && Nisi.fromBuff(buff) != null);
	}

	private @Nullable Nisi getMyNisi() {
		return Nisi.fromBuff(getMyNisiBuff());
	}

	private @Nullable BuffApplied getMyFinalNisiBuff() {
		return buffs.findBuff(buff -> buff.getTarget().isThePlayer() && Nisi.fromFinalBuff(buff) != null);
	}

	private @Nullable Nisi getMyNeededNisi() {
		return Nisi.fromFinalBuff(getMyFinalNisiBuff());
	}

	private @Nullable BuffApplied getNeededNisiBuff() {
		// Two cases: Need to get, and need to give
		Nisi needed = getMyNeededNisi();
		if (needed == null) {
			return null;
		}
		return buffs.findBuff(ba -> ba.buffIdMatches(needed.nisiBuffId));
	}

	private @Nullable XivPlayerCharacter whoNeedsMyNisi() {
		Nisi myNisi = getMyNisi();
		if (myNisi == null) {
			return null;
		}
		else {
			BuffApplied buff = buffs.findBuff(ba -> ba.buffIdMatches(myNisi.finalBuffId));
			if (buff != null) {
				return (XivPlayerCharacter) buff.getTarget();
			}
			else {
				return null;
			}
		}
	}


	@AutoFeed
	private final SequentialTrigger<BaseEvent> nisi = SqtTemplates.sq(180_000,
			AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x483E),
			(e1, s) -> {
				s.waitEventsQuickSuccession(4, BuffApplied.class, ba -> ba.buffIdMatches(0x8AE, 0x8AF, 0x859, 0x85A), Duration.ofMillis(100));
				{
					@Nullable BuffApplied myNisiBuff = getMyNisiBuff();
					if (myNisiBuff == null) {
						s.updateCall(nisiNotOnYou);
					}
					else {
						Nisi myNisi;
						switch ((int) myNisiBuff.getBuff().getId()) {
							case 0x8AE -> myNisi = Nisi.Alpha;
							case 0x8AF -> myNisi = Nisi.Beta;
							case 0x859 -> myNisi = Nisi.Gamma;
							case 0x85A -> myNisi = Nisi.Delta;
							default -> {
								log.error("Bad Nisi! {}", myNisiBuff);
								return;
							}
						}
						s.updateCall(switch (myNisi) {
							case Alpha -> nisiAlpha;
							case Beta -> nisiBeta;
							case Gamma -> nisiGamma;
							case Delta -> nisiDelta;
						}, myNisiBuff);
					}
				}
				{
					// Wait for final decree
					s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(0x8B0, 0x8B1, 0x85B, 0x85C) && ba.getTarget().isThePlayer());
					s.waitMs(100);
					{
						Nisi myNisi = getMyNisi();
						BuffApplied myNisiBuff = getMyNisiBuff();
						if (myNisiBuff == null) {
							Nisi needed = getMyNeededNisi();
							BuffApplied buddyBuff = getNeededNisiBuff();
							s.setParam("nisiBuddy", buddyBuff == null ? "Error" : buddyBuff.getTarget());
							s.updateCall(switch (needed) {
								case Alpha -> nisiGetAlpha;
								case Beta -> nisiGetBeta;
								case Gamma -> nisiGetGamma;
								case Delta -> nisiGetDelta;
							}, buddyBuff);
						}
						else {
							XivPlayerCharacter buddy = whoNeedsMyNisi();
							s.setParam("nisiBuddy", buddy);
							s.updateCall(switch (myNisi) {
								case Alpha -> nisiGiveAlpha;
								case Beta -> nisiGiveBeta;
								case Gamma -> nisiGiveGamma;
								case Delta -> nisiGiveDelta;
							}, myNisiBuff);
						}
					}
					s.waitMs(30_000);
					{
						Nisi myNisi = getMyNisi();
						BuffApplied myNisiBuff = getMyNisiBuff();
						if (myNisiBuff == null) {
							Nisi needed = getMyNeededNisi();
							BuffApplied buddyBuff = getNeededNisiBuff();
							s.setParam("nisiBuddy", buddyBuff == null ? "Error" : buddyBuff.getTarget());
							s.updateCall(switch (needed) {
								case Alpha -> nisiGetAlpha;
								case Beta -> nisiGetBeta;
								case Gamma -> nisiGetGamma;
								case Delta -> nisiGetDelta;
							}, buddyBuff);
						}
						else {
							XivPlayerCharacter buddy = whoNeedsMyNisi();
							s.setParam("nisiBuddy", buddy);
							s.updateCall(switch (myNisi) {
								case Alpha -> nisiGiveAlpha;
								case Beta -> nisiGiveBeta;
								case Gamma -> nisiGiveGamma;
								case Delta -> nisiGiveDelta;
							}, myNisiBuff);
						}
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> chakramsMoveOut = ModifiableCallout.durationBasedCall("Chrakrams: Move Out and Bait", "Move Out");
	private final ModifiableCallout<AbilityUsedEvent> chakramsMoveIn = new ModifiableCallout<>("Chrakrams: Move In and Dodge", "Move In");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> chakrams = SqtTemplates.beginningAndEndingOfCast(acs -> acs.abilityIdMatches(0x4855), chakramsMoveOut, chakramsMoveIn);

	private final ModifiableCallout<BuffApplied> neitherLightningNorWater = ModifiableCallout.durationBasedCall("Water on {water.target}, Lightning on {lightning.target}");
	private final ModifiableCallout<BuffApplied> water = ModifiableCallout.<BuffApplied>durationBasedCall("Water on you, lightning on {lightning.target}").statusIcon(0x85E);
	private final ModifiableCallout<BuffApplied> lightning = ModifiableCallout.<BuffApplied>durationBasedCall("Lightning on you, water on {water.target}").statusIcon(0x85F);

	private final ModifiableCallout<?> enumYou = new ModifiableCallout<>("Enum on you and {enum2}");
	private final ModifiableCallout<?> enumNotYou = new ModifiableCallout<>("Enums on {enum1} and {enum2}");


	public XivState getState() {
		return state;
	}

	public StatusEffectRepository getBuffs() {
		return buffs;
	}
}
