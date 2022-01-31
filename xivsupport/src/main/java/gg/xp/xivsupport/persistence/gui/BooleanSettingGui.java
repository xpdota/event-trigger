package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.util.function.Supplier;

public class BooleanSettingGui {

	private final JCheckBox checkBox;

	public BooleanSettingGui(BooleanSetting setting, String label, boolean listen) {
		this(setting, label, listen, () -> true);
	}

	public BooleanSettingGui(BooleanSetting setting, String label, boolean listen, Supplier<Boolean> enabled) {
		checkBox = new JCheckBox(label) {
			@Override
			public boolean isEnabled() {
				return super.isEnabled() && enabled.get();
			}
		};
		checkBox.setSelected(setting.get());
		checkBox.addItemListener(l -> {
			setting.set(checkBox.isSelected());
		});
		if (listen) {
			setting.addListener(() -> SwingUtilities.invokeLater(() -> checkBox.setSelected(setting.get())));
		}
	}

	public BooleanSettingGui(BooleanSetting setting, String label) {
		this(setting, label, true, () -> true);
	}

	public BooleanSettingGui(BooleanSetting setting, String label, Supplier<Boolean> enabled) {
		this(setting, label, true, enabled);
	}

	public JCheckBox getComponent() {
		return checkBox;
	}
}
