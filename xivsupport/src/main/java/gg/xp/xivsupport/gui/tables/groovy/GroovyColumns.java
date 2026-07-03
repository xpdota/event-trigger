package gg.xp.xivsupport.gui.tables.groovy;

import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaProperty;
import groovy.lang.PropertyValue;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class GroovyColumns {
	private static final Logger log = LoggerFactory.getLogger(GroovyColumns.class);
	public static final CustomColumn<PropertyValue> propName = new CustomColumn<>("Property", PropertyValue::getName);
	public static final CustomColumn<PropertyValue> propVal = new CustomColumn<>("Value", propertyValue -> {
		try {
			return propertyValue.getValue();
		}
		catch (Throwable t) {
			return t;
		}
	}, c -> {
		c.setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				value = singleValueConversion(value);
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
	});
	public static final CustomColumn<PropertyValue> propType = new CustomColumn<>("Type", PropertyValue::getType);

	private GroovyColumns() {
	}

	public static List<PropertyValue> getValues(Object obj) {
		if (obj == null) {
			return Collections.emptyList();
		}
		List<PropertyValue> props = new ArrayList<>(DefaultGroovyMethods.getMetaPropertyValues(obj)
				.stream()
				.filter(pv1 -> {
					try {
						return isReadable(pv1);
					}
					catch (Throwable t) {
						return false;
					}
				})
				.filter(pv -> !"serialVersionUID".equals(pv.getName()))
				.toList());

		if (obj instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Object key = entry.getKey();
				Object convertedKey = singleValueConversion(key);
				String keyStr = key instanceof String ? "\"" + convertedKey + "\"" : String.valueOf(convertedKey);
				props.add(new VirtualPropertyValue("[" + keyStr + "]", entry, Map.Entry.class));
			}
		}
		else if (obj instanceof List<?> list) {
			for (int i = 0; i < list.size(); i++) {
				props.add(new VirtualPropertyValue("[" + i + "]", list.get(i), Object.class));
			}
		}
		else if (obj instanceof Set<?> set) {
			int i = 0;
			for (Object o : set) {
				props.add(new VirtualPropertyValue("[" + i++ + "]", o, Object.class));
			}
		}
		else if (obj.getClass().isArray()) {
			int length = Array.getLength(obj);
			for (int i = 0; i < length; i++) {
				props.add(new VirtualPropertyValue("[" + i + "]", Array.get(obj, i), Object.class));
			}
		}
		return props;
	}

	public static boolean isReadable(PropertyValue pv) {
		try {
			pv.getValue();
			return true;
		}
		catch (GroovyRuntimeException gre) {
			if (gre.getMessage() != null && gre.getMessage().startsWith("Cannot read write-only property")) {
				return false;
			}
			else {
				throw gre;
			}
		}
	}

	public static boolean propEquals(PropertyValue a, PropertyValue b) {
		return Objects.equals(a.getName(), b.getName());
	}

	public static void addColumns(CustomTableModel.CustomTableModelBuilder<PropertyValue> builder) {
		builder.addColumn(propName)
				.addColumn(propVal)
				.addColumn(propType);
		builder.setItemEquivalence(GroovyColumns::propEquals);
	}

	public static void addDetailColumns(TableWithFilterAndDetails.TableWithFilterAndDetailsBuilder<?, PropertyValue> builder) {
		builder.addDetailsColumn(propName)
				.addDetailsColumn(propVal)
				.addDetailsColumn(propType);
		builder.setDetailsSelectionEquivalence(GroovyColumns::propEquals);
	}

	@SuppressWarnings("MalformedFormatString")
	public static Object singleValueConversion(Object obj) {
		if (obj == null) {
			return "(null)";
		}
		if (obj instanceof Map.Entry<?, ?> entry) {
			return singleValueConversion(entry.getValue());
		}
		if (obj instanceof Byte || obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
			return String.format("%d (0x%x)", obj, obj);
		}
		if (obj.getClass().isArray()) {
			int length = Array.getLength(obj);
			List<Object> converted = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				converted.add(Array.get(obj, i));
			}
			return converted.stream()
					.map(GroovyColumns::singleValueConversion)
					.map(Object::toString)
					.collect(Collectors.joining(", ", "[", "]"));
		}
		return obj;
	}

	private static class VirtualPropertyValue extends PropertyValue {
		private final String name;
		private final Object value;
		private final Class<?> type;

		public VirtualPropertyValue(String name, Object value, Class<?> type) {
			super(null, new VirtualMetaProperty(name, type));
			this.name = name;
			this.value = value;
			this.type = type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Class getType() {
			return type;
		}
	}

	private static class VirtualMetaProperty extends MetaProperty {
		public VirtualMetaProperty(String name, Class<?> type) {
			super(name, type);
		}

		@Override
		public Object getProperty(Object object) {
			return null;
		}

		@Override
		public void setProperty(Object object, Object newValue) {
		}
	}
}

