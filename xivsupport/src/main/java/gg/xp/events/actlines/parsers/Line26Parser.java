package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.BuffApplied;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivStatusEffect;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line26Parser extends AbstractACTLineParser<Line26Parser.Fields> {

	public Line26Parser() {
		super(26, Fields.class);
	}

	enum Fields {
		buffId, buffName, duration, sourceId, sourceName, targetId, targetName, buffStacks
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new BuffApplied(
				new XivStatusEffect(fields.getHex(Fields.buffId), fields.getString(Fields.buffName)),
				fields.getDouble(Fields.duration),
				new XivEntity(fields.getHex(Fields.sourceId), fields.getString(Fields.sourceName)),
				new XivEntity(fields.getHex(Fields.targetId), fields.getString(Fields.targetName)),
				fields.getHex(Fields.buffStacks)
		);
	}
}
