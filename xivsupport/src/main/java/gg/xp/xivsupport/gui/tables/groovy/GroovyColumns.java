package gg.xp.xivsupport.gui.tables.groovy;

import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import groovy.lang.GroovyRuntimeException;
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
import java.util.Objects;
import java.util.stream.Collectors;

public final class GroovyColumns {
	private static final Logger log = LoggerFactory.getLogger(GroovyColumns.class);
	public static final CustomColumn<PropertyValue> propName = new CustomColumn<>("Name", PropertyValue::getName);
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
		else {
			return DefaultGroovyMethods.getMetaPropertyValues(obj)
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
					.toList();
		}
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
}

