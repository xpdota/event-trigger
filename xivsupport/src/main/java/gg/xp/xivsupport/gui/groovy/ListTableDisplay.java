package gg.xp.xivsupport.gui.groovy;

import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static gg.xp.xivsupport.gui.groovy.GroovyPanel.singleValueConversion;

public class ListTableDisplay {
	private final List<TableColumn> cols;
	private final boolean autoProps;
	private final List<TypeDisplay> typeDisplays;

	public ListTableDisplay(List<TableColumn> cols, boolean autoProps, List<TypeDisplay> typeDisplays) {
		this.cols = Collections.unmodifiableList(cols);
		this.autoProps = autoProps;
		this.typeDisplays = typeDisplays;
	}

	public ListTableDisplay(List<TableColumn> cols, boolean autoProps) {
		this(cols, autoProps, List.of());
	}

	public List<TableColumn> getCols() {
		return cols;
	}

	public boolean isAutoProps() {
		return autoProps;
	}

	public JTable makeTable(GroovySandbox sbx, Collection<?> values) {
		List<TableColumn> colDefs = getCols();
		boolean autoProps = isAutoProps();
		Collection<String> colNames = new LinkedHashSet<>();
		Map<String, TableColumn> colDefMap = new HashMap<>();
		colDefs.forEach(def -> colDefMap.put(def.name(), def));
		List<Map<String, Object>> processedValues = values.stream().map(val -> {
			Map<String, Object> colValues = new LinkedHashMap<>();
			colDefs.forEach(def -> colValues.put(def.name(), def.get(val)));
			if (autoProps) {
				Map props = DefaultGroovyMethods.getProperties(val);
				colValues.putAll(props);
			}
			colNames.addAll(colValues.keySet());
			return colValues;
		}).toList();
		CustomTableModel.CustomTableModelBuilder<Map<String, Object>> builder = CustomTableModel.builder(() -> processedValues);
		for (String col : colNames) {
			TableColumn def = colDefMap.get(col);
			builder = builder.addColumn(new CustomColumn<>(col, sbx.wrapFunc(val -> val.get(col)), c -> {
				c.setCellRenderer(new DefaultTableCellRenderer() {
					@Override
					public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
						try (var ignored = sbx.enter()) {
							value = convertValue(def, value);
							return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
						}
					}
				});
			}));
		}
		CustomTableModel<Map<String, Object>> model = builder.build();
		JTable table = model.makeTable();
		table.setAutoCreateRowSorter(true);
		// TODO: this doesn't do numbers correctly
//		table.setRowSorter(new TableRowSorter<>() {
//			@Override
//			public Comparator<?> getComparator(int column) {
//				return (a, b) -> {
//					if (a == null && b != null) {
//						return 1;
//					}
//					else if (a != null && b == null) {
//						return -1;
//					}
//					else if (a == null && b == null) {
//						return 0;
//					}
//					else if (a instanceof Number an && b instanceof Number bn) {
//						return Double.compare(an.doubleValue(), bn.doubleValue());
//					}
//					else {
//						return a.toString().compareTo(b.toString());
//					}
//				};
//			}
//		});
		return table;
	}

	// TODO: more custom conversion can be added here
	private Object convertValue(@Nullable TableColumn<?, ?, ?> colDef, @Nullable Object value) {
		if (value == null) {
			return null;
		}
		if (colDef != null) {
			Function disp = colDef.disp();
			if (disp != null) {
				try {
					return disp.apply(value);
				}
				catch (Throwable t) {
					return "Error in display func";
				}
			}
		}
		return typeDisplays.stream()
				.filter(td -> td.applicableTo(value))
				.findFirst()
				.map(td -> td.func().apply(value))
				.orElseGet(() -> singleValueConversion(value));
	}

	public static ListTableDisplay autoPropTable() {
		return new ListTableDisplay(List.of(), true);
	}

	public static ListTableDisplay defaultListDisplay() {
		return new ListTableDisplay(List.of(new TableColumn("toString", Object::toString)), false);
	}
}
