package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.EntityKilledEvent;
import gg.xp.events.models.XivEntity;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line25Parser extends AbstractACTLineParser<Line25Parser.Fields> {

	public Line25Parser() {
		super(25, Fields.class);
	}

	enum Fields {
		targetId, targetName, sourceId, sourceName
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new EntityKilledEvent(
				new XivEntity(fields.getHex(Fields.sourceId), fields.getString(Fields.sourceName)),
				new XivEntity(fields.getHex(Fields.targetId), fields.getString(Fields.targetName))
		);
	}
}
