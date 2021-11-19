package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.persistence.BooleanSetting;

import javax.swing.*;

public class BooleanSettingGui {

	private final JCheckBox checkBox;

	public BooleanSettingGui(BooleanSetting setting, String label) {
		checkBox = new JCheckBox(label);
		checkBox.setSelected(setting.get());
		checkBox.addItemListener(l -> {
			setting.set(checkBox.isSelected());
		});
	}

	public JCheckBox getComponent() {
		return checkBox;
	}
}
