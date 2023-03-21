package gg.xp.postnamazu;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.services.ServiceDescriptor;
import gg.xp.services.ServiceHandle;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkLanguage;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.AutoMarkServiceSelector;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;

import java.util.Map;

import static gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign.*;

public class PnAutoMarkHandler implements FilteredEventHandler {

	private final ServiceHandle pn;
	private int atkCounter;
	private int ignCounter;
	private int bindCounter;

	public PnAutoMarkHandler(AutoMarkServiceSelector serv) {
		pn = serv.register(ServiceDescriptor.of("postnamazu", "PostNamazu (Requires ACT Plugin)", 11));
	}

	@HandleEvents(order = -10_000)
	public void handleSpecificAm(EventContext context, SpecificAutoMarkRequest amr) {
		if (amr.isHandled()) {
			return;
		}
		MarkerSign mark = amr.getMarker();
		if (mark == CLEAR) {
			context.accept(new PnOutgoingMessage("mark", Map.of(
							"ActorID", amr.getTarget().getId(),
							"MarkType", 0
					))
			);
		}
		else {
			MarkerSign markerToPlace = switch (mark) {
				case ATTACK_NEXT -> nextEmptyAtk();
				case BIND_NEXT -> nextEmptyBind();
				case IGNORE_NEXT -> nextEmptyIgn();
				default -> mark;
			};
			context.accept(new PnOutgoingMessage("mark", Map.of(
							"ActorID", amr.getTarget().getId(),
							"MarkType", markerToPlace.getCommand(AutoMarkLanguage.EN)
					))
			);
		}
		amr.setHandled();
	}

	@HandleEvents(order = -10_000)
	public void handleAm(EventContext context, AutoMarkRequest amr) {
		if (amr.isHandled()) {
			return;
		}
		context.accept(new PnOutgoingMessage("mark", Map.of(
						"ActorID", amr.getTarget().getId(),
						"MarkType", nextEmptyAtk().getCommand(AutoMarkLanguage.EN)
				))
		);
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
		context.accept(new PnGameCommand("/mk clear <1>"));
		context.accept(new PnGameCommand("/mk clear <2>"));
		context.accept(new PnGameCommand("/mk clear <3>"));
		context.accept(new PnGameCommand("/mk clear <4>"));
		context.accept(new PnGameCommand("/mk clear <5>"));
		context.accept(new PnGameCommand("/mk clear <6>"));
		context.accept(new PnGameCommand("/mk clear <7>"));
		context.accept(new PnGameCommand("/mk clear <8>"));
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
