package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StandaloneOverlayTest {
	private static final Logger log = LoggerFactory.getLogger(StandaloneOverlayTest.class);

	private StandaloneOverlayTest() {
	}

	public static void main(String[] args) throws InterruptedException {
		CommonGuiSetup.setup();
		MutablePicoContainer pico = XivMain.testingMinimalInit();
		pico.addComponent(OverlayMain.class);
		OverlayConfig oc = pico.getComponent(OverlayConfig.class);
		PersistenceProvider pers = pico.getComponent(PersistenceProvider.class);
		{
			XivOverlay overlay = new ExampleOverlayWithLotsOfButtons(new InMemoryMapPersistenceProvider(), oc);
			overlay.finishInit();
			overlay.setVisible(true);
			overlay.setEditMode(true);
			overlay.getEnabled().set(true);
			double scaleFactor = 5.2;
			overlay.setScale(scaleFactor);
		}
		{
			XivOverlay overlay = new ExampleOverlayWithLotsOfButtons(new InMemoryMapPersistenceProvider(), oc);
			overlay.finishInit();
			overlay.setVisible(true);
			overlay.setEditMode(true);
			overlay.getEnabled().set(true);
			double scaleFactor = 1.0;
			overlay.setScale(scaleFactor);
		}
//		Thread.sleep(3000);
//		overlay.setScale(2.2);
//		Thread.sleep(3000);
//		overlay.setScale(0.8);
	}
}
