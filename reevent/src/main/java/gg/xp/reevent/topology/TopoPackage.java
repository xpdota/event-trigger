package gg.xp.reevent.topology;

import java.util.List;

public class TopoPackage extends BaseToggleableTopo {
	public TopoPackage(String packageName, List<TopoClass> classes, TopologyInfo topoInfo) {
		super(packageName, classes, topoInfo, packageName);
	}
}
