package gg.xp.xivsupport.events.triggers.easytriggers.actions.gui;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovyAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovySubScriptHelper;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasEventType;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.tables.filters.ValidationError;

import javax.swing.*;
import java.util.function.Function;

public class GroovySubScriptEditor extends JPanel {
	private final TextFieldWithValidation<String> textBox;
//	private final JCheckBox checkBox;

	public GroovySubScriptEditor(GroovySubScriptHelper subScript) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		textBox = new TextFieldWithValidation<>(Function.identity(), groovyScript -> {
			try {
				subScript.setGroovyScript(groovyScript);
			}
			catch (Throwable t) {
				throw new ValidationError(t.getMessage());
			}

		}, subScript::getGroovyScript);
//		checkBox = new JCheckBox("Strict", subScript.isStrict());
//		checkBox.addActionListener(l -> {
//			subScript.setStrict(checkBox.isSelected());
//		});
//		add(checkBox);
//		add(Box.createHorizontalStrut(2));
		add(textBox);
	}
}
