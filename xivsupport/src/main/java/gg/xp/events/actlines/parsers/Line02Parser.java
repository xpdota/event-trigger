package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line02Parser extends AbstractACTLineParser<Line02Parser.Fields> {

	public Line02Parser() {
		super(2, Fields.class);
	}

	enum Fields {
		id, name
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: some kind of @Disable annotation
		return null;
//		return new PlayerChangeEvent(new XivEntity(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
