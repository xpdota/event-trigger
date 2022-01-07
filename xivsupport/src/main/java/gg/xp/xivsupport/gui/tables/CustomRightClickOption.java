package gg.xp.xivsupport.gui.tables;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CustomRightClickOption {
	private final String label;
	private final Predicate<CustomTableModel<?>> shouldAppear;
	private final Consumer<CustomTableModel<?>> action;

	private CustomRightClickOption(String label, Predicate<CustomTableModel<?>> shouldAppear, Consumer<CustomTableModel<?>> action) {
		this.label = label;
		this.shouldAppear = shouldAppear;
		this.action = action;
	}

	@SuppressWarnings("unchecked")
	public static <X> CustomRightClickOption forRow(String label, Class<X> expectedClass, Consumer<X> action) {
		return new CustomRightClickOption(label, t -> expectedClass.isInstance(getTableRowValue(t)), t -> {
			Object actualObject = getTableRowValue(t);
			if (expectedClass.isInstance(actualObject)) {
				action.accept((X) actualObject);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public static <X, Y> CustomRightClickOption forRowWithConverter(String label, Class<X> initialClass, Function<X, @Nullable Y> conversion, Consumer<Y> action) {
		return new CustomRightClickOption(label, t -> {
			Object rowVal = getTableRowValue(t);
			return initialClass.isInstance(rowVal) && conversion.apply((X) rowVal) != null;
		}, t -> {
			Object rowVal = getTableRowValue(t);
			if (!initialClass.isInstance(rowVal)) {
				return;
			}
			Y actualObject = conversion.apply((X) rowVal);
			action.accept(actualObject);
		});
	}

	private static <X> X getTableRowValue(CustomTableModel<X> table) {
		return table.getSelectedValue();
	}

	public String getLabel() {
		return label;
	}

	public boolean shouldAppear(CustomTableModel<?> object) {
		return shouldAppear.test(object);
	}

	public void doAction(CustomTableModel<?> object) {
		action.accept(object);
	}
}
