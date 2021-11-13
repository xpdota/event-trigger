package gg.xp.gui.tables;

import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableColumn;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomColumn<X> {

	private final String columnName;
	private final Function<X, Object> getter;
	private final Consumer<TableColumn> columnConfigurer;

	// TODO: custom renderers
	public CustomColumn(String columnName, Function<X, @Nullable Object> getter) {
		this(columnName, getter, ignored -> {});
	}

	public CustomColumn(String columnName, Function<X, @Nullable Object> getter, Consumer<TableColumn> columnConfigurer) {
		this.columnName = columnName;
		this.getter = getter;
		this.columnConfigurer = columnConfigurer;
	}

	public String getColumnName() {
		return columnName;
	}

	public Object getValue(X item) {
		return getter.apply(item);
	}

	public void configureColumn(TableColumn column) {
		columnConfigurer.accept(column);
	}
}
