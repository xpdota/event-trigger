package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.ActImportOnly;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.XivZone;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line01Parser extends AbstractACTLineParser<Line01Parser.Fields> implements ActImportOnly {

	public Line01Parser(XivStateImpl state) {
		super(state, 1, Fields.class);
	}

	enum Fields {
		id, name
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new ZoneChangeEvent(new XivZone(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
