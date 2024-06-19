package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Pseudo-event that indicates that a parser is returning multiple events
 */
public class MultipleEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = 2610619497808626940L;
	final List<Event> events = new ArrayList<>();

	public void add(Event event) {
		this.events.add(event);
	}
}
