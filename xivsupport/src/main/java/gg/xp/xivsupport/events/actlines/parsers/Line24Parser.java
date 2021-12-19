package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line24Parser extends AbstractACTLineParser<Line24Parser.Fields> {

	public Line24Parser(XivState state) {
		super(state, 24, Fields.class);
	}

	enum Fields {
		targetId, targetName,
		dotOrHot,
		effectId,
		damage,
		targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetUnknown1, targetUnknown2, targetX, targetY, targetZ, targetHeading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		XivCombatant target = fields.getEntity(Fields.targetId, Fields.targetName, Fields.targetCurHp, Fields.targetMaxHp, Fields.targetCurMp, Fields.targetMaxMp, Fields.targetX, Fields.targetY, Fields.targetZ, Fields.targetHeading);
		// Not sure what to do yet, atm only care about the combatant update, but this *could* be useful for server tickers
		return null;
	}
}
