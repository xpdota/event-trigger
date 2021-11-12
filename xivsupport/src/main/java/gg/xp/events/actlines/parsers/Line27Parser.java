package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.HeadMarkerEvent;
import gg.xp.events.models.XivEntity;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line27Parser extends AbstractACTLineParser<Line27Parser.Fields> {

	public Line27Parser() {
		super(27, Fields.class);
	}

	enum Fields {
		targetId, targetName, unknown1, unknown2, markerId
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: handle obfuscated headmarks - I believe these are zone-specific
		return new HeadMarkerEvent(
				fields.getEntity(Fields.targetId, Fields.targetName),
				fields.getHex(Fields.markerId)
		);
	}
}
