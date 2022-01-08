package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
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
import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CalloutRepo("P3S")
public class P3S implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(P3S.class);

	private final ModifiableCallout scorchedExaltation = new ModifiableCallout("Scorched Exaltation", "Raidwide");
	private final ModifiableCallout heatOfCondemnation = new ModifiableCallout("Heat of Condemnation", "Tank Tethers");
	private final ModifiableCallout deadRebirth = new ModifiableCallout("Dead Rebirth", "Big Aoe");

	private final ModifiableCallout darkenedFire = new ModifiableCallout("Darkened Fire", "Partners");
	private final ModifiableCallout number1 = new ModifiableCallout("#1", "1");
	private final ModifiableCallout number2 = new ModifiableCallout("#2", "2");
	private final ModifiableCallout number3 = new ModifiableCallout("#3", "3");
	private final ModifiableCallout number4 = new ModifiableCallout("#4", "4");
	private final ModifiableCallout number5 = new ModifiableCallout("#5", "5");
	private final ModifiableCallout number6 = new ModifiableCallout("#6", "6");
	private final ModifiableCallout number7 = new ModifiableCallout("#7", "7");
	private final ModifiableCallout number8 = new ModifiableCallout("#8", "8");

	private final ModifiableCallout expFpShiva = new ModifiableCallout("Experimental Fireplume (Shiva)", "Middle then Shiva Circles");
	private final ModifiableCallout expFpOut = new ModifiableCallout("Experimental Fireplume (Out)", "Middle then Out");

	private final ModifiableCallout expGpShiva = new ModifiableCallout("Experimental Gloryplume (Shiva)", "Shiva Circles");
	private final ModifiableCallout expGpOut = new ModifiableCallout("Experimental Gloryplume (Out)", "Out");
	private final ModifiableCallout expGpSpread = new ModifiableCallout("Experimental Gloryplume (Spread)", "Spread");
	private final ModifiableCallout expGpLightParties = new ModifiableCallout("Experimental Gloryplume (Light Parties)", "Light Parties");

	private final ModifiableCallout expApSpread = new ModifiableCallout("Experimental Ashplume (Spread)", "Spread");
	private final ModifiableCallout expApLightParties = new ModifiableCallout("Experimental Ashplume (Light Parties)", "Light Parties");

	private final ModifiableCallout diveSidesSpread = new ModifiableCallout("Divebomb (Quickmarch)", "Sides and Spread");
	private final ModifiableCallout diveMiddlePairs = new ModifiableCallout("Divebomb (Middle+Partners)", "Middle and Pairs");

	private final ModifiableCallout leftWingBad = new ModifiableCallout("Left Wing Bad", "Go Right");
	private final ModifiableCallout rightWingBad = new ModifiableCallout("Right Wing Bad", "Go Left");

	private final ModifiableCallout tetheredToBird = new ModifiableCallout("Tethered to Bird + Player", "Tethered to {birdspot} bird and {otherplayer}");
	private final ModifiableCallout tetheredToPlayer = new ModifiableCallout("Tethered to Player", "Tethered to {otherplayer} ({birdspot} bird)");

	private final ModifiableCallout deathsToll1 = new ModifiableCallout("Death's Toll (1)", "1 Stack (Cardinal)");
	private final ModifiableCallout deathsToll2 = new ModifiableCallout("Death's Toll (2)", "2 Stacks (Intercard)");
	private final ModifiableCallout deathsToll4 = new ModifiableCallout("Death's Toll (4)", "4 Stacks (Middle)");
	private final ModifiableCallout deathsTollN = new ModifiableCallout("Death's Toll (?)", "{stacks} Stacks");

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
			ModifiableCallout call;
			if (id == 0x6706) {
				call = scorchedExaltation;
			}
			else if (id == 0x6700) {
				call = heatOfCondemnation;
			}
			else if (id == 0x66E4) {
				call = deadRebirth;
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
			context.accept(call.getModified());
		}
	}

	@HandleEvents
	public void actualCast(EventContext context, AbilityUsedEvent event) {
		if (event.getSource().getType() == CombatantType.NPC) {
			long id = event.getAbility().getId();
			ModifiableCallout call;
			if (id == 0x66C8) {
				call = expGpSpread;
			}
			else if (id == 0x66CC) {
				call = expGpLightParties;
			}
			else {
				return;
			}
			context.accept(call.getModified());
		}
	}

	private Long firstHeadmark;

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		firstHeadmark = null;
		birdTethers.clear();
	}

	@HandleEvents
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		// This is done unconditionally to create the headmarker offset
		int headmarkOffset = getHeadmarkOffset(event);
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}
		ModifiableCallout call = switch (headmarkOffset) {
			case 0:
				yield number1;
			case 1:
				yield number2;
			case 2:
				yield number3;
			case 3:
				yield number4;
			case 4:
				yield number5;
			case 5:
				yield number6;
			case 6:
				yield number7;
			case 7:
				yield number8;
			default:
				yield null;
		};
		if (call != null) {
			context.accept(call.getModified());
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
		if (tether.getId() == 0x39) {
			birdTethers.add(tether);
			if (birdTethers.size() == 8) {
				try {
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
						context.accept(this.tetheredToBird.getModified(Map.of("otherplayer", otherPlayer, "birdspot", birdSpot)));
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
						context.accept(this.tetheredToPlayer.getModified(Map.of("otherplayer", otherPlayer, "birdspot", birdSpot)));
					}
					else {
						log.error("Expected to be tethered to one or two other entities, but was tethered to: {}", tetheredToPlayer);
					}
				} finally {
					birdTethers.clear();
				}

			}
		}
	}

	@HandleEvents
	public void deathsToll(EventContext context, BuffApplied buff) {
		// Stack counting down would be considered a refresh
		if (buff.getBuff().getId() == 0xACA && buff.getTarget().isThePlayer() && !buff.isRefresh()) {
			long stacks = buff.getStacks();
			CalloutEvent callout = switch ((int) stacks) {
				case 1 -> deathsToll1.getModified();
				case 2 -> deathsToll2.getModified();
				case 4 -> deathsToll4.getModified();
				default -> deathsTollN.getModified(Map.of("stacks", stacks));
			};
			context.accept(callout);
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3EF);
	}

}
