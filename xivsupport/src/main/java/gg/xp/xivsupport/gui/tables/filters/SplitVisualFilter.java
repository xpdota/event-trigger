package gg.xp.xivsupport.gui.tables.filters;

import javax.swing.*;

public interface SplitVisualFilter<X> extends VisualFilter<X> {
	String getName();

	@Override
	JPanel getComponent();
}
