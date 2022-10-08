package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.events.P6STetherEvent;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.events.P6STileEvent;
import gg.xp.xivsupport.events.triggers.duties.Pandamonium.events.TileType;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CalloutRepo(name = "P6S", duty = KnownDuty.P6S)
public class P6S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P6S.class);

	/*
		TODO:
		Call out current LC number
		Point in/Point out (status 0xCF2 is face out, 0xCF3 is face in)
		3rd set of healer stacks + other mechanics
		Whatever mechs come after

		Probably not doable:
		Floor tile patterns (likely need MapEvent)


	 */

	// TODO: Check these, especially number 4
	private final ModifiableCallout<AbilityCastStart> aethericPolyminoid1 = ModifiableCallout.durationBasedCall("Aetheric Polyominoid 1", "Light Parties in Safe Tiles");
	private final ModifiableCallout<AbilityCastStart> aethericPolyminoid2 = ModifiableCallout.durationBasedCall("Aetheric Polyominoid 2", "Spread in Safe Spots, Bait");
	private final ModifiableCallout<AbilityCastStart> aethericPolyminoid3 = ModifiableCallout.durationBasedCall("Aetheric Polyominoid 3", "Bait then Move to Safe Spot");
	private final ModifiableCallout<AbilityCastStart> aethericPolyminoid4 = ModifiableCallout.durationBasedCall("Aetheric Polyominoid 4", "Safe Spot, Dodge Cleave");
	private final ModifiableCallout<AbilityCastStart> aethericPolyminoid5 = ModifiableCallout.durationBasedCall("Aetheric Polyominoid 5", "Light Parties in Safe Tiles");

	private final ModifiableCallout<AbilityCastStart> exchange1 = ModifiableCallout.durationBasedCall("Exchange 1", "Stack/Spread");
	private final ModifiableCallout<AbilityCastStart> exchange2 = ModifiableCallout.durationBasedCall("Exchange 2", "Bait then Move to Safe");
	private final ModifiableCallout<AbilityCastStart> exchange3 = ModifiableCallout.durationBasedCall("Exchange 3", "Light Parties/Spreads");
	private final ModifiableCallout<AbilityCastStart> exchange4 = ModifiableCallout.durationBasedCall("Exchange 4", "Light Parties in Safe Spot");
	private final ModifiableCallout<AbilityCastStart> exchange5 = ModifiableCallout.durationBasedCall("Exchange 5", "Stack/Spread");
	private final ModifiableCallout<AbilityCastStart> exchange6 = ModifiableCallout.durationBasedCall("Exchange 6", "Find Safe Spot, Dodge Cleave");
	private final ModifiableCallout<AbilityCastStart> exchange7 = ModifiableCallout.durationBasedCall("Exchange 7", "Check Debuff, Stack/Spread");

	private final ModifiableCallout<AbilityCastStart> chelicSynergy = ModifiableCallout.durationBasedCall("Chelic Synergy", "Buster with Bleed");
	private final ModifiableCallout<AbilityCastStart> unholyDarknessHealer = ModifiableCallout.durationBasedCall("Unholy Darkness", "Healer Stacks");
	private final ModifiableCallout<AbilityCastStart> exoCleaver = ModifiableCallout.durationBasedCall("Exocleaver", "Cleaves");
	private final ModifiableCallout<AbilityCastStart> exoCleaverAfter = new ModifiableCallout<>("Exocleaver After", "Move");
	//	private final ModifiableCallout<AbilityCastStart> polyominoidSigma = ModifiableCallout.durationBasedCall("Polyominoid Sigma", "Tiles Swapping");
	private final ModifiableCallout<HeadMarkerEvent> limitCutNumber = new ModifiableCallout<>("Limit Cut Number", "{number}", 20_000);
	private final ModifiableCallout<AbilityCastStart> chorosIxouSides = ModifiableCallout.durationBasedCall("Choros Ixou Cleaving Side First", "Front/Back then Sides");
	private final ModifiableCallout<?> chorosIxouSidesAfter = new ModifiableCallout<>("Choros Ixou, Move to Side", "Sides");
	private final ModifiableCallout<AbilityCastStart> chorosIxouFrontBack = ModifiableCallout.durationBasedCall("Choros Ixou Cleaving Front/Back", "Sides then Front/Back");
	private final ModifiableCallout<?> chorosIxouFrontBackAfter = new ModifiableCallout<>("Choros Ixou, Move to Front/Back", "Front/Back");
	private final ModifiableCallout<AbilityCastStart> hemitheosDarkIV = ModifiableCallout.durationBasedCall("Hemitheos's Dark IV", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> synergy = ModifiableCallout.durationBasedCall("Synergy", "Tankbuster"); //????+1 on MT, ????+2 on OT
	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread");
	private final ModifiableCallout<AbilityCastStart> darkSphere = ModifiableCallout.durationBasedCall("Dark Sphere", "Spread to Safe Spots");
	private final ModifiableCallout<AbilityCastStart> darkBurst = ModifiableCallout.durationBasedCall("Dark Burst (Flare)", "Out");
	private final ModifiableCallout<AbilityCastStart> darkPerimeter = ModifiableCallout.durationBasedCall("Dark Sphere (Donut)", "Donut");
	private final ModifiableCallout<AbilityCastStart> unholyDarkness2 = ModifiableCallout.durationBasedCall("Unholy Darkness (Single)", "Stack");
	private final ModifiableCallout<AbilityCastStart> unholyDarknessSomeoneElse = ModifiableCallout.durationBasedCall("Nothing", "Nothing (Stack)");
	private final ModifiableCallout<AbilityCastStart> darkDomeBait = ModifiableCallout.durationBasedCall("Dark Dome Bait", "Bait");
	// Added number to reset customizations
	private final ModifiableCallout<AbilityUsedEvent> darkDomeMove2 = new ModifiableCallout<>("Dark Dome Move", "Move!");

	private final ModifiableCallout<BuffApplied> dpWing8 = new ModifiableCallout<BuffApplied>("8 Second + Hit By Wing (Left)", "8, Left", 23_000).statusIcon(3319);
	private final ModifiableCallout<BuffApplied> dpWing12 = new ModifiableCallout<BuffApplied>("12 Second + Hit By Wing (Left)", "12, Left", 23_000).statusIcon(3319);
	private final ModifiableCallout<BuffApplied> dpWing16 = new ModifiableCallout<BuffApplied>("16 Second + Hit By Wing (Left)", "16, Left", 23_000).statusIcon(3319);
	private final ModifiableCallout<BuffApplied> dpWing20 = new ModifiableCallout<BuffApplied>("20 Second + Hit By Wing (Left)", "20, Left", 23_000).statusIcon(3319);
	private final ModifiableCallout<BuffApplied> dpSnake8 = new ModifiableCallout<BuffApplied>("8 Second + Hit By Snake (Right)", "8, Right", 23_000).statusIcon(3320);
	private final ModifiableCallout<BuffApplied> dpSnake12 = new ModifiableCallout<BuffApplied>("12 Second + Hit By Snake (Right)", "12, Right", 23_000).statusIcon(3320);
	private final ModifiableCallout<BuffApplied> dpSnake16 = new ModifiableCallout<BuffApplied>("16 Second + Hit By Snake (Right)", "16, Right", 23_000).statusIcon(3320);
	private final ModifiableCallout<BuffApplied> dpSnake20 = new ModifiableCallout<BuffApplied>("20 Second + Hit By Snake (Right)", "20, Right", 23_000).statusIcon(3320);
	private final ModifiableCallout<AbilityCastStart> pteraIxou = ModifiableCallout.durationBasedCall("Ptera Ixou (Unknown/Error)", "Switch Sides");
	private final ModifiableCallout<AbilityCastStart> pteraIxouLeft = ModifiableCallout.durationBasedCall("Ptera Ixou (Go Left/Wing)", "Left");
	private final ModifiableCallout<AbilityCastStart> pteraIxouRight = ModifiableCallout.durationBasedCall("Ptera Ixou (Go Right/Snake)", "Right");

	private final ModifiableCallout<BuffApplied> faceIn = ModifiableCallout.<BuffApplied>durationBasedCall("Cleaving Behind You", "Face In Soon").autoIcon();
	private final ModifiableCallout<BuffApplied> faceOut = ModifiableCallout.<BuffApplied>durationBasedCall("Cleaving in Front of You", "Face Out Soon").autoIcon();
	private final ModifiableCallout<BuffApplied> faceInNow = ModifiableCallout.<BuffApplied>durationBasedCall("Cleaving Behind You", "Face In Now").autoIcon();
	private final ModifiableCallout<BuffApplied> faceOutNow = ModifiableCallout.<BuffApplied>durationBasedCall("Cleaving in Front of You", "Face Out Now").autoIcon();
//	private final ModifiableCallout<AbilityCastStart> stropheIxouCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Clockwise");
//	private final ModifiableCallout<AbilityCastStart> stropheIxouCCW = ModifiableCallout.durationBasedCall("Strophe Ixou", "Sides, Counterclockwise");
//	private final ModifiableCallout<AbilityCastStart> darkAshes = ModifiableCallout.durationBasedCall("Dark Ashes", "Spread"); //????-1 real boss

//	private final ModifiableCallout<HasDuration> glossomorph = ModifiableCallout.durationBasedCall("Glossomorph debuff", "Point Away Soon").autoIcon();

	public P6S(XivState state, StatusEffectRepository buffs, ActiveCastRepository acr) {
		this.state = state;
		this.buffs = buffs;
		this.acr = acr;
	}

	private final XivState state;

	private XivState getState() {
		return this.state;
	}

	private final StatusEffectRepository buffs;

	private StatusEffectRepository getBuffs() {
		return this.buffs;
	}

	private final ActiveCastRepository acr;

	private ActiveCastRepository getAcr() {
		return acr;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P6S);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		// Unknown:
		/*
			_rsv_30858 (788A) - chelic synergy (buster)
			_rsv_30828 (786C) - ?

		*/
		switch (id) {
			case 30858 -> call = chelicSynergy;
			case 0x7891 -> call = unholyDarknessHealer;
			case 0x7887 -> call = synergy;
			case 0x788D -> call = darkAshes;
			case 0x788F -> call = darkSphere;
			case 0x7860 -> call = hemitheosDarkIV;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	@AutoFeed
	// Transmission
	private final SequentialTrigger<BaseEvent> faceInOut = SqtTemplates.multiInvocation(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7861), (e1, s) -> {
				log.info("Transmission 1: Start");
				BuffApplied faceInOutBuff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(0xcf3, 0xd48));
				log.info("Transmission 1: Got {}", faceInOutBuff);
				long waitMs = faceInOutBuff.getEstimatedRemainingDuration().minusSeconds(2).toMillis();
				if (faceInOutBuff.buffIdMatches(0xcf3)) {
					s.updateCall(faceIn.getModified(faceInOutBuff));
					s.waitMs(waitMs);
					s.updateCall(faceInNow.getModified(faceInOutBuff));
				}
				else if (faceInOutBuff.buffIdMatches(0xd48)) {
					s.updateCall(faceOut.getModified(faceInOutBuff));
					s.waitMs(waitMs);
					s.updateCall(faceOutNow.getModified(faceInOutBuff));
				}
			}, (e1, s) -> {
				log.info("Transmission 2");
				// Handled by other triggers?
			}, (e1, s) -> {
				log.info("Transmission 3");
				// Kind of handled by other triggers?
			});

	private Long firstHeadmark;

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> chorosIxouFb = SqtTemplates.beginningAndEndingOfCast(acs -> acs.abilityIdMatches(0x7881),
			chorosIxouFrontBack, chorosIxouFrontBackAfter);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> chorosIxouS = SqtTemplates.beginningAndEndingOfCast(acs -> acs.abilityIdMatches(0x7883),
			chorosIxouSides, chorosIxouSidesAfter);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> cleaver = SqtTemplates.beginningAndEndingOfCast(acs -> acs.abilityIdMatches(0x7869),
			exoCleaver, exoCleaverAfter);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> aethericPolyminoids = SqtTemplates.multiInvocation(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7866),
			(e1, s) -> {
				// Healer stacks in safe spot
				s.updateCall(aethericPolyminoid1.getModified(e1));
			},
			(e1, s) -> {
				// Bait Puddle then move to safe spot
				s.updateCall(aethericPolyminoid2.getModified(e1));
			},
			(e1, s) -> {
				// Bait Puddle then move to safe spot
				s.updateCall(aethericPolyminoid3.getModified(e1));
			},
			(e1, s) -> {
				// Bait Puddle then move to safe spot
				s.updateCall(aethericPolyminoid4.getModified(e1));
			},
			(e1, s) -> {
				s.updateCall(aethericPolyminoid5.getModified(e1));
			}
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> exchange = SqtTemplates.multiInvocation(30_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x784D),
			(e1, s) -> {
				// Swapping headmarkers/casts
				s.updateCall(exchange1.getModified(e1));
				AbilityCastStart event = s.waitEvent(AbilityCastStart.class, acs -> acs.getTarget().isThePlayer() && acs.getSource().getbNpcId() == 9020);
				ModifiableCallout<AbilityCastStart> call;
				switch ((int) event.getAbility().getId()) {
					// Each of these seems to have 3 IDs. I'm guessing it's no-swap plus the two possibilities for swap
					case 0x7872, 0x7871, 0x7870 -> call = darkBurst;
					case 0x7875, 0x7874, 0x7873 -> call = darkPerimeter;
					case 0x786D, 0x786E, 0x786F -> call = unholyDarkness2;
					default -> {
						// shouldn't happen
						return;
					}
				}
				s.updateCall(call.getModified(event));
			},
			(e1, s) -> {
				// Bait Puddle then move to safe spot
				s.updateCall(exchange2.getModified(e1));
			},
			(e1, s) -> {
				s.updateCall(exchange3.getModified(e1));
				// Light parties and markers
				// Like before, but we have to account for getting no marker whatsoever
				List<AbilityCastStart> casts = s.waitEvents(4, AbilityCastStart.class, acs -> acs.getTarget().isPc() && acs.getSource().getbNpcId() == 9020);
				log.info("Exchange 3 casts: {}", casts);
				casts.stream().filter(acs -> acs.getTarget().isThePlayer())
						.findAny()
						.ifPresentOrElse(event -> {
							ModifiableCallout<AbilityCastStart> call;

							switch ((int) event.getAbility().getId()) {
								// Each of these seems to have 3 IDs. I'm guessing it's no-swap plus the two possibilities for swap
								case 0x7872, 0x7871, 0x7870 -> call = darkBurst;
								case 0x7875, 0x7874, 0x7873 -> call = darkPerimeter;
								case 0x7A0F -> call = unholyDarkness2;
								default -> {
									// shouldn't happen
									log.warn("Invalid ID! {}", event);
									return;
								}
							}
							s.updateCall(call.getModified(event));
						}, () -> {
							s.updateCall(unholyDarknessSomeoneElse.getModified(getAcr()
									.getAll()
									.stream()
									.filter(tracker -> tracker.getResult() == CastResult.IN_PROGRESS)
									.map(CastTracker::getCast)
									// TODO: test this
									.filter(cast -> cast.abilityIdMatches(0x7A0F))
									.findAny()
									.orElse(null)));
						});
			},
			(e1, s) -> {
				// Healer Stacks in Safe Spot
				s.updateCall(exchange4.getModified(e1));
			},
			(e1, s) -> {
				// Swapping headmarkers/casts
				s.updateCall(exchange5.getModified(e1));
				AbilityCastStart event = s.waitEvent(AbilityCastStart.class, acs -> acs.getTarget().isThePlayer() && acs.getSource().getbNpcId() == 9020);
				ModifiableCallout<AbilityCastStart> call;
				switch ((int) event.getAbility().getId()) {
					// Each of these seems to have 3 IDs. I'm guessing it's no-swap plus the two possibilities for swap
					case 0x7872, 0x7871, 0x7870 -> call = darkBurst;
					case 0x7875, 0x7874, 0x7873 -> call = darkPerimeter;
					case 0x786D, 0x786E, 0x786F -> call = unholyDarkness2;
					default -> {
						// shouldn't happen
						return;
					}
				}
				s.updateCall(call.getModified(event));
			},
			(e1, s) -> {
				// the one where you dodge cleave
				s.updateCall(exchange6.getModified(e1));
			},
			(e1, s) -> {
				s.updateCall(exchange7.getModified(e1));
			}
	);


	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		firstHeadmark = null;
	}

	@HandleEvents
	public void headmark(EventContext context, HeadMarkerEvent event) {
		int offset = getHeadmarkOffset(event);
		if (offset >= -239 && offset <= -232 && event.getTarget().isThePlayer()) {
			context.accept(limitCutNumber.getModified(event, Map.of("number", offset + 240)));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> darkDomeSq = SqtTemplates.beginningAndEndingOfCast(
			acs -> acs.abilityIdMatches(30859),
			darkDomeBait, darkDomeMove2
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> dualPredation = new SequentialTrigger<>(50_000, BaseEvent.class,
			e1 -> e1 instanceof BuffApplied ba && ba.buffIdMatches(3321),
			(e1, s) -> {
				s.waitMs(200);
				{
					List<BuffApplied> playerBuffs = getBuffs().statusesOnTarget(getState().getPlayer());
					BuffApplied headBuff = playerBuffs.stream().filter(ba -> ba.buffIdMatches(3321)).findFirst().orElseThrow(() -> new RuntimeException("Didn't find head buff!"));
					BuffApplied sideBuff = playerBuffs.stream().filter(ba -> ba.buffIdMatches(3319, 3320)).findFirst().orElseThrow(() -> new RuntimeException("Didn't find side buff!"));
					long seconds = headBuff.getInitialDuration().toSeconds();
					boolean getHitByWing = sideBuff.buffIdMatches(0xCF7);
					ModifiableCallout<BuffApplied> call;
					if (seconds < 10) {
						call = getHitByWing ? dpWing8 : dpSnake8;
					}
					else if (seconds < 14) {
						call = getHitByWing ? dpWing12 : dpSnake12;
					}
					else if (seconds < 18) {
						call = getHitByWing ? dpWing16 : dpSnake16;
					}
					else {
						call = getHitByWing ? dpWing20 : dpSnake20;
					}
					s.updateCall(call.getModified(headBuff));
				}
				AbilityCastStart ptera = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(30844));
				{
					List<BuffApplied> playerBuffs = getBuffs().statusesOnTarget(getState().getPlayer());
					BuffApplied sideBuff = playerBuffs.stream().filter(ba -> ba.buffIdMatches(3319, 3320)).findFirst().orElse(null);
					if (sideBuff == null) {
						s.updateCall(pteraIxou.getModified(ptera));
					}
					else {
						boolean getHitByWing = sideBuff.buffIdMatches(0xCF7);
						if (getHitByWing) {
							s.updateCall(pteraIxouLeft.getModified(ptera));
						}
						else {
							s.updateCall(pteraIxouRight.getModified(ptera));
						}
					}
				}
			}
	);

	@AutoFeed
	private final SequentialTrigger<BaseEvent> aethericPolyminoidSq = SqtTemplates.multiInvocation(30_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7866, 0x7868),
			this::poly1,
			this::poly2,
			this::poly3,
			this::poly4,
			this::poly5,
			this::poly6,
			this::poly7,
			this::poly8);

	private final ModifiableCallout<?> poly1safeWIn = new ModifiableCallout<>("Poly 1 W in, E out", "West in, East out");
	private final ModifiableCallout<?> poly1safeWOut = new ModifiableCallout<>("Poly 1 W out, E in", "West out, East in");
	private final ModifiableCallout<?> poly1error = new ModifiableCallout<>("Poly 1 Error", "Error");
	private final ModifiableCallout<?> poly2safe = new ModifiableCallout<>("Poly 2 bait then safe spot", "Bait middle, then {safe}");
	private final ModifiableCallout<?> poly3safe = new ModifiableCallout<>("Poly 3 safe spots", "Light parties, {safe1} {safe2}");
	private final ModifiableCallout<?> poly5safe = new ModifiableCallout<>("Poly 5 start spot", "Start inner {start}");
	private final ModifiableCallout<?> poly6safeUP = new ModifiableCallout<>("Poly 6 reference tile", "Corners of inner untethered plus");
	private final ModifiableCallout<?> poly6safeTC = new ModifiableCallout<>("Poly 6 reference tile", "Corners of inner tethered cross");
	private final ModifiableCallout<?> poly6error = new ModifiableCallout<>("Poly 6 error", "Error");
	private final ModifiableCallout<?> poly7safe = new ModifiableCallout<>("Poly 7 bait then safe spot", "Bait middle, then {safe}");
	private final ModifiableCallout<?> poly7error = new ModifiableCallout<>("Poly 7 call", "Error");
	private final ModifiableCallout<?> poly8safe = new ModifiableCallout<>("Poly 8 safe side", "{in}");

	@HandleEvents
	public void p6stile(EventContext context, MapEffectEvent mee) {
		P6STileEvent event;
		TileType type = TileType.forFLag(mee.getFlags());
		if (type == null)
			return;

		switch ((int) mee.getIndex()) {
			case 0x1 -> event = new P6STileEvent(0, 0, type, 0x1);
			case 0x2 -> event = new P6STileEvent(1, 0, type, 0x2);
			case 0x3 -> event = new P6STileEvent(2, 0, type, 0x3);
			case 0x4 -> event = new P6STileEvent(3, 0, type, 0x4);
			case 0xF -> event = new P6STileEvent(0, 1, type, 0xF);
			case 0x5 -> event = new P6STileEvent(1, 1, type, 0x5);
			case 0x6 -> event = new P6STileEvent(2, 1, type, 0x6);
			case 0x10 -> event = new P6STileEvent(3, 1, type, 0x10);
			case 0xD -> event = new P6STileEvent(0, 2, type, 0xD);
			case 0x7 -> event = new P6STileEvent(1, 2, type, 0x7);
			case 0x8 -> event = new P6STileEvent(2, 2, type, 0x8);
			case 0xE -> event = new P6STileEvent(3, 2, type, 0xE);
			case 0x9 -> event = new P6STileEvent(0, 3, type, 0x9);
			case 0xA -> event = new P6STileEvent(1, 3, type, 0xA);
			case 0xB -> event = new P6STileEvent(2, 3, type, 0xB);
			case 0xC -> event = new P6STileEvent(3, 3, type, 0xC);
			default -> {
				return;
			}
		}
		context.accept(event);
	}

	@HandleEvents
	public void p6stether(EventContext context, TetherEvent te) {
		P6STetherEvent event;
		if (te.getId() != 0xCF)
			return;

		event = new P6STetherEvent(te.getSource(), te.getTarget());
	}

	/*
	00020001 = Plus
	00400020 = Cross
	00020001 - + appears
	00400020 - x appears
	00100001 - tether swap, x becomes +
	00800020 - tether swap, + becomes x
	00080004 - resolve/remove effect

	grid layout:
	01|02|03|04
	--+--+--+--
	0F|05|06|10
	--+--+--+--
	0D|07|08|0E
	--+--+--+--
	09|0A|0B|0C
	 */
	//Only need to know one of the tiles, so just look at the first found
	private void poly1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 1: Begin, waiting for map effects");
		P6STileEvent tileEvent = s.waitEvent(P6STileEvent.class);
		log.info("Poly 1: Found map effect at: {}, {}", tileEvent.getX(), tileEvent.getY());
		//01 or 03, west in east out. 02 or 04, east in west out
		s.updateCall((switch (tileEvent.getX()) {
			case 0, 2 -> poly1safeWIn;
			case 1, 3 -> poly1safeWOut;
			default -> poly1error;
		}).getModified());
		log.info("Poly 1: End");
	}

	//Find tile not in corner, always CW from it?
	private void poly2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 2: Begin, waiting for map effects.");
		List<Integer> tilesToCheck = Arrays.asList(0x03, 0x0E, 0x0A, 0x0F);
		P6STileEvent tileEvent = s.waitEvent(P6STileEvent.class, te -> tilesToCheck.contains(te.getIndex()));
		log.info("Poly 2: found map effect at {}, {}", tileEvent.getX(), tileEvent.getY());
		//based on assumption that it is only ever rotated from a possible pattern
		s.updateCall(poly2safe.getModified(Map.of("safe", switch (tileEvent.getIndex()) {
			case 0x03 -> ArenaSector.SOUTHEAST;
			case 0x0E -> ArenaSector.SOUTHWEST;
			case 0x0A -> ArenaSector.NORTHWEST;
			case 0x0F -> ArenaSector.NORTHEAST;
			default -> ArenaSector.UNKNOWN;
		})));
		log.info("Poly 2: End");
	}

	//Only care about any inner plus tile, so just look at the first found
	private void poly3(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 3: Begin, finding map effects.");
		List<Integer> tilesToCheck = Arrays.asList(0x02, 0x03, 0x0E, 0x0B, 0x0A, 0x0D, 0x0F, 0x10);
		P6STileEvent tileEvent = s.waitEvent(P6STileEvent.class, te -> tilesToCheck.contains(te.getIndex()) && te.getTileType() == TileType.PLUS);
		log.info("Poly 3: Found map effect at {}, {}", tileEvent.getX(), tileEvent.getY());
		//always adjacent to plus that isnt in a corner
		s.updateCall(poly3safe.getModified(switch (tileEvent.getIndex()) {
			//SW, NE
			case 0x03, 0x0A, 0x0D, 0x10 -> Map.of("safe1", ArenaSector.SOUTHWEST, "safe2", ArenaSector.NORTHEAST);
			//NW, SE
			case 0x02, 0x0B, 0x0E, 0x0F -> Map.of("safe1", ArenaSector.NORTHWEST, "safe2", ArenaSector.SOUTHEAST);
			default -> Map.of("safe1", ArenaSector.UNKNOWN, "safe2", ArenaSector.UNKNOWN);
		}));
		log.info("Poly 3: End");
	}

	//Unsure how to call this one, probably not possible for all 8 positions at once
	private void poly4(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

	}

	//Find tethered plus, start diagonal inner from it
	private void poly5(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 5: Begin, finding correct map effects");
		List<P6STileEvent> tileEvents = s.waitEvents(3, P6STileEvent.class, te -> true);
		TetherEvent tetherEvent = s.waitEvent(TetherEvent.class, te -> te.getId() == 0xCF);
		s.refreshCombatants(100);
		//use this if i want to call where to move after chorus ixou starts
		P6STileEvent crossMapEffect = tileEvents.stream().filter(e -> e.getTileType() == TileType.CROSS).findFirst().get();
		P6STileEvent tetheredPlusMapEffect = findFirstTetheredTile(tileEvents.stream().filter(e -> e.getTileType() == TileType.PLUS).collect(Collectors.toList()), tetherEvent);
		log.info("Poly 5: Finished finding tethered map effect: {}, {}", tetheredPlusMapEffect.getX(), tetheredPlusMapEffect.getY());
		s.updateCall(switch (tetheredPlusMapEffect.getIndex()) {
			case 0x04 -> poly5safe.getModified(Map.of("start", ArenaSector.NORTHEAST));
			case 0x0C -> poly5safe.getModified(Map.of("start", ArenaSector.SOUTHEAST));
			case 0x09 -> poly5safe.getModified(Map.of("start", ArenaSector.SOUTHWEST));
			case 0x01 -> poly5safe.getModified(Map.of("start", ArenaSector.NORTHWEST));
			default -> poly5safe.getModified(Map.of("start", ArenaSector.UNKNOWN));
		});
		log.info("Poly 5: End.");
	}

	//Aka cachexia 2. find middle type, call cross or plus
	private void poly6(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 6: Begin, finding correct map event");
		List<P6STileEvent> tileEvents = s.waitEvents(4, P6STileEvent.class, te -> true);
		List<Integer> tilesToCheck = Arrays.asList(0x05, 0x06, 0x07, 0x08);
		List<P6STileEvent> middleMapEffects = tileEvents.stream().filter(e -> tilesToCheck.contains(e.getIndex())).toList();
		log.info("Poly 6: Found middle effect type {}", middleMapEffects.get(0).getTileType());

		if (middleMapEffects.get(0).getTileType() == TileType.PLUS) {
//			s.updateCall(poly6safeUP.getModified());
//			poly6safe.getModified(Map.of("ref", "untethered plus"));
		}
		else if (middleMapEffects.get(0).getTileType() == TileType.CROSS) {
//			s.updateCall(poly6safeTC.getModified());
//			poly6safe.getModified(Map.of("ref", "tethered cross"));
		}
		else {
			poly6error.getModified();
		}
		log.info("Poly 6: End.");
	}

	//on diagonal of pluses and adjascent to cross or not adjascent to plus
	@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
	private void poly7(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 7: Begin, finding map effects");
		List<P6STileEvent> mapEffects = s.waitEvents(4, P6STileEvent.class, me -> true);
		//either gives same info
		List<Integer> tilesToCheck = Arrays.asList(0x01, 0x04, 0x09, 0x0C);
		P6STileEvent plusMapEffect = mapEffects.stream().filter(e -> tilesToCheck.contains(e.getIndex()) && e.getTileType() == TileType.PLUS).findFirst().get();
		log.info("Poly 7: Found map effects");
		//first get two possible safe spots
		List<ArenaSector> safeSpots;
		safeSpots = switch (plusMapEffect.getIndex()) {
			case 0x01, 0x0C -> Arrays.asList(ArenaSector.NORTHWEST, ArenaSector.SOUTHEAST);
			case 0x04, 0x09 -> Arrays.asList(ArenaSector.SOUTHWEST, ArenaSector.NORTHEAST);
			default -> Arrays.asList(ArenaSector.UNKNOWN);
		};
		log.info("Poly 7: possible safespots are: {}", safeSpots);
		List<Integer> innerTilesToCheck = Arrays.asList(0x02, 0x03, 0x0A, 0x0B, 0x0D, 0x0E, 0x0F, 0x10);
		P6STileEvent oddOneOut = mapEffects.stream().filter(e -> innerTilesToCheck.contains(e.getIndex())).findFirst().get();
		if (oddOneOut.getTileType() == TileType.PLUS) {
			//plus
			switch (oddOneOut.getIndex()) {
				case 0x02, 0x0A -> {
					safeSpots.remove(ArenaSector.NORTHWEST);
					safeSpots.remove(ArenaSector.SOUTHWEST);
				}
				case 0x03, 0x0B -> {
					safeSpots.remove(ArenaSector.NORTHEAST);
					safeSpots.remove(ArenaSector.SOUTHEAST);
				}
				case 0x0F, 0x10 -> {
					safeSpots.remove(ArenaSector.NORTHWEST);
					safeSpots.remove(ArenaSector.NORTHEAST);
				}
				case 0x0D, 0x0E -> {
					safeSpots.remove(ArenaSector.SOUTHWEST);
					safeSpots.remove(ArenaSector.SOUTHEAST);
				}
				default -> log.info("Poly 7: Weird oddOneOut index: {}", oddOneOut.getIndex());
			}
		}
		else {
			//cross
			switch (oddOneOut.getIndex()) {
				case 0x02, 0x0E -> safeSpots.remove(ArenaSector.NORTHEAST);
				case 0x03, 0x0D -> safeSpots.remove(ArenaSector.NORTHWEST);
				case 0x10, 0x0A -> safeSpots.remove(ArenaSector.SOUTHEAST);
				case 0x0B, 0x0F -> safeSpots.remove(ArenaSector.SOUTHWEST);
				default -> log.info("Poly 7: Weird oddOneOut index: {}", oddOneOut.getIndex());
			}
		}
		if (safeSpots.size() != 1) {
			poly7error.getModified();
		}
		else {
			poly7safe.getModified(Map.of("safe", safeSpots));
		}
		log.info("Poly 7: End");
	}

	//same pattern as first, just dodge chorus ixou
	private void poly8(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("Poly 8: Begin, waiting for map effects");
		List<MapEffectEvent> mapEffects = s.waitEvents(2, MapEffectEvent.class, me -> me.getFlags() == TileType.PLUS.flag);
		log.info("Poly 8: MapEffect grid positions(index): {}, {}", mapEffects.get(0).getIndex(), mapEffects.get(1).getIndex());
		//01 or 03, west in east out. 02 or 04, east in west out
		List<Integer> tilesToCheck = Arrays.asList(0x01, 0x02, 0x03, 0x04, 0x09, 0x0A, 0x0B, 0x0C);
		mapEffects.stream().filter(e -> tilesToCheck.contains((int) e.getIndex())).findFirst().ifPresent(e -> s.updateCall(switch ((int) e.getIndex()) {
			case 0x01, 0x03, 0x09, 0x0B -> poly8safe.getModified(Map.of("in", ArenaSector.WEST));
			case 0x02, 0x04, 0x0A, 0x0C -> poly8safe.getModified(Map.of("in", ArenaSector.EAST));
			default -> poly8safe.getModified(Map.of("in", ArenaSector.UNKNOWN));
		}));
		log.info("Poly 8: End");
	}

	private static P6STileEvent findFirstTetheredTile(List<P6STileEvent> tel, TetherEvent t) {
		return tel.stream().filter(te -> isApproximatelyNearbyTether(te.tilePos(), t))
				.findFirst()
				.orElseThrow(null);
	}

	private static boolean isApproximatelyNearbyTether(Position pos, TetherEvent t) {
		return t.eitherTargetMatches(target -> {
			Position tpos = target.getPos();
			return tpos != null && tpos.distanceFrom2D(pos) < 0.05;
		});
	}
}
