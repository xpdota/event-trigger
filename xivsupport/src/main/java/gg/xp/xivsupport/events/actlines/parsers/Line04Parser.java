package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;
import gg.xp.xivsupport.events.state.XivStateImpl;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line04Parser extends AbstractACTLineParser<Line04Parser.Fields> {

	public Line04Parser(org.picocontainer.PicoContainer container) {
		super(container,  4, Fields.class);
	}

	enum Fields {
		id, name, job
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new RawRemoveCombatantEvent(fields.getEntity(Fields.id, Fields.name));
	}

	@Override
	protected EntityLookupMissBehavior entityLookupMissBehavior() {
		return EntityLookupMissBehavior.IGNORE;
	}
}
