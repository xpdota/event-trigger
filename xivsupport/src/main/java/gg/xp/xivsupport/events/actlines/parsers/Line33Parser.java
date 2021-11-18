package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line33Parser extends AbstractACTLineParser<Line33Parser.Fields> {

	public Line33Parser() {
		super(33, Fields.class);
	}

	enum Fields {
		instance, command, data0, data1, data2, data3
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new ActorControlEvent(
				fields.getHex(Fields.instance),
				fields.getHex(Fields.command),
				fields.getHex(Fields.data0),
				fields.getHex(Fields.data1),
				fields.getHex(Fields.data2),
				fields.getHex(Fields.data3)
		);
	}
}
