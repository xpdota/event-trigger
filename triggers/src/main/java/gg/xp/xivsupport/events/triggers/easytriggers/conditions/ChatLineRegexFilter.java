package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;

import java.util.regex.Pattern;

// TODO: chat source?
public class ChatLineRegexFilter implements Condition<ChatLineEvent> {

	@Description("Regex")
	public Pattern regex = Pattern.compile("^Regex Here$");

	@Override
	public String fixedLabel() {
		return "Chat Log Regex";
	}

	@Override
	public String dynamicLabel() {
		return +' ' + regex.pattern();
	}

	@Override
	public boolean test(ChatLineEvent event) {
		return regex.matcher(event.getLine()).find();
	}

}
