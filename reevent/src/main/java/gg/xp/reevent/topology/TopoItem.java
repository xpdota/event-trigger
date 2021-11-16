package gg.xp.reevent.topology;

import java.util.List;

public interface TopoItem {
	String getName();

	List<? extends TopoItem> getChildren();

	default boolean canBeDisabled() {
		return false;
	};

	default boolean isEnabledDirectly() {
		return true;
	}

	default void setEnabledDirectly(boolean newStatus) {
		// By default do nothing, since not every topo can be enabled/disabled
	}

	default boolean isEnabledByParent() {
		return true;
	}

	default void setEnabledByParent(boolean enabled) {
		// By default do nothing, since not every topo can be enabled/disabled
	}

}
