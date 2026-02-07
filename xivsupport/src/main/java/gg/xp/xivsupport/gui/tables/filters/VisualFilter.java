package gg.xp.xivsupport.gui.tables.filters;

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
	 * @param item The instance to filter
	 * @return Whether it passed
	 */
	boolean passesFilter(X item);

	/**
	 * What to visually display
	 *
	 * @return a component to display
	 */
	Component getComponent();

	/**
	 * Component to display when it is being shown in a table header.
	 * This typically returns a more compact version of the filter UI (e.g. just the input field).
	 *
	 * @return a component to display in the table header
	 */
	default Component getHeaderComponent() {
		return getComponent();
	}

}
