package gg.xp.telestosupport.subscriber;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.state.XivState;

import java.util.Map;

public class TelestoTransformationHandler {

	private final XivState state;

	public TelestoTransformationHandler(XivState state) {
		this.state = state;
	}

	@HandleEvents
	public void handleMessage(EventContext context, TelestoSubscriptionMessage message) {
		// TODO
		if (message.getNotificationType().equals("trid")) {
//		if (message.getNotificationType().equals("hp")) {
			Map<String, Object> p = message.getPayload();
			int newTfId = Integer.parseInt(p.get("newvalue").toString());
			long entityId = Long.parseLong(p.get("objectid").toString(), 16);
			state.provideTransformation(entityId, (short) newTfId);
		}
	}
}
