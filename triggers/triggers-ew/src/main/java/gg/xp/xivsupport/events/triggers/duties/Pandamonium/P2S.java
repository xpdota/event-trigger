package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.events.triggers.support.PlayerHeadmarker;
import gg.xp.xivsupport.models.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CalloutRepo(name = "P2S", duty = KnownDuty.P2S)
public class P2S extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P2S.class);
	private final ModifiableCallout<BuffApplied> leftTide = ModifiableCallout.durationBasedCall("Left Push", "{longshort} West Push");
	private final ModifiableCallout<BuffApplied> rightTide = ModifiableCallout.durationBasedCall("Right Push", "{longshort} East Push");
	private final ModifiableCallout<BuffApplied> foreTide = ModifiableCallout.durationBasedCall("Fore Push", "{longshort} North Push");
	private final ModifiableCallout<BuffApplied> rearTide = ModifiableCallout.durationBasedCall("Rear Push", "{longshort} South Push");

	@PlayerHeadmarker(value = -114, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> blue1 = new ModifiableCallout<>("Blue #1", "Blue 1");
	@PlayerHeadmarker(value = -113, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> blue2 = new ModifiableCallout<>("Blue #2", "Blue 2");
	@PlayerHeadmarker(value = -112, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> blue3 = new ModifiableCallout<>("Blue #3", "Blue 3");
	@PlayerHeadmarker(value = -111, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> blue4 = new ModifiableCallout<>("Blue #4", "Blue 4");
	@PlayerHeadmarker(value = -110, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> purp1 = new ModifiableCallout<>("Purple #1", "Purple 1");
	@PlayerHeadmarker(value = -109, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> purp2 = new ModifiableCallout<>("Purple #2", "Purple 2");
	@PlayerHeadmarker(value = -108, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> purp3 = new ModifiableCallout<>("Purple #3", "Purple 3");
	@PlayerHeadmarker(value = -107, offset = true)
	private final ModifiableCallout<HeadMarkerEvent> purp4 = new ModifiableCallout<>("Purple #4", "Purple 4");

	private final ModifiableCallout<AbilityCastStart> shockwave = ModifiableCallout.durationBasedCall("Shockwave", "Knockback");
	private final ModifiableCallout<AbilityCastStart> sewageDeluge = ModifiableCallout.durationBasedCall("Sewage Deluge", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> murkyDepths = ModifiableCallout.durationBasedCall("Murky Depths", "Raidwide");
	private final ModifiableCallout<AbilityCastStart> taintedFlood = ModifiableCallout.durationBasedCall("Tainted Flood", "Spread");
	private final ModifiableCallout<AbilityCastStart> winged = ModifiableCallout.durationBasedCall("Winged Cataract", "Go In Front of Head");
	private final ModifiableCallout<AbilityCastStart> spoken = ModifiableCallout.durationBasedCall("Spoken Cataract", "Go Behind Head");

	private final ModifiableCallout<AbilityCastStart> dissociationWestSafe = new ModifiableCallout<>("Dissociation (W safe)", "West Safe");
	private final ModifiableCallout<AbilityCastStart> dissociationEastSafe = new ModifiableCallout<>("Dissociation (E safe)", "East Safe");

	private final ModifiableCallout<BuffApplied> stack = ModifiableCallout.durationBasedCall("Mark of the Depths", "Stack on {event.getTarget()}");
	private final ModifiableCallout<BuffApplied> tides = ModifiableCallout.durationBasedCall("Mark of the Tides","Get Away");

	private final List<BuffApplied> stackSpreadBuffs = new ArrayList<>();

	private final XivState state;

	public P2S(XivState state) {
		this.state = state;
	}

	@HandleEvents
	public void stackOrSpread(EventContext context, BuffApplied event) {
		long id = event.getBuff().getId();
		// AD0 = get away, AD1 = stack
		// Use same logic as jails since these always come in sets of 3
		if (id == 0xAD0 || id == 0xAD1) {
			stackSpreadBuffs.add(event);
			if (stackSpreadBuffs.size() == 3) {
				// First check if the player has a spread debuff on them
				Optional<BuffApplied> spreadOnYou = stackSpreadBuffs.stream().filter(ba -> ba.getTarget().isThePlayer() && ba.getBuff().getId() == 0xAD0).findAny();
				if (spreadOnYou.isPresent()) {
					context.accept(tides.getModified(spreadOnYou.get()));
				}
				else {
					Optional<BuffApplied> anyStack = stackSpreadBuffs.stream().filter(ba -> ba.getBuff().getId() == 0xAD1).findAny();
					if (anyStack.isPresent()) {
						context.accept(stack.getModified(anyStack.get(), Map.of("target", anyStack.get().getTarget())));
					}
					else {
						log.warn("Found no stack! Events: {}", stackSpreadBuffs);
					}
				}
				resetStackSpread();
			}
		}
	}

	@HandleEvents
	public void stackOrSpreadRemove(EventContext context, BuffRemoved event) {
		long id = event.getBuff().getId();
		if (id == 0xAD0 || id == 0xAD1) {
			resetStackSpread();
		}
	}

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		resetStackSpread();
	}

	private void resetStackSpread() {
		stackSpreadBuffs.clear();
	}

	@HandleEvents
	public void pushBuff(EventContext context, BuffApplied event) {
		if (event.getTarget().isThePlayer()) {
			ModifiableCallout<BuffApplied> call;
			if (event.getBuff().getId() == 0xAD4) {
				call = leftTide;
			}
			else if (event.getBuff().getId() == 0xAD5) {
				call = rightTide;
			}
			else if (event.getBuff().getId() == 0xAD3) {
				call = rearTide;
			}
			else if (event.getBuff().getId() == 0xAD2) {
				call = foreTide;
			}
			else {
				return;
			}
			String durationText = event.getInitialDuration().getSeconds() > 15 ? "Long" : "Short";
			context.accept(call.getModified(event, Map.of("longshort", durationText)));
		}
	}

	@HandleEvents
	public void simpleAbilities(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		if (id == 0x6810) {
			context.accept(sewageDeluge.getModified(event));
		}
		else if (id == 0x6833) {
			context.accept(murkyDepths.getModified(event));
		}
		else if (id == 0x6838 && event.getTarget().isThePlayer()) {
			// TODO: check this
			context.accept(taintedFlood.getModified(event));
		}
	}

	@AutoFeed
	private final SequentialTrigger<BaseEvent> shockwaveSq = SqtTemplates.callWhenDurationIs(
			AbilityCastStart.class, acs -> acs.abilityIdMatches(0x682F),
			shockwave,
			Duration.ofSeconds(6));

	@HandleEvents
	public void cataract(EventContext context, AbilityCastStart event) {
		if (event.getSource().getbNpcId() == 0x359b) {
			long id = event.getAbility().getId();
			// TODO: are the various unique IDs perhaps the directions?
			if (id == 0x6817 || id == 0x6811 || id == 0x6812 || id == 0x6813) {
				context.accept(spoken.getModified(event));
			}
			else if (id == 0x6814 || id == 0x6815 || id == 0x6818 || id == 0x6816) {
				context.accept(winged.getModified(event));
			}
		}
	}

	@HandleEvents
	public void dissociation(EventContext context, AbilityCastStart event) {
		if (event.getSource().getbNpcId() == 0x386a && event.getAbility().getId() == 0x682E) {
			Position pos = event.getSource().getPos();
			if (pos == null) {
				return;
			}
			double x = pos.getX();
			if (x > 100) {
				context.accept(dissociationWestSafe.getModified(event));
			}
			else {
				context.accept(dissociationEastSafe.getModified(event));
			}
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P2S);
	}
}
