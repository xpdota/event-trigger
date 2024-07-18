package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.ActorControlExtraEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlSelfExtraEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line274Parser extends AbstractACTLineParser<Line274Parser.Fields> {

	public Line274Parser(PicoContainer container) {
		super(container, 274, Fields.class);
	}

	enum Fields {
		entityId, category, data0, data1, data2, data3, data4, data5
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new ActorControlSelfExtraEvent(
				fields.getEntity(Fields.entityId),
				(int) fields.getHex(Fields.category),
				fields.getHex(Fields.data0),
				fields.getHex(Fields.data1),
				fields.getHex(Fields.data2),
				fields.getHex(Fields.data3),
				fields.getHex(Fields.data4),
				fields.getHex(Fields.data5)
		);
	}

}
