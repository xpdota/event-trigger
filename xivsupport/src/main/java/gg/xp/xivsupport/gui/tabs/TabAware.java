package gg.xp.xivsupport.gui.tabs;

import java.awt.*;

public interface TabAware {

	default boolean hasWarning() {
		return false;
	}

	Component getParent();

	default void notifyParents() {
		Component parent = getParent();
		while (parent != null) {
			if (parent instanceof SmartTabbedPane stp) {
				stp.recheckTabs();
				break;
			}
			else {
				parent = parent.getParent();
			}
		}
	}

}
