package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.RawAddCombatantEvent;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line03Parser extends AbstractACTLineParser<Line03Parser.Fields> {

	public Line03Parser() {
		super(3, Fields.class);
	}

	enum Fields {
		id, name, job
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: some kind of @Disable annotation
		return new RawAddCombatantEvent(fields.getEntity(Fields.id, Fields.name));
//		return new PlayerChangeEvent(new XivEntity(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
