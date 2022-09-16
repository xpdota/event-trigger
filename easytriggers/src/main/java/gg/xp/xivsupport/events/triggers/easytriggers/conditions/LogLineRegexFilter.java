package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.gui.util.HasFriendlyName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogLineRegexFilter implements Condition<ACTLogLineEvent> {

	@SuppressWarnings("unused")
	public enum LogLineType implements HasFriendlyName {
		NETWORK("Network Line"),
		PARSED("Parsed Line");

		private final String description;

		LogLineType(String description) {
			this.description = description;
		}

		@Override
		public String getFriendlyName() {
			return description;
		}
	}

	public LogLineType lineType = LogLineType.NETWORK;
	@Description("Regex")
	public Pattern regex = Pattern.compile("^Regex Here$");
	@Description("Matcher Variable")
	public String matcherVar = "";

	@Override
	public String fixedLabel() {
		return "ACT Log Regex";
	}

	@Override
	public String dynamicLabel() {
		return lineType.getFriendlyName() + " matches regex '" + regex.pattern() + '\'';
	}

	@Override
	public boolean test(EasyTriggerContext context, ACTLogLineEvent event) {
		String line = lineType == LogLineType.NETWORK ? event.getLogLine() : event.getEmulatedActLogLine();
		Matcher matcher = regex.matcher(line);
		if (matcher.find()) {
			if (matcherVar != null && !matcherVar.isBlank()) {
				context.addVariable(matcherVar, matcher);
			}
			return true;
		}
		return false;
	}

}
