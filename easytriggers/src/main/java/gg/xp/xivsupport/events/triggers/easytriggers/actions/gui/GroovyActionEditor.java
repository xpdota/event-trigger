package gg.xp.xivsupport.events.triggers.easytriggers.actions.gui;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovyAction;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasEventType;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;

import javax.swing.*;
import java.util.function.Function;

public class GroovyActionEditor<X extends Event> extends JPanel {
	private final TextFieldWithValidation<String> textBox;
	private final JCheckBox checkBox;

	public GroovyActionEditor(GroovyAction action, HasEventType trigger) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		action.eventType = (Class<? extends Event>) trigger.getEventType();
		textBox = new TextFieldWithValidation<>(Function.identity(), groovyScript -> {
			try {
				action.setGroovyScript(groovyScript);
			}
			catch (Throwable t) {
				throw new ValidationError(t.getMessage());
			}

		}, action::getGroovyScript);
		checkBox = new JCheckBox("Strict", action.isStrict());
		checkBox.addActionListener(l -> {
			action.setStrict(checkBox.isSelected());
		});
		add(checkBox);
		add(Box.createHorizontalStrut(2));
		add(textBox);
	}
}
