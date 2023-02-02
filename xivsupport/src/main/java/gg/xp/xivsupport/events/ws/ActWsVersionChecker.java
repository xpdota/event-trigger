package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.GuiMain;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Arrays;

@ScanMe
public class ActWsVersionChecker {

	private static final Logger log = LoggerFactory.getLogger(ActWsVersionChecker.class);
	private static final String expectedVersionStr = "0.19.14";
	private static final int[] expectedVersion = Arrays.stream(expectedVersionStr.split("\\.")).mapToInt(Integer::parseInt).toArray();

	private final PicoContainer container;
	private boolean alreadyWarnedThisRun;

	public ActWsVersionChecker(PicoContainer container) {
		this.container = container;
	}

	@HandleEvents
	public void opVersion(EventContext context, ActWsVersionEvent event) {
		if (alreadyWarnedThisRun) {
			return;
		}
		String version = event.getVersion();

		int[] versionParts = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
		if (versionParts.length < expectedVersion.length) {
			log.error("Unusual OverlayPlugin version: {}", version);
		}
		for (int i = 0; i < expectedVersion.length; i++) {
			if (versionParts[i] < expectedVersion[i]) {
				// TODO: add an option to disable the message for the particular version
//				JFrame frame = container.getComponent(GuiMain.class).getMainFrame();
				// TODO fix, can't use the main frame directly because of race condition (this event comes in very early in startup)
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Your OverlayPlugin is out of date. You have %s, but you should have at least %s. If you were not prompted to automatically update, you should remove and reinstall it via the 'Get Plugins' button in ACT.".formatted(version, expectedVersionStr)));
				alreadyWarnedThisRun = true;
			}
		}
	}
}
