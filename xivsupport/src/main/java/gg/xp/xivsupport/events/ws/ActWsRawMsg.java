package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.misc.Compressor;
import org.intellij.lang.annotations.Language;

import java.time.Instant;

@SystemEvent
public class ActWsRawMsg extends BaseEvent {
	private static final long serialVersionUID = -7390177233308577948L;
	private String rawMsgData;
	private byte[] compressed;

	public ActWsRawMsg(@Language("JSON") String rawMsgData) {
		this.rawMsgData = rawMsgData;
	}

	public String getRawMsgData() {
		String raw = this.rawMsgData;
		if (raw != null) {
			return raw;
		}
		else {
			return Compressor.uncompressBytesToString(compressed);
		}
	}

	@Override
	public void setPumpFinishedAt(Instant pumpedAt) {
		super.setPumpFinishedAt(pumpedAt);
		byte[] compressed = Compressor.compressStringToBytes(rawMsgData);
		// Only compress if it actually saves memory
		if (compressed.length < rawMsgData.length()) {
			this.compressed = compressed;
			rawMsgData = null;
		}
	}
}
