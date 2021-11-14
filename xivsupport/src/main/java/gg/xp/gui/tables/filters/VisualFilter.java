package gg.xp.gui.tables.filters;

import java.awt.*;

/**
 * Class for a filter that presents a graphical interface
 *
 * @param <X> The type of thing being filtered
 */
public interface VisualFilter<X> {

	/**
	 * Whether it passes the filter
	 *
	 * @param item The item to filter
	 * @return Whether it passed
	 */
	boolean passesFilter(X item);

	/**
	 * What to visually display
	 *
	 * @return a component to display
	 */
	Component getComponent();

}
