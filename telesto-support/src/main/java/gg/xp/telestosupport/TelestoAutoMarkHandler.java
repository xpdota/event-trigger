package gg.xp.telestosupport;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkHandler;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkSlotRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
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
		doAutoMarkForSlot(context, event.getSlotToMark());
	}

	private static void doAutoMarkForSlot(EventContext context, int partySlot) {
		context.accept(new TelestoGameCommand(String.format("/mk attack <%s>", partySlot)));
	}

	private void clearAutoMark(EventContext context) {
		context.accept(new TelestoGameCommand("/mk clear 1"));
		context.accept(new TelestoGameCommand("/mk clear 2"));
		context.accept(new TelestoGameCommand("/mk clear 3"));
		context.accept(new TelestoGameCommand("/mk clear 4"));
		context.accept(new TelestoGameCommand("/mk clear 5"));
		context.accept(new TelestoGameCommand("/mk clear 6"));
		context.accept(new TelestoGameCommand("/mk clear 7"));
		context.accept(new TelestoGameCommand("/mk clear 8"));
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
