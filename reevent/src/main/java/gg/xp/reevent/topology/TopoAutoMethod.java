package gg.xp.reevent.topology;

import gg.xp.reevent.scan.AutoHandler;

import java.util.Collections;
import java.util.List;

public class TopoAutoMethod implements TopoMethod {


	private final AutoHandler handler;

	public TopoAutoMethod(AutoHandler handler) {
		this.handler = handler;
	}

	@Override
	public String getName() {
		return "Method: " + handler.getTopoLabel();
	}

	@Override
	public List<? extends TopoItem> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public boolean canBeDisabled() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return handler.isEnabled();
	}

	@Override
	public void setEnabled(boolean newStatus) {
		handler.setEnabled(newStatus);
	}
}
