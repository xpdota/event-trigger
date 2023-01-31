package gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.conditions.Description;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.EditorIgnore;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.IdPickerFactory;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.IdType;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.WideTextField;
import gg.xp.xivsupport.events.triggers.easytriggers.model.AcceptsSaveCallback;
import gg.xp.xivsupport.gui.ColorPickerGui;
import gg.xp.xivsupport.gui.ResettableField;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class GenericFieldEditor extends JPanel implements AcceptsSaveCallback {

	private final Object object;
	private final List<ResettableField> resettables = new ArrayList<>();
	private final IdPickerFactory idPickerFactory;

	// TODO: this doesn't receive auto-save functionality
	public GenericFieldEditor(Object object, PicoContainer pico) {
		setLayout(new WrapLayout(0, 10, 0));
		idPickerFactory = pico.getComponent(IdPickerFactory.class);
		this.object = object;
		Field[] fields = object.getClass().getFields();
		Arrays.stream(fields)
				.filter(field -> !Modifier.isStatic(field.getModifiers()))
				.filter(field -> !field.isAnnotationPresent(EditorIgnore.class))
				.forEach(this::addField);
	}

	public GenericFieldEditor(Object object, PicoContainer pico, Field[] fields) {
		setLayout(new WrapLayout(0, 10, 0));
		idPickerFactory = pico.getComponent(IdPickerFactory.class);
		this.object = object;
		Arrays.stream(fields)
				.filter(field -> !Modifier.isStatic(field.getModifiers()))
				.filter(field -> !field.isAnnotationPresent(EditorIgnore.class))
				.forEach(this::addField);
	}

	@Override
	public Dimension getPreferredSize() {
		return super.getPreferredSize();
//		return new Dimension(getMaximumSize().width, super.getPreferredSize().height);
	}

	private String valueToString(Object value) {
		if (value == null) {
			return "";
		}
		else {
			return String.valueOf(value);
		}
	}

	private void addField(Field field) {
		String name;
		boolean useLabel = true;
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
		Object fieldValue = getField(field);
		if (type.equals(long.class) || type.equals(Long.class)) {
			if (idPick == null) {
				// TODO: replace simple parseLong with something that allows hex
				editorComponent = new TextFieldWithValidation<>(Long::parseLong, l -> setField(field, l), () -> valueToString(getField(field).toString()));
			}
			else {
				editorComponent = idPickerFactory.pickerFor(idPick.value(), idPick.matchRequired(), () -> (long) getField(field), l -> setField(field, l));
			}
		}
		else if (type.equals(int.class) || type.equals(Integer.class)) {
			editorComponent = new TextFieldWithValidation<>(Integer::parseInt, l -> setField(field, l), () -> valueToString(String.valueOf(getField(field))));
		}
		else if (type.equals(float.class) || type.equals(Float.class)) {
			editorComponent = new TextFieldWithValidation<>(Float::parseFloat, l -> setField(field, l), () -> valueToString(String.valueOf(getField(field))));
		}
		else if (type.equals(double.class) || type.equals(Double.class)) {
			editorComponent = new TextFieldWithValidation<>(Double::parseDouble, l -> setField(field, l), () -> valueToString(String.valueOf(getField(field))));
		}
		else if (String.class.isAssignableFrom(type)) {
			TextFieldWithValidation<String> textField = new TextFieldWithValidation<>(Function.identity(), l -> setField(field, l), () -> valueToString(String.valueOf(getField(field))));
			if (field.isAnnotationPresent(WideTextField.class)) {
				textField.setColumns(50);
			}
			editorComponent = textField;
		}
		else if (type.equals(Pattern.class)) {
			TextFieldWithValidation<Pattern> textField = new TextFieldWithValidation<>(Pattern::compile, l -> setField(field, l), () -> valueToString(getField(field).toString()));
			textField.setColumns(30);
			editorComponent = textField;
//			editorComponent.setPreferredSize(new Dimension(2500, editorComponent.getPreferredSize().height));
		}
		else if (type.isEnum()) {
			JComboBox<?> comboBox = new JComboBox<>(type.getEnumConstants());
			comboBox.setSelectedItem(fieldValue);
			comboBox.addItemListener(l -> setField(field, comboBox.getSelectedItem()));
			comboBox.setRenderer(new FriendlyNameListCellRenderer());
			editorComponent = comboBox;
		}
		else if (Runnable.class.isAssignableFrom(type)) {
			JButton button = new JButton(name);
			button.addActionListener(l -> {
				((Runnable) fieldValue).run();
				resettables.forEach(ResettableField::reset);
			});
			editorComponent = button;
			useLabel = false;
		}
		else if (boolean.class.equals(type)) {
			JCheckBox cb = new JCheckBox(name);
			cb.setSelected((boolean) fieldValue);
			cb.addActionListener(l -> setField(field, cb.isSelected()));
			editorComponent = cb;
			useLabel = false;
		}
		else if (Color.class.equals(type)) {
			ColorPickerGui picker = new ColorPickerGui(() -> (Color) getField(field), color -> setField(field, color), name, () -> true);
			editorComponent = picker.getComponent();
			useLabel = false;
		}
		else {
			editorComponent = new JLabel("No editor for " + type.getSimpleName());
		}
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		if (useLabel) {
			JLabel nameLabel = GuiUtil.labelFor(name + ':', editorComponent);
			panel.add(nameLabel);
		}
		panel.add(editorComponent);
		add(panel);
		if (editorComponent instanceof ResettableField rb) {
			resettables.add(rb);
		}
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
		if (saveCallback != null) {
			saveCallback.run();
		}
	}

	private Runnable saveCallback;

	@Override
	public void setSaveCallback(Runnable saveCallback) {
		saveCallback = saveCallback;
	}
}
