package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.debug.DebugCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TtsProcessor {

	private static final Logger log = LoggerFactory.getLogger(TtsProcessor.class);


	@HandleEvents
	public void callout(EventContext context, CalloutEvent callout) {
		String callText = callout.getCallText();
		if (callText != null && !callText.isBlank()) {
			log.info("TTS: '{}'", callText);
			context.accept(new TtsRequest(callText));
		}
	}

	@HandleEvents
	public void ttsDebugCommand(EventContext context, DebugCommand echo) {
		if (echo.getCommand().equals("tts")) {
			context.accept(new BasicCalloutEvent("test"));
		}
	}

	// TODO: if someone legitimately wants to disable TTS entirely, this would be spammy
//	@HandleEvents(order = Integer.MAX_VALUE)
//	public void ttsNotHandledWarning(EventContext context, TtsRequest tts) {
//		if (!tts.isHandled()) {
//			log.warn("Unhandled TTS! '{}'", tts);
//		}
//	}
}
