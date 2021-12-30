package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.settings.LongSetting;

import javax.swing.*;
import java.awt.*;

public class LongSettingGui {

	private final TextFieldWithValidation<Long> textBox;
	private final String label;
	private JLabel jLabel;

	public LongSettingGui(LongSetting setting, String label) {
		textBox = new TextFieldWithValidation<>(Long::parseLong, setting::set, Long.toString(setting.get()));
		this.label = label;
	}

	public Component getTextBoxOnly() {
		return textBox;
	}

	public Component getLabelOnly() {
		if (jLabel == null) {
			jLabel = new JLabel(label);
			jLabel.setLabelFor(textBox);
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
