package gg.xp.reevent.topology;

import gg.xp.reevent.events.EventHandler;
import gg.xp.reevent.scan.AutoHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Topology extends BaseToggleableTopo {

	// TODO: later, try to make it update in place for UI purposes
	// Or, instead of uisng this as an object that merely represents the topology, have this *actually be*
	// the topology

	public Topology(List<TopoPackage> packages) {
		super("Root", packages);
	}

//	@Override
//	public boolean canBeDisabled() {
//		return false;
//	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public static Topology fromHandlers(List<EventHandler<?>> handlers) {
		// TODO: this won't work for packages
		List<TopoClass> classes = new ArrayList<>();
		Map<Class<?>, List<TopoMethod>> classesTemp = new HashMap<>();
		List<TopoMethod> manuallyAdded = new ArrayList<>();
		handlers.forEach(handler -> {
			if (handler instanceof AutoHandler) {
				Class<?> clazz = ((AutoHandler) handler).getHandlerClass();
				classesTemp.computeIfAbsent(clazz, c -> new ArrayList<>())
						.add(new TopoAutoMethod((AutoHandler) handler));
			}
			else {
				manuallyAdded.add(new TopoManualMethod(handler.toString()));
			}
		});
		classesTemp.forEach((cls, methods) -> {
			methods.sort(Comparator.comparing(TopoItem::getName));
			classes.add(new TopoClass(cls, methods));
		});
		classes.sort(Comparator.comparing(TopoItem::getName));
		classes.add(new TopoClass("Manually Registered Handlers", manuallyAdded));

		return new Topology(Collections.singletonList(new TopoPackage("Builtin", classes)));
	}


	@Override
	void applyEnabledStatus(boolean newEnabledStatus) {

	}

	@Override
	protected @Nullable String getPropertyKey() {
		return "root";
	}
}
