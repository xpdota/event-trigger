package gg.xp.xivsupport.gui.tables.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiPredicate;

public class BooleanEventFilter<X> implements VisualFilter<X> {

	private static final Logger log = LoggerFactory.getLogger(BooleanEventFilter.class);

	private final JCheckBox checkBox;
	private final Runnable filterUpdatedCallback;
	private final String label;
	private final BiPredicate<Boolean, X> filter;
	private boolean cbstate;

	public BooleanEventFilter(Runnable filterUpdatedCallback, String label, BiPredicate<Boolean, X> filter, boolean defaultState) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		this.label = label;
		this.filter = filter;
		cbstate = defaultState;
		checkBox = new JCheckBox();
		checkBox.setSelected(defaultState);
		checkBox.addItemListener(i -> {
			update();
		});
	}

	private void update() {
		cbstate = checkBox.isSelected();
		filterUpdatedCallback.run();
	}

	@Override
	public boolean passesFilter(X item) {
		boolean state = cbstate;
		return filter.test(state, item);
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		checkBox.setText(label);
		panel.add(checkBox);
		return panel;
	}
}
