package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.LimitBreakGaugeEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line36Parser extends AbstractACTLineParser<Line36Parser.Fields> {

	public Line36Parser(PicoContainer container) {
		super(container, 36, Fields.class);
	}

	enum Fields {
		value, barCount
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new LimitBreakGaugeEvent(fields.getInt(Fields.barCount), (int) fields.getHex(Fields.value));
	}
}
