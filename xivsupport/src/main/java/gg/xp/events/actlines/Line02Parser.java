package gg.xp.events.actlines;

import gg.xp.events.Event;
import gg.xp.events.XivEntity;

import java.time.ZonedDateTime;
import java.util.Map;

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
		return new PlayerChangeEvent(new XivEntity(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
