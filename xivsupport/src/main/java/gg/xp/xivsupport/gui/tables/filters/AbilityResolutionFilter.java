package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

public class AbilityResolutionFilter implements VisualFilter<Event> {

	private final JComboBox<FilterOption> comboBox;
	private FilterOption currentOption = FilterOption.SNAPSHOT;

	public AbilityResolutionFilter(Runnable filterUpdatedCallback) {
		comboBox = new JComboBox<>(FilterOption.values());
		comboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				if (value instanceof FilterOption) {
					value = ((FilterOption) value).getName();
				}
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});
		comboBox.addItemListener(event -> {
			currentOption = (FilterOption) event.getItem();
			filterUpdatedCallback.run();
		});
	}

	@Override
	public boolean passesFilter(Event item) {
		return currentOption.test(item);
	}

	private enum FilterOption implements Predicate<Event> {
		SNAPSHOT("Snapshot", e -> !(e instanceof AbilityResolvedEvent)),
		RESOLVED("Resolved", e -> !(e instanceof AbilityUsedEvent)),
		BOTH("Both",  e -> true);
//		GHOSTED("Ghosted (TODO)",  e -> e.getParent() != null);


		// TODO
		private final String name;
		private final Predicate<Event> pred;

		FilterOption(String name, Predicate<Event> pred) {
			this.name = name;
			this.pred = pred;
		}

		@Override
		public boolean test(Event event) {
			return pred.test(event);
		}

		public String getName() {
			return name;
		}
	}

	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JLabel label = new JLabel("Snap/Resolve: ");
		label.setLabelFor(comboBox);
		panel.add(label);
		panel.add(comboBox);
		return panel;
	}
}
