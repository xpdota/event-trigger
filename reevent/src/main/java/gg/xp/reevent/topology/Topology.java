package gg.xp.reevent.topology;

import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.scan.AutoHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Topology extends BaseToggleableTopo {

	// TODO: later, try to make it update in place for UI purposes
	// Or, instead of using this as an object that merely represents the topology, have this *actually be*
	// the topology

	public Topology(List<TopoPackage> packages, TopologyInfo info) {
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
		classes.add(new TopoClass("Manually Registered Handlers", manuallyAdded, topoInfo));

		Topology topo = new Topology(Collections.singletonList(new TopoPackage("Builtin", classes, topoInfo)), topoInfo);
		topo.init();
		return topo;
	}
}
