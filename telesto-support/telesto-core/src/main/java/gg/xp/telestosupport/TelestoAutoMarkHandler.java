package gg.xp.telestosupport;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkHandler;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkSlotRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkSlotRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelestoAutoMarkHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(TelestoAutoMarkHandler.class);
	private final AutoMarkHandler am;

	public TelestoAutoMarkHandler(AutoMarkHandler am) {
		this.am = am;
	}

	@HandleEvents
	public void doAutoMark(EventContext context, AutoMarkSlotRequest event) {
		context.accept(new TelestoGameCommand(String.format("/mk attack <%s>", event.getSlotToMark())));
	}

	@HandleEvents
	public void doSpecificAutoMark(EventContext context, SpecificAutoMarkSlotRequest event) {
		MarkerSign marker = event.getMarker();
		context.accept(new TelestoGameCommand(String.format(
				"/mk %s <%s>",
				marker.getCommand(am.getEffectiveLanguage()),
				event.getSlotToMark())));
	}

	private void clearAutoMark(EventContext context) {
		context.accept(new TelestoGameCommand("/mk clear <1>"));
		context.accept(new TelestoGameCommand("/mk clear <2>"));
		context.accept(new TelestoGameCommand("/mk clear <3>"));
		context.accept(new TelestoGameCommand("/mk clear <4>"));
		context.accept(new TelestoGameCommand("/mk clear <5>"));
		context.accept(new TelestoGameCommand("/mk clear <6>"));
		context.accept(new TelestoGameCommand("/mk clear <7>"));
		context.accept(new TelestoGameCommand("/mk clear <8>"));
	}

	@HandleEvents
//	@LiveOnly
	public void clearMarks(EventContext context, ClearAutoMarkRequest event) {
		log.info("Clearing marks");
		clearAutoMark(context);
	}


	@Override
	public boolean enabled(EventContext context) {
		return am.getUseTelesto().get();
	}
}
