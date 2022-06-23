package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line25Parser extends AbstractACTLineParser<Line25Parser.Fields> {

	public Line25Parser(PicoContainer container) {
		super(container,  25, Fields.class);
	}

	enum Fields {
		targetId, targetName, sourceId, sourceName
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new EntityKilledEvent(
				fields.getEntity(Fields.sourceId, Fields.sourceName),
				fields.getEntity(Fields.targetId, Fields.targetName)
		);
	}
}
