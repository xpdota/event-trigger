package gg.xp.xivsupport.gui.extra;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public interface TabDef {
	/**
	 * @return The title of the tab
	 */
	String getTabName();

	/**
	 * @return The contents of the tab
	 */
	Component getTabContents();

	default List<Object> keys() {
		return Collections.singletonList(this);
	}

}
