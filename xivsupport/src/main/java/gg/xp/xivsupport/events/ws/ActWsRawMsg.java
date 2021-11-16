package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import org.intellij.lang.annotations.Language;

@SystemEvent
public class ActWsRawMsg extends BaseEvent {
	private static final long serialVersionUID = -7390177233308577948L;
	private final String rawMsgData;

	public ActWsRawMsg(@Language("JSON") String rawMsgData) {
		this.rawMsgData = rawMsgData;
	}

	public String getRawMsgData() {
		return rawMsgData;
	}
}
