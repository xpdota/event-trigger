package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.RawPlayerChangeEvent;
import gg.xp.xivsupport.models.XivEntity;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line02Parser extends AbstractACTLineParser<Line02Parser.Fields> {

	public Line02Parser() {
		super(2, Fields.class);
	}

	enum Fields {
		id, name
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new RawPlayerChangeEvent(new XivEntity(fields.getHex(Fields.id), fields.getString(Fields.name)));
	}
}
