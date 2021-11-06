package gg.xp.events.actlines;

import gg.xp.events.AbilityUsedEvent;
import gg.xp.events.Event;
import gg.xp.events.XivAbility;
import gg.xp.events.XivEntity;

import java.time.ZonedDateTime;
import java.util.Map;

@SuppressWarnings("unused")
public class Line21Parser extends AbstractACTLineParser<Line21Parser.Fields> {

	public Line21Parser() {
		super(21, Fields.class);
	}

	enum Fields {
		casterId, casterName, abilityId, abilityName, targetId, targetName;
	}

	@Override
	protected Event convert(Map<Fields, String> fields, int lineNumber, ZonedDateTime time) {
		return new AbilityUsedEvent(
				new XivAbility(Long.parseLong(fields.get(Fields.abilityId), 16), fields.get(Fields.abilityName)),
				new XivEntity(Long.parseLong(fields.get(Fields.casterId), 16), fields.get(Fields.casterName)),
				new XivEntity(Long.parseLong(fields.get(Fields.targetId), 16), fields.get(Fields.targetName))
		);
	}
}
