package gg.xp.events.actlines;

import gg.xp.events.Event;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivStatusEffect;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line30Parser extends AbstractACTLineParser<Line30Parser.Fields> {

	public Line30Parser() {
		super(30, Fields.class);
	}

	enum Fields {
		buffId, buffName, unknownFieldMaybeDuration, sourceId, sourceName, targetId, targetName, buffStacks
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new BuffRemoved(
				new XivStatusEffect(fields.getHex(Fields.buffId), fields.getString(Fields.buffName)),
				fields.getDouble(Fields.unknownFieldMaybeDuration),
				new XivEntity(fields.getHex(Fields.sourceId), fields.getString(Fields.sourceName)),
				new XivEntity(fields.getHex(Fields.targetId), fields.getString(Fields.targetName)),
				fields.getHex(Fields.buffStacks)
		);
	}
}
