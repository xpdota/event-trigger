package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.persistence.settings.DoubleSetting;

import javax.swing.*;

@SuppressWarnings("SerializableStoresNonSerializable")
public class DoubleSettingSlider {

	private final JSlider slider;

	public DoubleSettingSlider(String name, DoubleSetting setting, double increment, boolean listen) {
		double min = setting.getMin();
		double range = setting.getMax() - min;
		int increments = (int) ((range / increment));
		slider = new JSlider(0, increments) {
			@Override
			public String getToolTipText() {
				return String.format("%s: %s (Valid range: %s - %s)", name, setting.get(), setting.getMin(), setting.getMax());
			}
		};
		slider.setValue((int) ((setting.get() - min) / increment));
		// TODO: write value
		slider.addChangeListener(e -> setting.set(slider.getValue() * increment + min));
		slider.setSnapToTicks(true);
		if (listen) {
			setting.addListener(() -> SwingUtilities.invokeLater(() -> slider.setValue((int) ((setting.get() - min) / increment))));
		}
	}

	public JSlider getComponent() {
		return slider;
	}
}
