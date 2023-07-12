package gg.xp.xivsupport.gui.groovy;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class DisplayControl {


	public enum ListDisplayMode {
		AUTO,
		TABLE
	}

	private ListTableDisplay output;

	public ListTableDisplay getListDisplay() {
		if (output == null) {
			return ListTableDisplay.defaultListDisplay();
		}
		return output;
	}

	@Deprecated
	public void listAuto() {
		output = ListTableDisplay.defaultListDisplay();
	}

	@Deprecated
	public void listAsTable(String... columns) {
		output = new ListTableDisplay(Arrays.stream(columns).map(col -> new TableColumn(col, propGetter(col))).toList(), false);
	}

	@Deprecated
	public ListDisplayMode getListDisplayMode() {
		return ListDisplayMode.AUTO;
	}

	@Deprecated
	public List<String> getListTableColumns() {
		return Collections.emptyList();
	}

	public void table(@DelegatesTo(TableDSL.class) Closure<?> closure) {
		TableDSL delegate = new TableDSL();
		closure.setDelegate(delegate);
		closure.run();
		output = delegate.build();
	}

	private static class TableColumnPartial {
		String name;
		Function<?, ?> func;
		Function<?, ?> disp;
	}

	private static class TableTypePartial {
		Class<?> type;
		Function<?, ?> disp;
	}

	private static Object getProp(Object obj, String prop) {
		try {
			return DefaultGroovyMethods.getMetaClass(obj).getProperty(obj, prop);
		}
		catch (MissingPropertyException mpe) {
			return "Missing Property";
		}
		catch (Throwable t) {
			return "Error";
		}
	}

	private static Function<?, ?> propGetter(String prop) {
		return obj -> getProp(obj, prop);
	}

	public static class TableDSL {
		private final List<TableColumn> cols = new ArrayList<>();
		private final List<TypeDisplay> typeDisplays = new ArrayList<>();
		public boolean autoProps;

		private TableColumnPartial curCol;
		private TableTypePartial curType;

		private void flush() {
			if (curCol != null) {
				String name = curCol.name;
				Function<?, ?> func = curCol.func == null ? obj -> getProp(obj, name) : curCol.func;
				cols.add(new TableColumn(name, func, curCol.disp));
				curCol = null;
			}
			if (curType != null) {
				if (curType.disp == null) {
					throw new IllegalArgumentException("No display function specified for type '%s'".formatted(curType.type.getSimpleName()));
				}
				typeDisplays.add(new TypeDisplay(curType.type, curType.disp));
				curType = null;
			}
		}

		public TableDSL col(String name) {
			return column(name);
		}

		public TableDSL column(String name) {
			flush();
			curCol = new TableColumnPartial();
			curCol.name = name;
			return this;
		}

		public TableDSL prop(String prop) {
			return property(prop);
		}

		public TableDSL property(String prop) {
			curCol.func = obj -> getProp(obj, prop);
			return this;
		}

		public TableDSL func(Function<?, ?> func) {
			return function(func);
		}

		public TableDSL function(Function<?, ?> func) {
			curCol.func = func;
			return this;
		}

		public TableDSL disp(Function<?, ?> func) {
			return display(func);
		}

		public TableDSL display(Function<?, ?> func) {
			if (curCol != null) {
				curCol.disp = func;
			}
			else if (curType != null) {
				curType.disp = func;
			}
			else {
				throw new IllegalStateException("'display' doesn't make sense here");
			}
			return this;
		}

		public TableDSL allProps(boolean allProps) {
			this.autoProps = allProps;
			return this;
		}

		public TableDSL type(Class<?> type) {
			flush();
			curType = new TableTypePartial();
			curType.type = type;
			return this;
		}

		public ListTableDisplay build() {
			flush();
			return new ListTableDisplay(cols, autoProps, typeDisplays);
		}
	}


}
