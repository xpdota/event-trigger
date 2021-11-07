package gg.xp.events.ws;

import gg.xp.events.BaseEvent;
import org.intellij.lang.annotations.Language;

public class ActWsRawMsg extends BaseEvent {
	private final String rawMsgData;

	public ActWsRawMsg(@Language("JSON") String rawMsgData) {
		this.rawMsgData = rawMsgData;
	}

	public String getRawMsgData() {
		return rawMsgData;
	}
}
