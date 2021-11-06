package gg.xp.events.actlines;

import gg.xp.events.Event;
import gg.xp.events.XivEntity;
import gg.xp.events.XivZone;

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
		return new ZoneChangeEvent(new XivZone(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
