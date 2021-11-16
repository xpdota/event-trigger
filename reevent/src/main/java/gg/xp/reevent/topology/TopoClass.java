package gg.xp.reevent.topology;

import java.util.List;

public class TopoClass extends BaseToggleableTopo {

	private List<TopoMethod> methods;
	private final String key;

	public TopoClass(String className, List<TopoMethod> methods) {
		super(className, methods);
		this.key = "TODO";
	}

	public TopoClass(Class<?> clazz, List<TopoMethod> methods) {
		super("Class: " + clazz.getSimpleName(), methods);
		String canonicalName = clazz.getCanonicalName();
		if (canonicalName == null) {
			throw new IllegalArgumentException("Cannot have an anonymous class in a topology! " + clazz);
		}
		this.key = canonicalName;
	}

	@Override
	void applyEnabledStatus(boolean newEnabledStatus) {
		// no-op - we only disable children
	}

	@Override
	protected String getPropertyKey() {
		return key;
	}

}
