package gg.xp.reevent.topology;

import java.util.List;

public class TopoClass implements TopoItem {

	private final String className;
	private List<TopoMethod> methods;

	public TopoClass(String className, List<TopoMethod> methods) {
		this.className = className;
		this.methods = methods;
	}

	public TopoClass(Class<?> clazz, List<TopoMethod> methods) {
		this.className = "Class: " + clazz.getSimpleName();
		this.methods = methods;
	}

	@Override
	public String getName() {
		return className;
	}

	@Override
	public List<? extends TopoItem> getChildren() {
		return methods;
	}

}
