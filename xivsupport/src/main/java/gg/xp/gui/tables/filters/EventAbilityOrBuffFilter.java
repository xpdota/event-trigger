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

public class EventAbilityOrBuffFilter implements VisualFilter<Event> {

	private static final Logger log = LoggerFactory.getLogger(EventAbilityOrBuffFilter.class);

	private final JTextField textBox;
	private final Runnable filterUpdatedCallback;
	// TODO: really shouldn't be a string
	private String selectedItem;

	public EventAbilityOrBuffFilter(Runnable filterUpdatedCallback) {
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
		// TODO: computing a single lambda once when we change filters is probably faster?
		if (selectedItem.isEmpty()) {
			return true;
		}
		// We need to check them both just in case there's something that has both.
		// TODO: is there?
		if (item instanceof HasAbility) {
			if (((HasAbility) item).getAbility().matchesFilter(selectedItem)) {
				return true;
			}
		}
		if (item instanceof HasStatusEffect) {
			if (((HasStatusEffect) item).getBuff().matchesFilter(selectedItem)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel("Ability/Buff: ");
		label.setLabelFor(textBox);
		panel.add(label);
		panel.add(textBox);
		return panel;
	}
}
