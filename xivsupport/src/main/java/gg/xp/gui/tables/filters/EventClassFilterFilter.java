package gg.xp.gui.tables.filters;

import gg.xp.events.Event;
import gg.xp.events.actlines.events.HasAbility;
import gg.xp.events.actlines.events.HasStatusEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Locale;

public class EventClassFilterFilter implements VisualFilter<Event> {

	private static final Logger log = LoggerFactory.getLogger(EventClassFilterFilter.class);

	private final JTextField textBox;
	private final Runnable filterUpdatedCallback;
	// TODO: really shouldn't be a string
	private String selectedItem;

	public EventClassFilterFilter(Runnable filterUpdatedCallback) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		textBox = new JTextField(10);
		textBox.setEditable(true);
		textBox.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
		});
		selectedItem = textBox.getText();
	}

	private void update() {
		selectedItem = textBox.getText();
		filterUpdatedCallback.run();
	}

	@Override
	public boolean passesFilter(Event item) {
		if (selectedItem.isEmpty()) {
			return true;
		}
		String simpleName = item.getClass().getSimpleName();
		return simpleName.toUpperCase(Locale.ROOT).contains(selectedItem.toUpperCase(Locale.ROOT));
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel("Event Class: ");
		label.setLabelFor(textBox);
		panel.add(label);
		panel.add(textBox);
		return panel;
	}
}
