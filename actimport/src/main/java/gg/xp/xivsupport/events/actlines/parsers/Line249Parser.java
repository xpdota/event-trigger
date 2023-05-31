package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.ActImportOnly;
import gg.xp.xivdata.data.GameLanguage;
import gg.xp.xivsupport.lang.GameLanguageInfoEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Line249Parser extends AbstractACTLineParser<Line249Parser.Fields> implements ActImportOnly {

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
			GameLanguage lang = GameLanguage.values()[Integer.parseInt(langIdMatcher.group(1))];
			return new GameLanguageInfoEvent(lang);
		}
		Matcher langNameMatcher = langName.matcher(rawText);
		if (langNameMatcher.matches()) {
			GameLanguage lang = GameLanguage.valueOf(langNameMatcher.group(1));
			return new GameLanguageInfoEvent(lang);
		}
		return null;
	}
}
