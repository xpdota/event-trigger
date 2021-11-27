package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;

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
	}

	@Override
	protected EntityLookupMissBehavior entityLookupMissBehavior() {
		return EntityLookupMissBehavior.GET;
	}
}
