package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;

public class BooleanSettingGui {

	private final JCheckBox checkBox;

	public BooleanSettingGui(BooleanSetting setting, String label, boolean listen) {
		checkBox = new JCheckBox(label);
		checkBox.setSelected(setting.get());
		checkBox.addItemListener(l -> {
			setting.set(checkBox.isSelected());
		});
		if (listen) {
			setting.addListener(() -> SwingUtilities.invokeLater(() -> checkBox.setSelected(setting.get())));
		}
	}

	public BooleanSettingGui(BooleanSetting setting, String label) {
		this(setting, label, true);
	}

	public JCheckBox getComponent() {
		return checkBox;
	}
}
