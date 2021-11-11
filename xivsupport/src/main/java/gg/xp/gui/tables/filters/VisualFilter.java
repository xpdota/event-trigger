package gg.xp.gui.tables.filters;

import java.awt.*;

public interface VisualFilter<X> {

	boolean passesFilter(X item);

	Component getComponent();

}
