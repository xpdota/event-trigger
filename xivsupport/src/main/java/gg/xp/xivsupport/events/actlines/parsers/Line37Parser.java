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
		Long curHp = fields.getOptionalLong(Fields.targetCurHp);
		if (curHp != null) {
			// Plan A - both the current and max fields are present
			Long maxHp = fields.getOptionalLong(Fields.targetMaxHp);
			if (maxHp != null) {
				state.provideCombatantHP(target, new HitPoints(curHp, maxHp));
			}
			// Plan B - we only have current available, so use stored max and assume it's the same (since max HP changes
			// are not that common).
			else {
				if (target.getHp() != null) {
					state.provideCombatantHP(target, new HitPoints(curHp, target.getHp().getMax()));
				}
			}
		}

		return new ActionSyncEvent(target, fields.getHex(Fields.sequenceId));
	}
}
