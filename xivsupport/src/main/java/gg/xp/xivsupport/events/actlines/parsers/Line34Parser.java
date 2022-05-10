package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActionSyncEvent;
import gg.xp.xivsupport.events.actlines.events.TargetabilityUpdate;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line34Parser extends AbstractACTLineParser<Line34Parser.Fields> {

	public Line34Parser(PicoContainer container) {
		super(container,  34, Fields.class);
	}

	enum Fields {
		sourceId, sourceName,
		targetId, targetName,
		toggle
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new TargetabilityUpdate(
				fields.getEntity(Fields.sourceId, Fields.sourceName),
				fields.getEntity(Fields.targetId, Fields.targetName),
				fields.getHex(Fields.toggle) == 1);
	}
}
