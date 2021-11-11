package gg.xp.gui.tables;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class CustomColumn<X> {

	private final String columnName;
	private final Function<X, Object> getter;

	// TODO: custom renderers
	public CustomColumn(String columnName, Function<X, @Nullable Object> getter) {
		this.columnName = columnName;
		this.getter = getter;
	}

	public String getColumnName() {
		return columnName;
	}

	public Object getValue(X item) {
		return getter.apply(item);
	}
}
