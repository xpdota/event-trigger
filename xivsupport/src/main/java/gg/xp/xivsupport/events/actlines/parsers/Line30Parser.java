package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line30Parser extends AbstractACTLineParser<Line30Parser.Fields> {

	public Line30Parser(PicoContainer container) {
		super(container,  30, Fields.class);
	}

	enum Fields {
		buffId, buffName, unknownFieldMaybeDuration, sourceId, sourceName, targetId, targetName, buffStacks
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new BuffRemoved(
				fields.getStatus(Fields.buffId, Fields.buffName),
				fields.getDouble(Fields.unknownFieldMaybeDuration),
				fields.getEntity(Fields.sourceId, Fields.sourceName),
				fields.getEntity(Fields.targetId, Fields.targetName),
				fields.getHex(Fields.buffStacks)
		);
	}
}
