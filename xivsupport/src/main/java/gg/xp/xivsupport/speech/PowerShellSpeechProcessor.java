package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PowerShellSpeechProcessor implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(PowerShellSpeechProcessor.class);

	private static final ExecutorService exs = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r);
		thread.setName("SpeechThread");
		thread.setDaemon(true);
		return thread;
	});
//	 There's no way this is the best option, but the projects I found for Java -> MS Speech are all either
//	 ancient, or are using the Azure API (not the local one) which requires API keys and all that.
//	 Never mind, can probably just use ACT websocket for TTS.
//
//	 Quick test util

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		// Test sanitization
		sayAsync("Foo '); Bar; ('");

		sayAsync("The quick brown fox");
		sayAsync("jumped over the lazy dog");
		// These are daemon threads, so it would exit before the speech is able to finish
		// Instead, submit a dummy task to wait for the rest to finish.
		exs.submit(() -> {
		}).get();
	}

	public static void sayAsync(String text) {
		exs.submit(() -> saySync(text));
	}

	public static void saySync(String textUnsanitized) {
		// TODO: make sure this is enough sanitization.
		String text = textUnsanitized.replaceAll("['\";\0]", "");
		if (!text.equals(textUnsanitized)) {
			log.warn("Had to sanitize text: Old {'{}'}, New {'{}'}", textUnsanitized, text);
			log.warn("If this does not appear to be malicious, consider improving your input");
		}
		log.info("Saying text: {}", text);
		try {
			// TODO: replace this with something that's not terrible. Even just holding the PS open and communicating
			// with pipes would probably make it a little more responsive.
			Process process = Runtime.getRuntime().exec("powershell.exe Add-Type -AssemblyName System.speech; " +
					"$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
					"$speak.Speak('" + text + "')");
			int exit = process.waitFor();
			if (exit == 0) {
				return;
			}
			String out = new String(process.getInputStream().readAllBytes());
			String err = new String(process.getErrorStream().readAllBytes());
			log.warn("exit: {}; out: {}; err: {}", exit, out, err);
		}
		catch (Throwable t) {
			log.error("Speech error", t);
		}
	}

	private final BooleanSetting enable;
	public PowerShellSpeechProcessor(PersistenceProvider pers) {
		this.enable = new BooleanSetting(pers, "fallback-local-tts.enable-tts", false);
	}

	// This is a fallback option
	@HandleEvents(order = 1_000_000)
	public void handle(EventContext context, TtsRequest event) {
		// Events are not processed nor distributed in parallel - thus we need to make sure that we use async operations
		// for things that take non-trivial amounts of time (e.g. speech)
		String text = event.getTtsString();
		if (text != null && !event.isHandled()) {
			sayAsync(text);
			event.setHandled();
		}
	}

	@Override
	public boolean enabled(EventContext context) {
		return enable.get();
	}

	public BooleanSetting getEnabledSetting() {
		return enable;
	}
}
