package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.combatstate.CountdownStartedEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;

import java.time.Duration;
import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line268Parser extends AbstractACTLineParser<Line268Parser.Fields> {


	public Line268Parser(PicoContainer container) {
		super(container, 268, Fields.class);
	}

	enum Fields {
		entityId, senderWorldId, durationSeconds, result, entityName
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// 0 result indicates that the countdown was started successfully
		// non-zero indicates that it failed (e.g. due to being in combat already)
		if (fields.getHex(Fields.result) == 0) {
			return null;
		}
		XivCombatant entity = fields.getEntity(Fields.entityId, Fields.entityName);
		return new CountdownStartedEvent(
				Duration.ofSeconds(fields.getInt(Fields.durationSeconds)),
				entity
		);
	}

}
