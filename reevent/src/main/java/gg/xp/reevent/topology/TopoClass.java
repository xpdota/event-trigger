package gg.xp.reevent.topology;

import java.util.List;
import java.util.Objects;

public class TopoClass extends BaseToggleableTopo {

	private final boolean canBeToggled;

	private final Class<?> clazz;

	public TopoClass(Class<?> clazz, List<TopoMethod> methods, TopologyInfo info) {
		super("Class: " + clazz.getSimpleName(), methods, info, Objects.requireNonNull(clazz.getSimpleName(), "Cannot have an anonymous class in a topology!" + clazz));
		this.clazz = clazz;
		canBeToggled = true;
	}

	public Class<?> getHandlerClass() {
		return clazz;
	}

	@Override
	public boolean canBeDisabled() {
		return canBeToggled;
	}
}
