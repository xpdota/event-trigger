package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.FeedHandlerChildInfo;
import gg.xp.reevent.scan.FeedHelperAdapter;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;

@ScanMe
public class HeadmarkerAdapter implements FeedHelperAdapter<PlayerHeadmarker, HeadMarkerEvent, ModifiableCallout<HeadMarkerEvent>> {

	@Override
	public Class<HeadMarkerEvent> eventType() {
		return HeadMarkerEvent.class;
	}

	@Override
	public TypedEventHandler<HeadMarkerEvent> makeHandler(FeedHandlerChildInfo<PlayerHeadmarker, ModifiableCallout<HeadMarkerEvent>> info) {
		long[] hmIds = info.getAnnotation().value();
		boolean offset = info.getAnnotation().offset();
		return new TypedEventHandler<>() {
			@Override
			public Class<? extends HeadMarkerEvent> getType() {
				return HeadMarkerEvent.class;
			}

			@Override
			public void handle(EventContext context, HeadMarkerEvent event) {
				long id = offset ? event.getMarkerOffset() : event.getMarkerId();
				if (event.getTarget().isThePlayer()) {
					for (int i = 0; i < hmIds.length; i++) {
						if (hmIds[i] == id) {
							context.accept(info.getHandlerFieldValue().getModified(event));
							return;
						}
					}
				}
			}
		};
	}
}
