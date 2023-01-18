package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.RsvEvent;
import gg.xp.xivdata.data.GameLanguage;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line261Parser extends AbstractACTLineParser<Line261Parser.Fields> {

	public Line261Parser(PicoContainer container) {
		super(container, 261, Fields.class);
	}

	enum Fields {
		locale, number, rsvKey, rsvValue
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {

		String localeStr = fields.getString(Fields.locale);
		GameLanguage lang = GameLanguage.valueOf(localeStr);

		return new RsvEvent(
				lang,
				fields.getString(Fields.rsvKey),
				fields.getString(Fields.rsvValue)
		);
	}
}
