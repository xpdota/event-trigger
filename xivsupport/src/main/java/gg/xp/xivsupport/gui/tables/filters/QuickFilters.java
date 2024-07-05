package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.scan.ScanMe;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@ScanMe
public class QuickFilters {

	private final Object lock = new Object();
	private final List<QuickFilter<?>> all = new ArrayList<>();

	public void register(QuickFilter<?> filter) {
		synchronized (lock) {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).name().equals(filter.name())) {
					all.set(i, filter);
					return;
				}
			}
			all.add(filter);
		}
	}

	public <X> void register(String name, Class<X> filterClass, Predicate<X> filterFunc) {
		register(new QuickFilter<X>() {
			@Override
			public Class<X> filterClass() {
				return filterClass;
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public boolean test(X x) {
				return filterFunc.test(x);
			}
		});
	}

	public <X> List<QuickFilter<X>> getFiltersForType(Class<X> filterClass) {
		synchronized (lock) {
			//noinspection unchecked
			return all.stream()
					.filter(filter -> filter.appliesToType(filterClass))
					.map(filter -> (QuickFilter<X>) filter)
					.toList();
		}
	}

	@SuppressWarnings("unused")
	public QuickFilter<?> getNamedFilter(String name) {
		synchronized (lock) {
			return all.stream()
					.filter(filter -> filter.name().equals(name))
					.findFirst()
					.orElse(null);
		}
	}

	public <X> VisualFilter<X> makeWidget(Class<X> eventClass, Runnable filterUpdatedCallback) {
		var model = new ComboBoxModel<>() {

			private @Nullable QuickFilter<X> current;
			private List<QuickFilter<X>> filters = new ArrayList<>();

			{
				update();
			}

			private void update() {
				filters = getFiltersForType(eventClass);
			}

			@Override
			public int getSize() {
				update();
				// +1 for the null option
				return filters.size() + 1;
			}

			@Override
			public @Nullable Object getElementAt(int index) {
				if (index == 0) {
					return null;
				}
				// -1 for the null option
				return filters.get(index - 1);
			}

			@Override
			public void addListDataListener(ListDataListener l) {

			}

			@Override
			public void removeListDataListener(ListDataListener l) {

			}

			@Override
			public void setSelectedItem(Object anItem) {
				current = (QuickFilter<X>) anItem;
				filterUpdatedCallback.run();
			}

			@Override
			public Object getSelectedItem() {
				return current;
			}
		};
		var out = new JComboBox<>(model);
		out.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				if (value instanceof QuickFilter<?> qf) {
					return super.getListCellRendererComponent(list, ((QuickFilter<?>) value).name(), index, isSelected, cellHasFocus);
				}
				else if (value == null) {
					return super.getListCellRendererComponent(list, "None", index, isSelected, cellHasFocus);
				}
				else {
					return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				}
			}
		});
		return new VisualFilter<X>() {
			@Override
			public boolean passesFilter(X item) {
				QuickFilter<X> current = model.current;
				if (current == null) {
					return true;
				}
				return current.test(item);
			}

			@Override
			public Component getComponent() {
				JPanel panel = new JPanel();
				panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
				JLabel label = new JLabel("Quick Filter:");
				label.setLabelFor(out);
				panel.add(label);
				panel.add(out);
				return panel;
			}
		};
	}
}
