package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class StringSettingGui {

	private final TextFieldWithValidation<String> checkBox;
	private final String label;
	private JLabel jLabel;

	public StringSettingGui(StringSetting setting, String label) {
		checkBox = new TextFieldWithValidation<>(Function.identity(), setting::set, setting.get());
		this.label = label;
	}

	public Component getTextBoxOnly() {
		return checkBox;
	}

	public Component getLabelOnly() {
		if (jLabel == null) {
			jLabel = new JLabel(label);
			jLabel.setLabelFor(checkBox);
		}
		return jLabel;
	}

	public JPanel getComponent() {
		JPanel box = new JPanel();
		box.setLayout(new WrapLayout());
		box.add(getTextBoxOnly());
		box.add(getLabelOnly());
		box.setMaximumSize(box.getPreferredSize());
		return box;
	}
}
