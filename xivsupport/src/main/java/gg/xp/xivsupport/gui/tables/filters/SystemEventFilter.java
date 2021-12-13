package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.SystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class SystemEventFilter implements VisualFilter<Event> {

	private static final Logger log = LoggerFactory.getLogger(SystemEventFilter.class);

	private final JCheckBox checkBox;
	private final Runnable filterUpdatedCallback;
	private boolean selectedItem;

	public SystemEventFilter(Runnable filterUpdatedCallback) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		checkBox = new JCheckBox();
		checkBox.addItemListener(i -> {
			update();
		});
	}

	private void update() {
		selectedItem = checkBox.isSelected();
		filterUpdatedCallback.run();
	}

	@Override
	public boolean passesFilter(Event item) {
		if (selectedItem) {
			return true;
		}
		return !item.getClass().isAnnotationPresent(SystemEvent.class);
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		checkBox.setText("Show System Events");
		panel.add(checkBox);
		return panel;
	}
}
