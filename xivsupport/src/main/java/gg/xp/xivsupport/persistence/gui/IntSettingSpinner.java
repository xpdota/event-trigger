package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class IntSettingSpinner {

	private final JSpinner spinner;
	private final String label;
	private volatile boolean changePending;
	private JLabel jLabel;

	private final Boolean labelAtLeft;

	public IntSettingSpinner(IntSetting setting, String label) {
		this(setting, label, () -> true);
	}

	// TODO: something weird going on with insets with this class
	public IntSettingSpinner(IntSetting setting, String label, Supplier<Boolean> enabled) {
		this(setting, label, enabled, false);
	}

	public IntSettingSpinner(IntSetting setting, String label, Supplier<Boolean> enabled, boolean labelAtLeft) {
		SpinnerNumberModel model = new SpinnerNumberModel();
		model.setValue(setting.get());
		model.addChangeListener(e -> {
			if (changePending) {
				return;
			}
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
		spinner = new JSpinner(model) {
			@Override
			public boolean isEnabled() {
				return enabled.get();
			}
		};
		this.label = label;
		this.labelAtLeft = labelAtLeft;
		setting.addListener(() -> model.setValue(setting.get()));
		spinner.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, () -> {
			changePending = true;
			setting.delete();
			changePending = false;
		}));

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
		if (labelAtLeft) {
			box.add(getLabelOnly());
			box.add(getSpinnerOnly());
		} else {
			box.add(getSpinnerOnly());
			box.add(getLabelOnly());
		}
		box.setMinimumSize(box.getPreferredSize());
		box.setMaximumSize(box.getPreferredSize());
		return box;
	}
}
