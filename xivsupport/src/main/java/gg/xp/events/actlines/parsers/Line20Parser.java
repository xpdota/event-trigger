package gg.xp.events.actlines.parsers;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.AbilityCastStart;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivEntity;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line20Parser extends AbstractACTLineParser<Line20Parser.Fields> {

	public Line20Parser() {
		super(20, Fields.class);
	}

	enum Fields {
		casterId, casterName, abilityId, abilityName, targetId, targetName, castTime;
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new AbilityCastStart(
				new XivAbility(fields.getHex(Fields.abilityId), fields.getString(Fields.abilityName)),
				new XivEntity(fields.getHex(Fields.casterId), fields.getString(Fields.casterName)),
				new XivEntity(fields.getHex(Fields.targetId), fields.getString(Fields.targetName)),
				fields.getDouble(Fields.castTime)
		);
	}
}
