package gg.xp.xivsupport.gui.tables;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
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

	public static void configureTable(JTable table, CustomTableModel<?> model, List<CustomRightClickOption> options) {
		if (options.isEmpty()) {
			return;
		}
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				popupMenu.removeAll();
				for (CustomRightClickOption rightClickOption : options) {
					if (rightClickOption.shouldAppear(model)) {

						JMenuItem menuItem = new JMenuItem(rightClickOption.getLabel());
						menuItem.addActionListener(l -> {
							rightClickOption.doAction(model);
						});
						popupMenu.add(menuItem);
					}
				}
				popupMenu.revalidate();

			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					JTable source = (JTable) e.getSource();
					int row = source.rowAtPoint(e.getPoint());
					int column = source.columnAtPoint(e.getPoint());

					if (!source.isRowSelected(row)) {
						source.changeSelection(row, column, false, false);
					}
				}
			}
		});
		table.setComponentPopupMenu(popupMenu);

	}
}
