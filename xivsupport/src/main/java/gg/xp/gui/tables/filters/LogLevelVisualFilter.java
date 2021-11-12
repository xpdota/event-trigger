package gg.xp.gui.tables.filters;

import gg.xp.events.slf4j.LogEvent;
import gg.xp.gui.tables.filters.VisualFilter;
import org.slf4j.event.Level;

import javax.swing.*;
import java.awt.*;

public class LogLevelVisualFilter implements VisualFilter<LogEvent> {

	private final JComboBox<Level> comboBox;
	private Level currentOption;

	public LogLevelVisualFilter(Runnable filterUpdatedCallback) {
		comboBox = new JComboBox<>(Level.values());
		comboBox.addItemListener(event -> {
			currentOption = (Level) event.getItem();
			filterUpdatedCallback.run();
		});
		comboBox.setSelectedItem(Level.INFO);
	}

	@Override
	public boolean passesFilter(LogEvent item) {
		ch.qos.logback.classic.Level wantedLevel = ch.qos.logback.classic.Level.toLevel(currentOption.toString());
		return item.getEvent().getLevel().isGreaterOrEqual(wantedLevel);
	}

	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel("Log Level: ");
		label.setLabelFor(comboBox);
		panel.add(label);
		panel.add(comboBox);
		return panel;
	}
}
