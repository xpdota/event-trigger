package gg.xp.xivsupport.gui.extra;

import java.awt.*;

/**
 * Implement this and annotate your class with {@link gg.xp.reevent.scan.ScanMe} (or any other annotation that would
 * cause it to be auto-scanned). This will give it a tab on the 'Plugins' screen.
 */
public interface PluginTab {

	/**
	 * @return The title of the tab
	 */
	String getTabName();

	/**
	 * @return The contents of the tab
	 */
	Component getTabContents();

	/**
	 * return The sort order
	 */
	default int getSortOrder() {
		return 100;
	}
}
