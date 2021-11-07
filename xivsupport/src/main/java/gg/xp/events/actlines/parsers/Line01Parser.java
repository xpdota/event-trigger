package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line01Parser extends AbstractACTLineParser<Line01Parser.Fields> {

	public Line01Parser() {
		super(1, Fields.class);
	}

	enum Fields {
		id, name
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: some kind of @Disable annotation
		return null;
//		return new ZoneChangeEvent(new XivZone(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
