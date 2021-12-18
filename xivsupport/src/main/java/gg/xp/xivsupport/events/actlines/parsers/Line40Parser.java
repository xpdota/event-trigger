package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.jobs.XivMap;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivZone;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line40Parser extends AbstractACTLineParser<Line40Parser.Fields> {

	public Line40Parser(XivState state) {
		super(state, 40, Fields.class);
	}

	enum Fields {
		id
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new MapChangeEvent(XivMap.forId(fields.getLong(Fields.id)));
	}
}
