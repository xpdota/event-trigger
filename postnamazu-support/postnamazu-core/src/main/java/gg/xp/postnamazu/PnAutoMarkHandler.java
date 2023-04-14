package gg.xp.postnamazu;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.services.ServiceDescriptor;
import gg.xp.services.ServiceHandle;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.playermarkers.PlayerMarkerRepository;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkHandler;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkLanguage;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.AutoMarkServiceSelector;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.models.XivCombatant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK1;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK2;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK3;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK4;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.ATTACK5;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND1;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND2;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.BIND3;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.CLEAR;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.IGNORE1;
import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.IGNORE2;

public class PnAutoMarkHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(PnAutoMarkHandler.class);
	private final ServiceHandle pn;
	private final XivState state;
	private final AutoMarkHandler amh;
	private final PlayerMarkerRepository pmr;
	private int atkCounter;
	private int ignCounter;
	private int bindCounter;

	public PnAutoMarkHandler(AutoMarkServiceSelector serv, XivState state, AutoMarkHandler amh, PlayerMarkerRepository pmr) {
		pn = serv.register(ServiceDescriptor.of("postnamazu", "PostNamazu (Requires ACT Plugin)", 11));
		this.state = state;
		this.amh = amh;
		this.pmr = pmr;
	}

	@HandleEvents(order = -10_000)
	public void handleSpecificAm(EventContext context, SpecificAutoMarkRequest amr) {
		if (amr.isHandled()) {
			return;
		}
		log.info("PN auto mark request: mark {} with {}", amr.getTarget().getName(), amr.getMarker());
		MarkerSign mark = amr.getMarker();
		XivCombatant target = amr.getTarget();
		if (mark == CLEAR) {
			MarkerSign existingMarker = pmr.signOnCombatant(target);
			if (existingMarker != null) {
				// https://github.com/Natsukage/PostNamazu/issues/32#issuecomment-1478739976
				// Clearing a mark is done by marking 0xE000000 with the marker you wish to clear.
				// When you do `/mk clear`, the game does more or less the same logic.
				// PROBLEM: mixing marks and clear in the same set of AMs will cause a race condition!
				// e.g.:
				// Request to mark Player One with Attack1 and request to clear marker on Player Two, while Player Two
				//     currently holds Attack1
				// Thus, we submit the request to mark Player One, which works. But it takes time to process - so
				//     the 'Clear Player 2' resolves to 'Get Rid of the Attack1 Marker'
				// There WAS ALSO another problem where the ClearAll and AMs get mixed up because of having
				//     separate queues, but PnGameCommand now has an optional argument to make it use the AM queue.
				context.accept(PnOutgoingMessage.mark(0xE00_0000, existingMarker));
			}
		}
		else {
			MarkerSign markerToPlace = switch (mark) {
				case ATTACK_NEXT -> nextEmptyAtk();
				case BIND_NEXT -> nextEmptyBind();
				case IGNORE_NEXT -> nextEmptyIgn();
				default -> mark;
			};
			context.accept(PnOutgoingMessage.mark(target, markerToPlace));
		}
		amr.setHandled();
	}

	@HandleEvents(order = -10_000)
	public void handleAm(EventContext context, AutoMarkRequest amr) {
		if (amr.isHandled()) {
			return;
		}
		context.accept(PnOutgoingMessage.mark(amr.getTarget(), nextEmptyAtk()));
		amr.setHandled();
	}

	private MarkerSign nextEmptyAtk() {
		return switch (atkCounter = (atkCounter + 1) % 5) {
			case 0 -> ATTACK5;
			case 1 -> ATTACK1;
			case 2 -> ATTACK2;
			case 3 -> ATTACK3;
			case 4 -> ATTACK4;
			default -> throw new IllegalArgumentException("Bad value: " + atkCounter);
		};
	}

	private MarkerSign nextEmptyBind() {
		return switch (bindCounter = (bindCounter + 1) % 3) {
			case 0 -> BIND3;
			case 1 -> BIND1;
			case 2 -> BIND2;
			default -> throw new IllegalArgumentException("Bad value: " + bindCounter);
		};
	}

	private MarkerSign nextEmptyIgn() {
		return switch (ignCounter = (ignCounter + 1) % 2) {
			case 0 -> IGNORE2;
			case 1 -> IGNORE1;
			default -> throw new IllegalArgumentException("Bad value: " + ignCounter);
		};
	}

	@HandleEvents(order = -10_000)
	public void handleClear(EventContext context, ClearAutoMarkRequest clear) {
		if (clear.isHandled()) {
			return;
		}
		context.accept(new PnGameCommand("/mk clear <1>", true));
		context.accept(new PnGameCommand("/mk clear <2>", true));
		context.accept(new PnGameCommand("/mk clear <3>", true));
		context.accept(new PnGameCommand("/mk clear <4>", true));
		context.accept(new PnGameCommand("/mk clear <5>", true));
		context.accept(new PnGameCommand("/mk clear <6>", true));
		context.accept(new PnGameCommand("/mk clear <7>", true));
		context.accept(new PnGameCommand("/mk clear <8>", true));
		atkCounter = 0;
		bindCounter = 0;
		ignCounter = 0;
		clear.setHandled();
	}


	@Override
	public boolean enabled(EventContext context) {
		return pn.enabled();
	}
}
