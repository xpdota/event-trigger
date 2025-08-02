package gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyFolderFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasEventType;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;

import javax.swing.*;
import java.util.function.Function;

/**
 * Editor for GroovySupplierFilter, similar to GroovyFilterEditor but without event type handling.
 * This is used for TriggerFolder conditions that don't depend on specific event types.
 */
public class GroovyFolderFilterEditor extends JPanel {
	private final TextFieldWithValidation<String> textBox;
	private final JCheckBox checkBox;

	public GroovyFolderFilterEditor(GroovyFolderFilter filter, HasEventType trigger) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
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