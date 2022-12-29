package gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyEventFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasEventType;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;

import javax.swing.*;
import java.util.function.Function;

public class GroovyFilterEditor<X extends Event> extends JPanel {
	private final TextFieldWithValidation<String> textBox;
	private final JCheckBox checkBox;

	public GroovyFilterEditor(GroovyEventFilter filter, HasEventType trigger) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		filter.eventType = (Class<? extends Event>) trigger.getEventType();
		textBox = new TextFieldWithValidation<>(Function.identity(), groovyScript -> {
			try {
				filter.setGroovyScript(groovyScript);
			}
			catch (Throwable t) {
				throw new ValidationError(t.getMessage());
			}

		}, filter::getGroovyScript);
		checkBox = new JCheckBox("Strict", filter.isStrict());
		checkBox.addActionListener(l -> {
			filter.setStrict(checkBox.isSelected());
		});
		add(checkBox);
		add(Box.createHorizontalStrut(2));
		add(textBox);
	}
}
