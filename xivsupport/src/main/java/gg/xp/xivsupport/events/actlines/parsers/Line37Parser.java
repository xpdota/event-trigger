package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActionSyncEvent;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line37Parser extends AbstractACTLineParser<Line37Parser.Fields> {

	public Line37Parser(XivStateImpl state) {
		super(state,  37, Fields.class);
	}

	enum Fields {
		id, name, sequenceId,
		targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetUnknown1, targetUnknown2, targetX, targetY, targetZ, targetHeading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		fields.setTrustedHp(true);
		XivCombatant target = fields.getEntity(Fields.id, Fields.name, Fields.targetCurHp, Fields.targetMaxHp, Fields.targetCurMp, Fields.targetMaxMp, Fields.targetX, Fields.targetY, Fields.targetZ, Fields.targetHeading);
		return new ActionSyncEvent(target, fields.getHex(Fields.sequenceId));
	}
}
