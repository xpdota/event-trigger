package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.ActImportOnly;
import gg.xp.xivdata.data.GameLanguage;
import gg.xp.xivsupport.lang.GameLanguageInfoEvent;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Line249Parser extends AbstractACTLineParser<Line249Parser.Fields> implements ActImportOnly {

	private static final Logger log = LoggerFactory.getLogger(Line249Parser.class);

	public Line249Parser(PicoContainer container) {
		super(container, 249, Fields.class);
	}

	enum Fields {
		text
	}

	private static final Pattern langId = Pattern.compile("Selected Language ID: (\\d+), .*");
	private static final Pattern langName = Pattern.compile("Selected Language ID: ([A-Za-z]+), .*");

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		String rawText = fields.getString(Fields.text);
		Matcher langIdMatcher = langId.matcher(rawText);
		if (langIdMatcher.matches()) {
			int id = Integer.parseInt(langIdMatcher.group(1));
			if (id < 0 || id >= GameLanguage.values().length) {
				log.warn("Invalid language ID: {}", id);
				return null;
			}
			GameLanguage lang = GameLanguage.values()[id];
			return new GameLanguageInfoEvent(lang);
		}
		Matcher langNameMatcher = langName.matcher(rawText);
		if (langNameMatcher.matches()) {
			// Let this fail so that it logs the error
			GameLanguage lang = GameLanguage.valueOf(langNameMatcher.group(1));
			return new GameLanguageInfoEvent(lang);
		}
		return null;
	}
}
