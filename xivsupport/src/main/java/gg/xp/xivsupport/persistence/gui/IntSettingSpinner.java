package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.persistence.settings.IntSetting;

import javax.swing.*;
import java.awt.*;

public class IntSettingSpinner {

	private final JSpinner spinner;
	private final String label;
	private JLabel jLabel;

	// TODO: something weird going on with insets with this class
	public IntSettingSpinner(IntSetting setting, String label) {
		SpinnerNumberModel model = new SpinnerNumberModel();
		model.setValue(setting.get());
		model.addChangeListener(e -> {
			Integer newValue = (Integer) model.getValue();
			if (newValue != setting.get()) {
				setting.set(newValue);
			}
		});
		Integer min;
		if ((min = setting.getMin()) != null) {
			model.setMinimum(min);
		}
		Integer max;
		if ((max = setting.getMax()) != null) {
			model.setMaximum(max);
		}
		spinner = new JSpinner(model);
		this.label = label;
	}

	public Component getSpinnerOnly() {
		return spinner;
	}

	public Component getLabelOnly() {
		if (jLabel == null) {
			jLabel = new JLabel(label);
			jLabel.setLabelFor(spinner);
		}
		return jLabel;
	}

	public JPanel getComponent() {
		JPanel box = new JPanel();
		box.setLayout(new WrapLayout(FlowLayout.CENTER, 5, 0));
		box.add(getSpinnerOnly());
		box.add(getLabelOnly());
		box.setMaximumSize(box.getPreferredSize());
		return box;
	}
}
