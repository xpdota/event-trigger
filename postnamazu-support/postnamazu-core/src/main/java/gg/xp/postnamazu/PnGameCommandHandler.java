package gg.xp.postnamazu;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;

@ScanMe
public class PnGameCommandHandler {

	@HandleEvents
	public void pnGameCmd(EventContext context, PnGameCommand pgc) {
		if (pgc.getUsesAmQueue()) {
			context.accept(PnOutgoingMessage.markerCommand(pgc.getCommand()));
		}
		else {
			context.accept(PnOutgoingMessage.command(pgc.getCommand()));
		}
	}

}
