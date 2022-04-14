package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line20Parser extends AbstractACTLineParser<Line20Parser.Fields> {

	public Line20Parser(PicoContainer container) {
		super(container,  20, Fields.class);
	}

	enum Fields {
		casterId, casterName, abilityId, abilityName, targetId, targetName, castTime, casterX, casterY, casterZ, casterHeading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new AbilityCastStart(
				fields.getAbility(Fields.abilityId, Fields.abilityName),
				fields.getEntity(Fields.casterId, Fields.casterName, null, null, null, null, Fields.casterX, Fields.casterY, Fields.casterZ, Fields.casterHeading),
				fields.getEntity(Fields.targetId, Fields.targetName),
				fields.getDouble(Fields.castTime)
		);
	}
}
