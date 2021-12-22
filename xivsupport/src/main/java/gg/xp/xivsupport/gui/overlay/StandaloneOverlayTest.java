package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StandaloneOverlayTest {
	private static final Logger log = LoggerFactory.getLogger(StandaloneOverlayTest.class);

	private StandaloneOverlayTest() {
	}

	public static void main(String[] args) throws InterruptedException {
		CommonGuiSetup.setup();
		{
			XivOverlay overlay = new ExampleOverlayWithLotsOfButtons(new InMemoryMapPersistenceProvider());
			overlay.finishInit();
			overlay.setVisible(true);
			overlay.setEditMode(true);
			double scaleFactor = 5.2;
			overlay.setScale(scaleFactor);
		}
		{
			XivOverlay overlay = new ExampleOverlayWithLotsOfButtons(new InMemoryMapPersistenceProvider());
			overlay.finishInit();
			overlay.setVisible(true);
			overlay.setEditMode(true);
			double scaleFactor = 1.0;
			overlay.setScale(scaleFactor);
		}
//		Thread.sleep(3000);
//		overlay.setScale(2.2);
//		Thread.sleep(3000);
//		overlay.setScale(0.8);
	}
}
