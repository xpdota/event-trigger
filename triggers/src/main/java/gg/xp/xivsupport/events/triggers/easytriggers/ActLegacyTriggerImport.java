package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.LogLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActLegacyTriggerImport {
	private static final Pattern parsePattern = Pattern.compile(" ([A-Za-z]{1,2})=\"([^\"]*)\"");

	public static List<EasyTrigger<ACTLogLineEvent>> parseMultipleTriggerXml(String triggerXmlMultiLine) {
		return triggerXmlMultiLine.lines().filter(s -> !s.isBlank())
				.map(ActLegacyTriggerImport::parseTriggerXml)
				.toList();
	}

	public static EasyTrigger<ACTLogLineEvent> parseTriggerXml(String triggerXml) {
		// Here's the fields
		// <Trigger R="asdf" SD="qwer" ST="3" CR="F" C="zxcv" T="F" TN="tyui" Ta="F" />
		// R: name/regex
		// SD: output (WAV/TTS)
		// ST: 0 for none, 1 for beep, 2 for WAV, 3 for TTS
		// C: category
		// T: Timer
		// TN: timer/tab name
		// Ta: Add Results Tab
		// <Trigger R="asdf" SD="qwer" ST="2" CR="F" C="zxcv" T="F" TN="tyui" Ta="T" />
		Matcher matcher = parsePattern.matcher(triggerXml);

		Map<String, String> map = new HashMap<>(8);

		while (matcher.find()) {
			String key = matcher.group(1);
			String value = StringEscapeUtils.unescapeHtml4(matcher.group(2));
			map.put(key, value);
		}

		String regex = map.get("R");
		String output = map.get("SD");

		if (regex == null || output == null) {
			throw new IllegalArgumentException("Did not have required fields: " + triggerXml);
		}

		EasyTrigger<ACTLogLineEvent> trigger = new EasyTrigger<>();
		trigger.setEventType(ACTLogLineEvent.class);
		trigger.setName("Imported Legacy ACT Trigger");
		trigger.setText(output);
		trigger.setTts(output);
		LogLineRegexFilter cond = new LogLineRegexFilter();
		cond.regex = Pattern.compile(regex);
		cond.lineType = LogLineRegexFilter.LogLineType.PARSED;

		trigger.addCondition(cond);

		return trigger;
	}

}
