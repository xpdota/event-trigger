package gg.xp.events.actlines;

import gg.xp.events.Event;

import java.time.ZonedDateTime;
import java.util.Map;

@SuppressWarnings("unused")
public class Line00Parser extends AbstractACTLineParser<Line00Parser.Fields> {

	public Line00Parser() {
		super(0, Fields.class);
	}

	enum Fields {
		code, name, line
	}

	@Override
	protected Event convert(Map<Fields, String> fields, int lineNumber, ZonedDateTime time) {
		return new ZeroLogLineEvent(Long.parseLong(fields.get(Fields.code), 16), fields.get(Fields.name), fields.get(Fields.line));
	}
}
