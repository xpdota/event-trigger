package gg.xp.reevent.topology;

import java.util.List;
import java.util.Objects;

public class TopoClass extends BaseToggleableTopo {

	private final boolean canBeToggled;

	public TopoClass(String className, List<TopoMethod> methods, TopologyInfo info) {
		super(className, methods, info, "manual");
		canBeToggled = false;
	}

	public TopoClass(Class<?> clazz, List<TopoMethod> methods, TopologyInfo info) {
		super("Class: " + clazz.getSimpleName(), methods, info, Objects.requireNonNull(clazz.getSimpleName(), "Cannot have an anonymous class in a topology!" + clazz));
		canBeToggled = true;
	}

	@Override
	public boolean canBeDisabled() {
		return canBeToggled;
	}
}
