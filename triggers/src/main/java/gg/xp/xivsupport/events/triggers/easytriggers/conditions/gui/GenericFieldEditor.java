package gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Function;

public class GenericFieldEditor extends JPanel {

	private final Object object;

	public GenericFieldEditor(Object object) {
		setLayout(new WrapLayout(0, 10, 0));
		this.object = object;
		Field[] fields = object.getClass().getFields();
		Arrays.stream(fields)
				.filter(field -> !Modifier.isStatic(field.getModifiers()))
				.forEach(this::addField);
	}

	private void addField(Field field) {
		String name = field.getName();
		Class<?> type = field.getType();
		final Component editorComponent;
		if (type.equals(long.class) || type.equals(Long.class)) {
			editorComponent = new TextFieldWithValidation<>(Long::parseLong, l -> setField(field, l), getField(field).toString());
		}
		else if (type.equals(String.class)) {
			editorComponent = new TextFieldWithValidation<>(Function.identity(), l -> setField(field, l), getField(field).toString());
		}
		else if (type.isEnum()) {
			JComboBox<?> comboBox = new JComboBox<>(type.getEnumConstants());
			comboBox.setSelectedItem(getField(field));
			comboBox.addItemListener(l -> setField(field, comboBox.getSelectedItem()));
			comboBox.setRenderer(new FriendlyNameListCellRenderer());
			editorComponent = comboBox;
		}
		else {
			editorComponent = new JLabel("No editor for " + type.getSimpleName());
		}
		JPanel panel = new JPanel();
		JLabel nameLabel = GuiUtil.labelFor(name + ':', editorComponent);
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.add(nameLabel);
		panel.add(editorComponent);
		add(panel);
	}

	private Object getField(Field field) {
		try {
			return field.get(object);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void setField(Field field, Object value) {
		try {
			field.set(object, value);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}


}
