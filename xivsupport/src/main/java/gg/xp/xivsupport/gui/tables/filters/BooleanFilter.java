package gg.xp.xivsupport.gui.tables.filters;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * A tri-state boolean filter that can be in one of three states: Any, True, or False.
 * This is useful for filtering on boolean properties where the user may want to see either state or all items.
 *
 * @param <X> The type of object being filtered.
 */
public class BooleanFilter<X> implements VisualFilter<X> {

	private final JComboBox<TriState> comboBox;
	private final String label;
	private final Function<X, Boolean> valueGetter;
	private TriState currentOption;

	public enum TriState {
		ANY("Any"),
		TRUE("True"),
		FALSE("False");

		private final String friendlyName;

		TriState(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		@Override
		public String toString() {
			return friendlyName;
		}
	}

	public BooleanFilter(Runnable filterUpdatedCallback, String label, Function<X, Boolean> valueGetter) {
		this.label = label;
		this.valueGetter = valueGetter;
		comboBox = new JComboBox<>(TriState.values());
		comboBox.addActionListener(event -> {
			currentOption = (TriState) comboBox.getSelectedItem();
			filterUpdatedCallback.run();
		});
		currentOption = TriState.ANY;
	}

	@Override
	public boolean passesFilter(X item) {
		if (currentOption == TriState.ANY) {
			return true;
		}
		Boolean value = valueGetter.apply(item);
		boolean boolValue = value != null && value;
		return (currentOption == TriState.TRUE) == boolValue;
	}

	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JLabel jLabel = new JLabel(label + ": ");
		jLabel.setLabelFor(comboBox);
		panel.add(jLabel);
		panel.add(comboBox);
		return panel;
	}

	@Override
	public Component getHeaderComponent() {
		return comboBox;
	}
}
