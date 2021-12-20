package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActionSyncEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line39Parser extends AbstractACTLineParser<Line39Parser.Fields> {

	public Line39Parser(XivState state) {
		super(state,  39, Fields.class);
	}

	enum Fields {
		id, name,
		targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetUnknown1, targetUnknown2, targetX, targetY, targetZ, targetHeading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		fields.setTrustedHp(true);
		fields.getEntity(Fields.id, Fields.name, Fields.targetCurHp, Fields.targetMaxHp, Fields.targetCurMp, Fields.targetMaxMp, Fields.targetX, Fields.targetY, Fields.targetZ, Fields.targetHeading);
		return null;
	}
}
