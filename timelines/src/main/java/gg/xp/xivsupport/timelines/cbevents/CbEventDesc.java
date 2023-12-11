package gg.xp.xivsupport.timelines.cbevents;

import gg.xp.reevent.events.Event;

import java.util.List;

public interface CbEventDesc<X extends Event> {

	Class<X> getEventType();
	List<CbfMap<? super X>> getFieldMappings();

}
