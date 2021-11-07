package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.ZeroLogLineEvent;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line00Parser extends AbstractACTLineParser<Line00Parser.Fields> {

	public Line00Parser() {
		super(0, Fields.class);
	}

	enum Fields {
		code, name, line
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new ZeroLogLineEvent(fields.getHex(Fields.code), fields.getString(Fields.name), fields.getString(Fields.line));
	}
}
