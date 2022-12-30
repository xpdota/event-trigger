package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.LogLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActLegacyTriggerImport {
	private static final Pattern parsePattern = Pattern.compile(" ([A-Za-z]+)=\"([^\"]*)\"");

	private ActLegacyTriggerImport() {
	}

	public static List<EasyTrigger<ACTLogLineEvent>> parseMultipleTriggerXml(String triggerXmlMultiLine) {
		return triggerXmlMultiLine.lines()
				.map(String::trim)
				.filter(s -> s.startsWith("<Trigger"))
				.map(ActLegacyTriggerImport::parseTriggerXml)
				.toList();
	}

	public static List<EasyTrigger<ACTLogLineEvent>> parseMultipleTriggerXmlNonEmpty(String triggerXmlMultiLine) {
		List<EasyTrigger<ACTLogLineEvent>> triggers = parseMultipleTriggerXml(triggerXmlMultiLine);
		if (triggers.isEmpty()) {
			throw new AssertionError("No valid triggers found in the input text");
		}
		return triggers;
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

		// There's also a "long form" of these, looks like this:
		// <Trigger Active="True" Regex="(?#-- Thermionic Beam -&gt; Lunar Dynamo : Stack &amp; In --)Take fire, O hallowed moon!" SoundData="Stack &amp; In" SoundType="3" CategoryRestrict="False" Category="Nael RP Quotes" Timer="False" TimerName="" Tabbed="False" />

		Matcher matcher = parsePattern.matcher(triggerXml);

		Map<String, String> map = new HashMap<>(8);

		while (matcher.find()) {
			String key = matcher.group(1);
			String value = StringEscapeUtils.unescapeHtml4(matcher.group(2));
			map.put(key, value);
		}

		String regex = map.get("R");
		String output = map.get("SD");

		if (regex == null) {
			regex = map.get("Regex");
		}
		if (output == null) {
			output = map.get("SoundData");
		}
		if (regex == null || output == null) {
			throw new IllegalArgumentException("Did not have required fields: " + triggerXml);
		}

		// Now, we need to convert capture groups.
		// e.g. ${time} -> match.group('time')
		// https://github.com/xpdota/event-trigger/issues/123
		// Replace named capture groups
		output = output.replaceAll("\\$\\{([^\\d}'][^}']*)}", "{match.group('$1')}");
		// Replace indexed capture groups
		output = output.replaceAll("\\$\\{(\\d+)}", "{match.group($1)}");
		output = output.replaceAll("\\$(\\d+)", "{match.group($1)}");

		// Java doesn't support PCRE regex comments, so strip them.
		// They look like this: (?#-- Comment goes here --)
		regex = regex.replaceAll("\\(\\?#--.*?--\\)", "");

		EasyTrigger<ACTLogLineEvent> trigger = new EasyTrigger<>();
		trigger.setEventType(ACTLogLineEvent.class);
		trigger.setName(regex);
		CalloutAction call = new CalloutAction();
		call.setText(output);
		call.setTts(output);
		trigger.addAction(call);
		LogLineRegexFilter cond = new LogLineRegexFilter();
		cond.regex = Pattern.compile(regex);
		cond.lineType = LogLineRegexFilter.LogLineType.PARSED;
		cond.matcherVar = "match";

		trigger.addCondition(cond);
		trigger.recalc();

		return trigger;
	}

}
