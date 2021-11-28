package gg.xp.reevent.topology;

import java.util.List;

public class TopoPackage extends BaseToggleableTopo {
	public TopoPackage(String packageName, List<? extends TopoItem> classes, TopologyInfo topoInfo) {
		// TODO: really shouldn't be 'Builtin' but I'm being kind of lazy here to preserve back compat,
		// but I also don't want to break things due to the fact that I haven't nailed down package names yet.
		super(packageName, classes, topoInfo, "Builtin");
	}
}
