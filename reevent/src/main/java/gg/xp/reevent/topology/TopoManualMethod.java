package gg.xp.reevent.topology;

import java.util.Collections;
import java.util.List;

public class TopoManualMethod implements TopoMethod {

	private final String methodLabel;

	public TopoManualMethod(String methodLabel) {
		this.methodLabel = methodLabel;
	}

	@Override
	public String getName() {
		return "Method: " + methodLabel;
	}

	@Override
	public List<? extends TopoItem> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public void init() {

	}

}
