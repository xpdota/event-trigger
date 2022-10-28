package gg.xp.xivsupport.gui.extra;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.tabs.TabAware;

import java.awt.*;

/**
 * Implement this and annotate your class with {@link ScanMe} (or any other annotation that would
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

	/**
	 * return false to force synchronous loading of this plugin tab. This is realistically only needed if one of the
	 * following is true:
	 * <ol>
	 *     <li>You are relying on this plugin tab being loaded for proper functionality (this is bad practice - please avoid it)</li>
	 *     <li>You are making use of {@link TabAware} to highlight a problem for the user.</li>
	 * </ol>
	 *
	 * @return Whether or not it is fine to asynchronously load this plugin tab
	 */
	default boolean asyncOk() {
		return true;
	}

	/**
	 * Provides the tab with a hook for bringing itself to the front. Since these are lazy-loaded now, you can't use
	 * the old trick of grabbing some UI component of the tab in question and walking back up the tree to find
	 * JTabbedPane instances that need to switch tabs. Instead, use the provided runnable as the "activate me" button.
	 * <p>
	 * This default method is a no-op since most tabs won't ever need to be force-activated.
	 *
	 * @param hook A hook to use.
	 */
	default void installActivationHook(Runnable hook) {

	}
}
