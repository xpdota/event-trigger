package gg.xp.reevent.topology;

import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.scan.AutoHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Topology extends BaseToggleableTopo {

	private static final Logger log = LoggerFactory.getLogger(Topology.class);

	// TODO: later, try to make it update in place for UI purposes
	// Or, instead of using this as an object that merely represents the topology, have this *actually be*
	// the topology

	public Topology(List<? extends TopoItem> packages, TopologyInfo info) {
		super("Root", packages, info, "root");
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public static Topology fromHandlers(List<EventHandler<?>> handlers, TopologyInfo topoInfo) {
		// TODO: this won't work for packages
		List<TopoClass> classes = new ArrayList<>();
		Map<Class<?>, List<TopoMethod>> classesTemp = new HashMap<>();
		List<TopoMethod> manuallyAdded = new ArrayList<>();
		handlers.forEach(handler -> {
			if (handler instanceof AutoHandler) {
				Class<?> clazz = ((AutoHandler) handler).getHandlerClass();
				classesTemp.computeIfAbsent(clazz, c -> new ArrayList<>())
						.add(new TopoAutoMethod((AutoHandler) handler, topoInfo));
			}
			else {
				manuallyAdded.add(new TopoManualMethod(handler.toString()));
			}
		});
		classesTemp.forEach((cls, methods) -> {
			methods.sort(Comparator.comparing(TopoItem::getName));
			classes.add(new TopoClass(cls, methods, topoInfo));
		});
		classes.sort(Comparator.comparing(TopoItem::getName));

		Map<String, List<TopoClass>> byPackage = new HashMap<>();
		// TODO: I still want a better way of providing auto-scan details, like a friendly name,
		// but this will be fine for now.
		Pattern pattern = Pattern.compile("^.*/([^/\\d]*[^\\d-])(?:/target/classes/|.*\\.jar)$");
		MutableInt mint = new MutableInt();
		classes.forEach(c -> {
			String location = c.getHandlerClass().getProtectionDomain().getCodeSource().getLocation().toString();
			String packageName;
			Matcher matcher = pattern.matcher(location);
			if (matcher.matches()) {
				packageName = matcher.group(1);
			}
			else {
				packageName = "Unknown-" + mint.getAndIncrement();
			}
			// hacks
			if (packageName.equals("xivsupport")) {
				packageName = "Builtin (Do Not Touch These)";
			}
			byPackage.computeIfAbsent(packageName, (ignored) -> new ArrayList<>()).add(c);
		});

		List<TopoPackage> packages = new ArrayList<>();
		byPackage.forEach((name, list) -> {
			packages.add(new TopoPackage(name, list, topoInfo));
		});
		TopoPackage manual = new TopoPackage("Manually Added (Do Not Touch These, also TODO Fix this)", manuallyAdded, topoInfo);
		packages.add(manual);
		Topology topo = new Topology(packages, topoInfo);
		topo.init();
		return topo;
	}
}
