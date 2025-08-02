package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: chat source?
public class ChatLineRegexFilter implements Condition<ChatLineEvent> {

	@Description("Regex")
	public Pattern regex = Pattern.compile("^Regex Here$");
	@Description("Matcher Variable")
	public String matcherVar = "match";


	@Override
	public String fixedLabel() {
		return "Chat Log Regex";
	}

	@Override
	public String dynamicLabel() {
		return "Chat Line Matching " + regex.pattern();
	}

	@Override
	public Class<ChatLineEvent> getEventType() {
		return ChatLineEvent.class;
	}

	@Override
	public boolean test(EasyTriggerContext context, ChatLineEvent event) {
		Matcher matcher = regex.matcher(event.getLine());
		if (matcher.find()) {
			if (matcherVar != null && !matcherVar.isBlank()) {
				context.addVariable(matcherVar, matcher);
			}
			return true;
		}
		return false;
	}

}
