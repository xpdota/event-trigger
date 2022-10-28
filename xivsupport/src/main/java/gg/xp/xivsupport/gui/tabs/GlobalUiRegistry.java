package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.scan.ScanMe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ScanMe
public class GlobalUiRegistry {

	private static final Logger log = LoggerFactory.getLogger(GlobalUiRegistry.class);
	private final Map<Object, TabRef> mapping = new ConcurrentHashMap<>();

	private record TabRef(Runnable activator) {
	}

	public void registerTab(Object key, JTabbedPane pane, int tabNum, Object... parents) {
		registerItem(key, () -> {
			for (Object parent : parents) {
				activateItem(parent);
			}
			pane.setSelectedIndex(tabNum);
		});
	}

	public void registerItem(Object key, Runnable activator, Object... parents) {
		Runnable combinedActivator = () -> {
			for (Object parent : parents) {
				activateItem(parent);
			}
			activator.run();

		};
		mapping.put(key, new TabRef(combinedActivator));
		mapping.put(key.getClass(), new TabRef(combinedActivator));
	}


	public boolean activateItem(Object key) {
		TabRef tabRef = mapping.get(key);
		if (tabRef == null) {
			log.warn("Did not find registered item for ({})!", key);
			return false;
		}
		else {
			tabRef.activator.run();
			return true;
		}
	}

}
