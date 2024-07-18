package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line273Parser extends AbstractACTLineParser<Line273Parser.Fields> {

	public Line273Parser(PicoContainer container) {
		super(container, 273, Fields.class);
	}

	enum Fields {
		entityId, category, data0, data1, data2, data3
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new ActorControlExtraEvent(
				fields.getEntity(Fields.entityId),
				(int) fields.getHex(Fields.category),
				fields.getHex(Fields.data0),
				fields.getHex(Fields.data1),
				fields.getHex(Fields.data2),
				fields.getHex(Fields.data3));
	}

}
