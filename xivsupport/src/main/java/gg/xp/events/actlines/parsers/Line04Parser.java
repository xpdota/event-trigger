package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.RawRemoveCombatantEvent;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line04Parser extends AbstractACTLineParser<Line04Parser.Fields> {

	public Line04Parser() {
		super(4, Fields.class);
	}

	enum Fields {
		id, name, job
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: some kind of @Disable annotation
		return new RawRemoveCombatantEvent(fields.getEntity(Fields.id, Fields.name));
//		return new PlayerChangeEvent(new XivEntity(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}

	@Override
	protected boolean shouldIgnoreEntityLookupMisses() {
		return true;
	}
}
