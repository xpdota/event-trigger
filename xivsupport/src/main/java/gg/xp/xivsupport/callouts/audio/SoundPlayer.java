package gg.xp.xivsupport.callouts.audio;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundPlayer {

	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private static final Logger log = LoggerFactory.getLogger(SoundPlayer.class);

	private final SoundFilesManager mgr;

	public SoundPlayer(SoundFilesManager mgr) {
		this.mgr = mgr;
	}

	@HandleEvents
	public void getSoundFromCallout(EventContext context, CalloutEvent call) {
		String sound = call.getSound();
		SoundFile sf = mgr.getSoundFileByName(sound);
		if (sf != null) {
			context.accept(new PlaySoundFileRequest(sf.file.toFile()));
		}
	}

	@HandleEvents
	public void playSound(EventContext context, PlaySoundFileRequest event) {
		File file = event.getFile();
		exs.submit(() -> playSound(file));
	}

	private void playSound(File file) {
		try {
			AudioInputStream as = AudioSystem.getAudioInputStream(file);
			Clip clip = AudioSystem.getClip();
			clip.open(as);
			CountDownLatch latch = new CountDownLatch(1);
			clip.addLineListener(le -> {
				if (le.getType() == LineEvent.Type.STOP) {
					latch.countDown();
				}
			});
			clip.start();
			latch.await();
		}
		catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
			log.error("Error playing sound file", e);
			throw new RuntimeException(e);
		}
	}

}
