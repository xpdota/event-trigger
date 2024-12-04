package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.RsvEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line262Parser extends AbstractACTLineParser<Line262Parser.Fields> {

	public Line262Parser(PicoContainer container) {
		super(container, 262, Fields.class);
	}

	enum Fields {
		locale, number, rsvKey, rsvValue
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {

		String localeStr = fields.getString(Fields.locale);
		GameLanguage lang = GameLanguage.valueOfShort(localeStr);

		String value = fields.getString(Fields.rsvValue);
		if (value.isBlank()) {
			return null;
		}
		return new RsvEvent(
				lang,
				fields.getString(Fields.rsvKey),
				value
		);
	}

}
