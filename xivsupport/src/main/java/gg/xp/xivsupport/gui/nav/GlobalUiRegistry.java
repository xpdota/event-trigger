package gg.xp.xivsupport.gui.nav;

import gg.xp.reevent.scan.ScanMe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ScanMe
public class GlobalUiRegistry {

	private static final Logger log = LoggerFactory.getLogger(GlobalUiRegistry.class);
	private final Map<Object, GuiRef> mapping = new ConcurrentHashMap<>();
	private final Map<String, GuiRef> nameMapping = new ConcurrentHashMap<>();
	private final List<GuiRef> list = new ArrayList<>();


	public void registerTab(Object key, @NotNull String label, List<String> altNames, JTabbedPane pane, int tabNum, Object... parents) {
		registerItem(key, label, altNames, () -> {
			for (Object parent : parents) {
				activateItem(parent);
			}
			pane.setSelectedIndex(tabNum);
		});
	}

	public void registerItem(Object key, @NotNull String label, List<String> altNames, Runnable activator, Object... parents) {
		Runnable combinedActivator = () -> {
			for (Object parent : parents) {
				activateItem(parent);
			}
			activator.run();
		};
		List<String> combinedNames;
		if (altNames == null || altNames.isEmpty()) {
			combinedNames = Collections.singletonList(label);
			altNames = Collections.emptyList();
		}
		else {
			combinedNames = new ArrayList<>(altNames.size() + 1);
			combinedNames.add(label);
			combinedNames.addAll(altNames);
		}
		GuiRef ref = new GuiRef(label, combinedNames, combinedActivator);
		mapping.put(key, ref);
		mapping.put(key.getClass(), ref);
		nameMapping.put(label, ref);
		for (String name : altNames) {
			nameMapping.put(name, ref);
		}
		list.add(ref);
	}

	public boolean activateByName(String name) {
		GuiRef ref = nameMapping.get(name);
		if (ref == null) {
			return false;
		}
		else {
			ref.activate();
			return true;
		}
	}

	public List<GuiRef> search(String searchTerm) {
		String lower = searchTerm.toLowerCase();
		return list.stream()
				.filter(item -> item.names().stream().anyMatch(name -> name.toLowerCase().contains(lower)))
				.toList();
	}

	public boolean activateItem(Object key) {
		GuiRef guiRef = mapping.get(key);
		if (guiRef == null) {
			log.warn("Did not find registered instance for ({})!", key);
			return false;
		}
		else {
			guiRef.activate();
			return true;
		}
	}

}
