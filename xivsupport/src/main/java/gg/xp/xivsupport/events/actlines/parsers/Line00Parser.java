package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.state.XivStateImpl;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line00Parser extends AbstractACTLineParser<Line00Parser.Fields> {

	public Line00Parser(org.picocontainer.PicoContainer container) {
		super(container,  0, Fields.class);
	}

	enum Fields {
		code, name, line
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new ChatLineEvent(fields.getHex(Fields.code), fields.getString(Fields.name), fields.getString(Fields.line));
	}
}
