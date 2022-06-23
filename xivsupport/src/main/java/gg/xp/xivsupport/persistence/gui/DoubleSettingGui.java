package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.persistence.settings.DoubleSetting;

import javax.swing.*;
import java.awt.*;

public class DoubleSettingGui {

	private final TextFieldWithValidation<Double> textBox;
	private final String label;
	private JLabel jLabel;

	public DoubleSettingGui(DoubleSetting setting, String label) {
		textBox = new TextFieldWithValidation<>(Double::parseDouble, setting::set, Double.toString(setting.get()));
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
