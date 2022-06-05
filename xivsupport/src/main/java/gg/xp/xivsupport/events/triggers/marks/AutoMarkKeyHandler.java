package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkSlotRequest;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoMarkKeyHandler implements FilteredEventHandler {

	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private static final Logger log = LoggerFactory.getLogger(AutoMarkKeyHandler.class);

	private final BooleanSetting useFkeys;
	private final BooleanSetting useTelesto;

	public AutoMarkKeyHandler(PersistenceProvider pers, AutoMarkHandler handler) {
		useFkeys = new BooleanSetting(pers, "auto-marks.use-fkeys", false);
		useTelesto = handler.getUseTelesto();
	}

	public BooleanSetting getUseFkeys() {
		return useFkeys;
	}

	@Override
	public boolean enabled(EventContext context) {
		return !useTelesto.get();
	}

	@HandleEvents
	public void doMark(EventContext context, AutoMarkSlotRequest event) {
		doAutoMarkForSlot(context, event.getSlotToMark());
	}

	@HandleEvents
//	@LiveOnly
	public void clearMarks(EventContext context, ClearAutoMarkRequest event) {
		log.info("Clearing marks");
		clearAutoMark(context);
	}

	@HandleEvents
	public void doSpecificAutoMark(EventContext context, SpecificAutoMarkSlotRequest event) {
		MarkerSign marker = event.getMarker();
		if (marker == MarkerSign.ATTACK_NEXT) {
			context.accept(new AutoMarkSlotRequest(event.getSlotToMark()));
		}
		else if (marker.getBase() == MarkerSign.ATTACK_NEXT) {
			log.warn("Trying to turn marker {} into generic 'attack' marker - number may be wrong!", marker);
			context.accept(new AutoMarkSlotRequest(event.getSlotToMark()));
		}
		else {
			log.error("Marker unsupported by keyboard-based automarkers - {}", marker);
		}
	}


	public static final class KeyPressRequest extends BaseEvent implements HasPrimaryValue {
		@Serial
		private static final long serialVersionUID = -3520916842042620376L;
		private final int keyCode;

		// Leaving this private for now - need a way to prevent abuse
		private KeyPressRequest(int keyCode) {
			this.keyCode = keyCode;
		}

		public int getKeyCode() {
			return keyCode;
		}

		@Override
		public String getPrimaryValue() {
			return KeyEvent.getKeyText(keyCode);
		}
	}

	// i = 1-8
	private int keycodeForSlot(int i) {
		if (useFkeys.get()) {
			return KeyEvent.VK_F1 - 1 + i;
		}
		else {
			return KeyEvent.VK_NUMPAD1 - 1 + i;
		}
	}

	private int keycodeForClear() {
		if (useFkeys.get()) {
			return KeyEvent.VK_F9;
		}
		else {
			return KeyEvent.VK_NUMPAD9;
		}
	}

	private void clearAutoMark(EventContext context) {
		int keyCode = keycodeForClear();
		context.accept(new KeyPressRequest(keyCode));
	}

	private void doAutoMarkForSlot(EventContext context, int i) {
		int keyCode = keycodeForSlot(i);
		context.accept(new KeyPressRequest(keyCode));
	}

	@LiveOnly
	@HandleEvents
	public static void doKeyPress(EventContext context, KeyPressRequest event) {
		pressAndReleaseKey(event.getKeyCode());
	}

	private static void pressAndReleaseKey(int keyCode) {
		exs.submit(() -> {
			try {
				log.info("Pressing cdKey {} ({})", keyCode, KeyEvent.getKeyText(keyCode));
				new Robot().keyPress(keyCode);
				Thread.sleep(50);
				new Robot().keyRelease(keyCode);
				Thread.sleep(50);
			}
			catch (AWTException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}


}
