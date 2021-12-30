package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.XivStateImpl;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line38Parser extends AbstractACTLineParser<Line38Parser.Fields> {

	public Line38Parser(org.picocontainer.PicoContainer container) {
		super(container,  38, Fields.class);
	}

	enum Fields {
		id, name, jobLevelData,
		targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetUnknown1, targetUnknown2, targetX, targetY, targetZ, targetHeading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		fields.setTrustedHp(true);
		fields.getEntity(Fields.id, Fields.name, Fields.targetCurHp, Fields.targetMaxHp, Fields.targetCurMp, Fields.targetMaxMp, Fields.targetX, Fields.targetY, Fields.targetZ, Fields.targetHeading);
		return null;
	}
}
