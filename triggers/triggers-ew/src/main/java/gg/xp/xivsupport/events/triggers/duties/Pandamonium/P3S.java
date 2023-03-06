package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CalloutRepo(name = "P3S", duty = KnownDuty.P3S)
public class P3S implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P3S.class);

	private final ModifiableCallout<AbilityCastStart> scorchedExaltation = ModifiableCallout.durationBasedCall("Scorched Exaltation", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> heatOfCondemnation = ModifiableCallout.durationBasedCall("Heat of Condemnation", "Tank Tethers");
	private final ModifiableCallout<AbilityCastStart> deadRebirth = ModifiableCallout.durationBasedCall("Dead Rebirth", "Big Raidwide");
	private final ModifiableCallout<AbilityCastStart> firestorms = ModifiableCallout.durationBasedCall("Firestorms of Asphodelus", "Big Raidwide");

	private final ModifiableCallout<AbilityCastStart> darkenedFire = ModifiableCallout.durationBasedCall("Darkened Fire", "Partners");
	private final ModifiableCallout<HeadMarkerEvent> number1 = new ModifiableCallout<>("#1", "1");
	private final ModifiableCallout<HeadMarkerEvent> number2 = new ModifiableCallout<>("#2", "2");
	private final ModifiableCallout<HeadMarkerEvent> number3 = new ModifiableCallout<>("#3", "3");
	private final ModifiableCallout<HeadMarkerEvent> number4 = new ModifiableCallout<>("#4", "4");
	private final ModifiableCallout<HeadMarkerEvent> number5 = new ModifiableCallout<>("#5", "5");
	private final ModifiableCallout<HeadMarkerEvent> number6 = new ModifiableCallout<>("#6", "6");
	private final ModifiableCallout<HeadMarkerEvent> number7 = new ModifiableCallout<>("#7", "7");
	private final ModifiableCallout<HeadMarkerEvent> number8 = new ModifiableCallout<>("#8", "8");

	private final ModifiableCallout<HeadMarkerEvent> ff1West = new ModifiableCallout<>("Fledling Flight 1 West", "Go West");
	private final ModifiableCallout<HeadMarkerEvent> ff1East = new ModifiableCallout<>("Fledling Flight 1 East", "Go East");
	private final ModifiableCallout<HeadMarkerEvent> ff1North = new ModifiableCallout<>("Fledling Flight 1 North", "Go North");
	private final ModifiableCallout<HeadMarkerEvent> ff1South = new ModifiableCallout<>("Fledling Flight 1 South", "Go South");
	private final ModifiableCallout<HeadMarkerEvent> ff2West = new ModifiableCallout<>("Fledling Flight 2 West", "Go West");
	private final ModifiableCallout<HeadMarkerEvent> ff2East = new ModifiableCallout<>("Fledling Flight 2 East", "Go East");
	private final ModifiableCallout<HeadMarkerEvent> ff2North = new ModifiableCallout<>("Fledling Flight 2 North", "Go North");
	private final ModifiableCallout<HeadMarkerEvent> ff2South = new ModifiableCallout<>("Fledling Flight 2 South", "Go South");

	private final ModifiableCallout<AbilityCastStart> expFpShiva = new ModifiableCallout<>("Experimental Fireplume (Shiva)", "Middle then Shiva Circles");
	private final ModifiableCallout<AbilityCastStart> expFpOut = new ModifiableCallout<>("Experimental Fireplume (Out)", "Middle then Out");

	// Not going to make these duration-based
	private final ModifiableCallout<AbilityCastStart> expGpShiva = new ModifiableCallout<>("Experimental Gloryplume (Shiva)", "Shiva Circles");
	private final ModifiableCallout<AbilityCastStart> expGpOut = new ModifiableCallout<>("Experimental Gloryplume (Out)", "Out");
	private final ModifiableCallout<AbilityUsedEvent> expGpSpread = new ModifiableCallout<>("Experimental Gloryplume (Spread)", "Spread");
	private final ModifiableCallout<AbilityUsedEvent> expGpLightParties = new ModifiableCallout<>("Experimental Gloryplume (Light Parties)", "Light Parties");

	private final ModifiableCallout<AbilityCastStart> expApSpread = new ModifiableCallout<>("Experimental Ashplume (Spread)", "Spread");
	private final ModifiableCallout<AbilityCastStart> expApLightParties = new ModifiableCallout<>("Experimental Ashplume (Light Parties)", "Light Parties");

	private final ModifiableCallout<AbilityCastStart> diveSidesSpread = new ModifiableCallout<>("Divebomb (Quickmarch)", "Sides and Spread");
	private final ModifiableCallout<AbilityCastStart> diveMiddlePairs = new ModifiableCallout<>("Divebomb (Middle+Partners)", "Middle and Pairs");

	private final ModifiableCallout<AbilityCastStart> leftWingBad = ModifiableCallout.durationBasedCall("Left Wing Bad", "Go Right");
	private final ModifiableCallout<AbilityCastStart> rightWingBad = ModifiableCallout.durationBasedCall("Right Wing Bad", "Go Left");

	private final ModifiableCallout<TetherEvent> tetheredToBird = new ModifiableCallout<>("Tethered to Bird + Player", "Tethered to {birdspot} bird and {otherplayer}");
	private final ModifiableCallout<TetherEvent> tetheredToPlayer = new ModifiableCallout<>("Tethered to Player", "Tethered to {otherplayer} ({birdspot} bird)");

	private final ModifiableCallout<BuffApplied> deathsToll1 = new ModifiableCallout<>("Death's Toll (1)", "1 Stack (Cardinal)");
	private final ModifiableCallout<BuffApplied> deathsToll2 = new ModifiableCallout<>("Death's Toll (2)", "2 Stacks (Intercard)");
	private final ModifiableCallout<BuffApplied> deathsToll4 = new ModifiableCallout<>("Death's Toll (4)", "4 Stacks (Middle)");
	private final ModifiableCallout<BuffApplied> deathsTollN = new ModifiableCallout<>("Death's Toll (?)", "{stacks} Stacks");

	// TODO: test this, make sure it didn't break
	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	private final XivState state;

	public P3S(XivState state) {
		this.state = state;
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityCastStart> call;
			if (id == 0x6706) {
				call = scorchedExaltation;
			}
			else if (id == 0x6700) {
				call = heatOfCondemnation;
			}
			else if (id == 0x66E4) {
				call = deadRebirth;
			}
			else if (id == 0x66F0) {
				call = firestorms;
			}
			else if (id == 0x6702) {
				call = rightWingBad;
			}
			else if (id == 0x6703) {
				call = leftWingBad;
			}
			else if (id == 0x66B9) {
				call = darkenedFire;
			}
			else if (id == 0x66BE) {
				call = expFpOut;
			}
			else if (id == 0x66C0) {
				call = expFpShiva;
			}

			else if (id == 0x66C6) {
				call = expGpOut;
			}
			else if (id == 0x66CA) {
				call = expGpShiva;
			}
			else if (id == 0x66C2) {
				call = expApLightParties;
			}
			else if (id == 0x66C4) {
				call = expApSpread;
			}
			else if (id == 0x66FB) {
				call = diveSidesSpread;
			}
			else if (id == 0x66FC) {
				call = diveMiddlePairs;
			}
			else {
				return;
			}
			context.accept(call.getModified(event));
		}
	}

	@HandleEvents
	public void actualCast(EventContext context, AbilityUsedEvent event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout<AbilityUsedEvent> call;
			if (id == 0x66C8) {
				call = expGpSpread;
			}
			else if (id == 0x66CC) {
				call = expGpLightParties;
			}
			else {
				return;
			}
			context.accept(call.getModified(event));
		}
	}

	private Long firstHeadmark;
	private boolean isDeathsToll;

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		firstHeadmark = null;
		birdTethers.clear();
		isDeathsToll = false;
	}

	@HandleEvents
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		// This is done unconditionally to create the headmarker offset
		int headmarkOffset = getHeadmarkOffset(event);
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}
		ModifiableCallout<HeadMarkerEvent> call = switch (headmarkOffset) {
			case 0 -> number1;
			case 1 -> number2;
			case 2 -> number3;
			case 3 -> number4;
			case 4 -> number5;
			case 5 -> number6;
			case 6 -> number7;
			case 7 -> number8;
			case 28 -> isDeathsToll ? ff2West : ff1East;
			case 29 -> isDeathsToll ? ff2East : ff1West;
			case 30 -> isDeathsToll ? ff2North : ff1South;
			case 31 -> isDeathsToll ? ff2South : ff1North;
			default -> null;
		};
		if (call != null) {
			context.accept(call.getModified(event));
		}
	}

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}

	private final List<TetherEvent> birdTethers = new ArrayList<>();

	@HandleEvents
	public void birdTether(EventContext context, TetherEvent tether) {
		XivPlayerCharacter player = state.getPlayer();
		if (tether.getId() == 0x39 || tether.getId() == 0x1) {
			birdTethers.add(tether);
			if (birdTethers.size() == 8) {
				// Two possibilities: you are tied to a bird and a player, or you are tied to another player and they are tied to a bird
				Set<XivCombatant> tetheredToPlayer = TetherEvent.getUnitsTetheredTo(player, birdTethers);
				final XivCombatant bird;
				final XivCombatant otherPlayer;
				if (tetheredToPlayer.size() == 2) {
					List<XivCombatant> tetheredSorted = new ArrayList<>(tetheredToPlayer);
					tetheredSorted.sort(Comparator.comparing(XivEntity::getId));
					bird = tetheredSorted.get(1);
					otherPlayer = tetheredSorted.get(0);
					String birdSpot = arenaPos.forCombatant(bird).getFriendlyName();
					context.accept(this.tetheredToBird.getModified(tether, Map.of("otherplayer", otherPlayer, "birdspot", birdSpot)));
				}
				else if (tetheredToPlayer.size() == 1) {
					otherPlayer = tetheredToPlayer.iterator().next();
					Set<XivCombatant> tetheredToOtherPlayer = TetherEvent.getUnitsTetheredTo(otherPlayer, birdTethers);
					tetheredToOtherPlayer.remove(player);
					if (tetheredToOtherPlayer.size() != 1) {
						log.error("Error doing bird tethers: expected other player to be tethered to 1 bird, but they were tethered to: {}", tetheredToOtherPlayer);
						return;
					}
					bird = tetheredToOtherPlayer.iterator().next();
					String birdSpot = arenaPos.forCombatant(bird).getFriendlyName();
					context.accept(this.tetheredToPlayer.getModified(tether, Map.of("otherplayer", otherPlayer, "birdspot", birdSpot)));
				}
				else {
					log.error("Expected to be tethered to one or two other entities, but was tethered to: {}", tetheredToPlayer);
				}

			}
		}
	}

	@HandleEvents
	public void deathsToll(EventContext context, BuffApplied buff) {
		// Stack counting down would be considered a refresh
		if (buff.getBuff().getId() == 0xACA) {
			isDeathsToll = true;
			if (buff.getTarget().isThePlayer() && !buff.isRefresh()) {
				long stacks = buff.getStacks();
				ModifiableCallout<BuffApplied> callout = switch ((int) stacks) {
					case 1 -> deathsToll1;
					case 2 -> deathsToll2;
					case 4 -> deathsToll4;
					default -> deathsTollN;
				};
				context.accept(callout.getModified(buff, Map.of("stacks", stacks)));
			}
		}

	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P3S);
	}

}
