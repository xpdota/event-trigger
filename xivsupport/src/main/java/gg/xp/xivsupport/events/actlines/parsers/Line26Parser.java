package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line26Parser extends AbstractACTLineParser<Line26Parser.Fields> {

	public Line26Parser(PicoContainer container) {
		super(container, 26, Fields.class);
	}

	enum Fields {
		buffId, buffName, duration, sourceId, sourceName, targetId, targetName, buffStacks
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new BuffApplied(
				fields.getStatus(Fields.buffId, Fields.buffName),
				fields.getDouble(Fields.duration),
				fields.getEntity(Fields.sourceId, Fields.sourceName),
				fields.getEntity(Fields.targetId, Fields.targetName),
				fields.getHex(Fields.buffStacks)
		);
	}
}
