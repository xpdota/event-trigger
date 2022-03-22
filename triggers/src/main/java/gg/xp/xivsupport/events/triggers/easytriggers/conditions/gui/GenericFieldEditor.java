package gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.IdPicker;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.IdType;
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
import java.util.regex.Pattern;

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

	@Override
	public Dimension getPreferredSize() {
		return super.getPreferredSize();
//		return new Dimension(getMaximumSize().width, super.getPreferredSize().height);
	}

	private void addField(Field field) {
		String name;
		Description annotation = field.getAnnotation(Description.class);
		IdType idPick = field.getAnnotation(IdType.class);
		if (annotation == null) {
			name = field.getName();
		}
		else {
			name = annotation.value();
		}
		Class<?> type = field.getType();
		final Component editorComponent;
		if (type.equals(long.class) || type.equals(Long.class)) {
			if (idPick == null) {
				editorComponent = new TextFieldWithValidation<>(Long::parseLong, l -> setField(field, l), getField(field).toString());
			}
			else {
				editorComponent = IdPicker.pickerFor(idPick.value(), () -> (long) getField(field), l -> setField(field, l));
			}
		}
		else if (type.equals(int.class) || type.equals(Integer.class)) {
			editorComponent = new TextFieldWithValidation<>(Integer::parseInt, l -> setField(field, l), getField(field).toString());
		}
		else if (type.equals(String.class)) {
			editorComponent = new TextFieldWithValidation<>(Function.identity(), l -> setField(field, l), getField(field).toString());
		}
		else if (type.equals(Pattern.class)) {
			TextFieldWithValidation<Pattern> textField = new TextFieldWithValidation<>(Pattern::compile, l -> setField(field, l), getField(field).toString());
			textField.setColumns(30);
			editorComponent = textField;
//			editorComponent.setPreferredSize(new Dimension(2500, editorComponent.getPreferredSize().height));
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
