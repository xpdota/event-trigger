package gg.xp.xivsupport.timelines;

import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class NullableEnumComboBox<X extends Enum<X>> extends JComboBox<X> {

	private @Nullable X selectedItem;

	public NullableEnumComboBox(Class<X> enumCls, String nullLabel, Consumer<@Nullable X> consumer, @Nullable X initialValue) {
		X[] constants = enumCls.getEnumConstants();
		List<X> list = new ArrayList<>(Arrays.asList(constants));
		list.add(0, null);
		selectedItem = initialValue;
		ComboBoxModel<X> model = new ComboBoxModel<>() {

			@Override
			public int getSize() {
				return list.size();
			}

			@Override
			public X getElementAt(int index) {
				return list.get(index);
			}

			@Override
			public void addListDataListener(ListDataListener l) {

			}

			@Override
			public void removeListDataListener(ListDataListener l) {

			}

			@Override
			public void setSelectedItem(Object anItem) {
				selectedItem = (X) anItem;
				consumer.accept((X) anItem);
			}

			@Override
			public Object getSelectedItem() {
				return selectedItem;
			}
		};
		setModel(model);
		setRenderer(new FriendlyNameListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				if (value == null) {
					return super.getListCellRendererComponent(list, nullLabel, index, isSelected, cellHasFocus);
				}
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});

	}

}
