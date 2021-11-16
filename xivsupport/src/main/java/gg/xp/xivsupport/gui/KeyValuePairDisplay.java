package gg.xp.xivsupport.gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class KeyValuePairDisplay<C extends Component, D> extends JPanel implements Refreshable {

	private final C component;
	private final Supplier<D> dataGetter;
	private final BiConsumer<C, D> guiUpdater;
	private final JLabel label;

	public KeyValuePairDisplay(String labelText, C component, Supplier<D> dataGetter, BiConsumer<C, D> guiUpdater) {
		this.component = component;
		this.dataGetter = dataGetter;
		this.guiUpdater = guiUpdater;
		label = new JLabel(labelText);
		label.setMinimumSize(label.getPreferredSize());
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setLabelFor(component);
//			add(label);
//			add(component);
	}

	public void refresh() {
		D newData = dataGetter.get();
		guiUpdater.accept(component, newData);
	}

	public JPanel getComponent() {
		JPanel panel = new JPanel();
		panel.add(component);
		return panel;
	}

	public JLabel getLabel() {
		return label;
	}
}
