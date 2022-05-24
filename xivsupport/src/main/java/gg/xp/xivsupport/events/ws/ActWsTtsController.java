package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

public class ActWsTtsController implements FilteredEventHandler {
	private final ActWsLogSource ws;
	private final BooleanSetting allowTts;
	private final PrimaryLogSource pls;

	public ActWsTtsController(ActWsLogSource ws, PersistenceProvider pers, PrimaryLogSource pls) {
		this.ws = ws;
		this.allowTts = new BooleanSetting(pers, "actws-allow-tts", true);
		this.pls = pls;
	}

	// A queue for outgoing TTS, and whether there is an outstanding request
	private final Queue<TtsRequest> ttsQueue = new ArrayDeque<>();
	private boolean pendingTts;
	// Also cap to 5 seconds just in case something got lost
	private long lastRequestAt;

	@HandleEvents
	public void sayTts(EventContext context, TtsRequest event) {
		if (allowTts.get()) {
			String ttsString = event.getTtsString();
			if (ttsString == null || ttsString.isBlank()) {
				return;
			}
			ttsQueue.add(event);
			processTts();
		}
	}

	@HandleEvents
	public void incoming(EventContext context, ActWsJsonMsg msg) {
		if ("tts".equals(msg.getRseq())) {
			pendingTts = false;
		}
		processTts();
	}

	private void processTts() {
		if (ttsQueue.isEmpty()) {
			return;
		}
		if (pendingTts) {
			if ((lastRequestAt + 5000) < System.currentTimeMillis()) {
				pendingTts = false;
			}
			else {
				return;
			}
		}
		TtsRequest event = ttsQueue.poll();
		pendingTts = true;
		lastRequestAt = System.currentTimeMillis();
		ws.sendMessage(Map.ofEntries(
				Map.entry("call", "cactbotSay"),
				Map.entry("text", event.getTtsString()),
				Map.entry("rseq", "tts")));
	}

	public BooleanSetting getAllowTts() {
		return allowTts;
	}

	@Override
	public boolean enabled(EventContext context) {
		return pls.getLogSource() == KnownLogSource.WEBSOCKET_LIVE;
	}
}
