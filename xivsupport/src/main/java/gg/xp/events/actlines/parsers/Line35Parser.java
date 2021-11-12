package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.TetherEvent;
import gg.xp.events.models.XivEntity;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line35Parser extends AbstractACTLineParser<Line35Parser.Fields> {

	public Line35Parser() {
		super(35, Fields.class);
	}

	enum Fields {
		sourceId, sourceName, targetId, targetName, unknown1, unknown2, markerId
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: handle obfuscated headmarks - I believe these are zone-specific
		return new TetherEvent(
				fields.getEntity(Fields.sourceId, Fields.sourceName),
				fields.getEntity(Fields.targetId, Fields.targetName),
				fields.getHex(Fields.markerId)
		);
	}
}
