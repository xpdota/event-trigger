package gg.xp.speech;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventHandler;
import gg.xp.events.misc.EchoEvent;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeechProcessor {

	private static final Logger log = LoggerFactory.getLogger(SpeechProcessor.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r);
		thread.setName("SpeechThread");
		thread.setDaemon(true);
		return thread;
	});
	// There's no way this is the best option, but the projects I found for Java -> MS Speech are all either
	// ancient, or are using the Azure API (not the local one) which requires API keys and all that.

	// Quick test util

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
			log.warn("If this does not appear to be malicious, consuder improving your input");
		}
		log.info("Saying text: {}", text);
		try {
			Process process = Runtime.getRuntime().exec("powershell.exe Add-Type -AssemblyName System.speech; " +
					"$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
					"$speak.Speak('" + text + "')");
			int exit = process.waitFor();
			String out = new String(process.getInputStream().readAllBytes());
			String err = new String(process.getErrorStream().readAllBytes());
			log.info("exit: {}; out: {}; err: {}", exit, out, err);
		}
		catch (Throwable t) {
			log.error("Speech error", t);
		}
	}

	@HandleEvents
	public void handle(EventContext<Event> context, TtsCall event) {
		// Events are not processed nor distributed in parallel - thus we need to make sure that we use async operations
		// for things that take non-trivial amounts of time (e.g. speech)
		sayAsync(event.getCallText());
	}

	@HandleEvents
	public void handle(EventContext<Event> context, EchoEvent echo) {
		if (echo.getLine().equals("tts")) {
			context.accept(new TtsCall("test"));
		}
	}
}
