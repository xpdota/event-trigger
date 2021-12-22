package gg.xp.xivsupport.events.debug;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DebugCommand extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -6938761273983139597L;
	private final String rawString;
	private final List<String> split;

	public DebugCommand(String rawString) {
		this.rawString = rawString;
		// TODO: needs some form of escaping and/or quoting
		this.split = Arrays.asList(rawString.split(" "));
	}

	public String getRawString() {
		return rawString;
	}

	public String getCommand() {
		return split.get(0);
	}

	public List<String> getArgs() {
		return Collections.unmodifiableList(split);
	}


}
