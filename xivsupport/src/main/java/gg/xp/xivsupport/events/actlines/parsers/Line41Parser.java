package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.SystemLogMessageEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line41Parser extends AbstractACTLineParser<Line41Parser.Fields> {

	public Line41Parser(PicoContainer container) {
		super(container, 41, Fields.class);
	}

	enum Fields {
		unknown1, id, param0, param1, param2
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new SystemLogMessageEvent(
				fields.getHex(Fields.unknown1),
				fields.getHex(Fields.id),
				fields.getHex(Fields.param0),
				fields.getHex(Fields.param1),
				fields.getHex(Fields.param2));
	}
}
