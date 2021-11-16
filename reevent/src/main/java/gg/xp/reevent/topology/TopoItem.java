package gg.xp.reevent.topology;

import java.util.List;

public interface TopoItem {
	String getName();

	List<? extends TopoItem> getChildren();

	default boolean canBeDisabled() {
		return false;
	};

	default boolean isEnabled() {
		return true;
	}

	default void setEnabled(boolean newStatus) {
		// By default do nothing
	}
}
