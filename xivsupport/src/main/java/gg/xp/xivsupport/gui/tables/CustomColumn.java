package gg.xp.xivsupport.gui.tables;

import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableColumn;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomColumn<X> {

	private final String columnName;
	private final Function<X, Object> getter;
	private final Consumer<TableColumn> columnConfigurer;

	public CustomColumn(String columnName, Function<X, @Nullable Object> getter) {
		this(columnName, getter, ignored -> {
		});
	}

	/**
	 * Custom table column
	 *
	 * @param columnName       Name of the column
	 * @param getter           Function to convert an instance in the table to whatever this column cares about. Note that this
	 *                         is executed **in the table rendering code** so it should under no circumstances involve
	 *                         non-trivial computation or access.
	 * @param columnConfigurer Lets you configure the column, e.g. to override the renderer.
	 */
	public CustomColumn(String columnName, Function<X, @Nullable Object> getter, Consumer<TableColumn> columnConfigurer) {
		this.columnName = columnName;
		this.getter = getter;
		this.columnConfigurer = columnConfigurer;
	}

	public CustomColumn(String columnName, Function<X, @Nullable Object> getter, int fixedWidth) {
		this.columnName = columnName;
		this.getter = getter;
		this.columnConfigurer = c -> {
			c.setMinWidth(fixedWidth);
			c.setMaxWidth(fixedWidth);
		};
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

	public CustomColumn<X> withExtraConfig(Consumer<TableColumn> extraColumnConfigurer) {
		return new CustomColumn<>(columnName, getter, c -> {
			columnConfigurer.accept(c);
			extraColumnConfigurer.accept(c);
		});
	}
}
