package gg.xp.topology;

import java.util.List;

public class TopoPackage implements TopoItem {

	private final String packageName;
	private List<TopoClass> classes;

	public TopoPackage(String packageName, List<TopoClass> classes) {
		this.packageName = packageName;
		this.classes = classes;
	}


	@Override
	public String getName() {
		return packageName;
	}

	@Override
	public List<? extends TopoItem> getChildren() {
		return classes;
	}
}
