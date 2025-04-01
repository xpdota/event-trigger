package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.MapEffectEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line257Parser extends AbstractACTLineParser<Line257Parser.Fields> {

	public Line257Parser(PicoContainer container) {
		super(container, 257, Fields.class);
	}

	enum Fields {
		instanceContentId, flags, index, unknown1, unknown2
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		Long opt1 = fields.getOptionalHex(Fields.unknown1);
		Long opt2 = fields.getOptionalHex(Fields.unknown2);
		return new MapEffectEvent(
				fields.getHex(Fields.instanceContentId),
				fields.getHex(Fields.flags),
				fields.getHex(Fields.index),
				opt1 == null ? 0 : opt1,
				opt2 == null ? 0 : opt2);
	}
}
