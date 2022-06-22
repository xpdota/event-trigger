package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.settings.IntSetting;

import javax.swing.*;
import java.awt.*;

public class IntSettingGui {

	private final TextFieldWithValidation<Integer> textBox;
	private final String label;
	private JLabel jLabel;

	private Boolean labelAtLeft;

	public IntSettingGui(IntSetting setting, String label) {
		this(setting, label, false);
	}

	public IntSettingGui(IntSetting setting, String label, Boolean labelAtLeft) {
		textBox = new TextFieldWithValidation<>(Integer::parseInt, setting::set, Long.toString(setting.get()));
		this.label = label;
		this.labelAtLeft = labelAtLeft;
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
		if (labelAtLeft) {
			box.add(getLabelOnly());
			box.add(getTextBoxOnly());
		} else {
			box.add(getTextBoxOnly());
			box.add(getLabelOnly());
		}
		box.setMaximumSize(box.getPreferredSize());
		return box;
	}
}
