package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActionSyncEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.XivCombatant;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line37Parser extends AbstractACTLineParser<Line37Parser.Fields> {

	private final XivState state;

	public Line37Parser(XivState state) {
		super(37, Fields.class);
		this.state = state;
	}

	enum Fields {
		id, name, sequenceId, targetCurHp, targetMaxHp
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		XivCombatant target = fields.getEntity(Fields.id, Fields.name);
		long curHp = fields.getLong(Fields.targetCurHp);
		long maxHp = fields.getLong(Fields.targetMaxHp);

		// TODO: this should really just be moved into FieldMapper.getEntity
		state.provideCombatantHP(target, new HitPoints(curHp, maxHp));


		return new ActionSyncEvent(target, fields.getHex(Fields.sequenceId));
	}
}
