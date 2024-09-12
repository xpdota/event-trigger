package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.ColorUtils;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;
import gg.xp.xivsupport.persistence.settings.StringSetting;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class StringSettingGui {

	private final TextFieldWithValidation<String> textBox;
	private final StringSetting setting;
	private final String label;
	// For a reset, we want to ignore the property change, otherwise we won't truly reset it
	private volatile boolean resetInProgress;
	private JLabel jLabel;

	public StringSettingGui(StringSetting setting, String label) {
		this(setting, label, () -> true);
	}

	public StringSettingGui(StringSetting setting, String label, Supplier<Boolean> enabled) {
		// TODO: pull all this logic out so it can be used elsewhere
		textBox = new TextFieldWithValidation<>(Function.identity(), this::setNewValue, setting.get()) {
			@Override
			public Color getForeground() {
				Color defaultColor = super.getForeground();
				if (setting.isSet()) {
					return ColorUtils.modifiedSettingColor(defaultColor);
				}
				return defaultColor;
			}

			@Override
			public Color getDisabledTextColor() {
				Color defaultColor = super.getForeground();
				if (setting.isSet()) {
					return ColorUtils.modifiedSettingColor(defaultColor);
				}
				return defaultColor;
			}

			private volatile boolean wasEnabled;

			@Override
			public boolean isEnabled() {
				boolean nowEnabled = super.isEnabled() && enabled.get();
				if (nowEnabled != wasEnabled) {
					// this is so fucking bad
					// Of all things, the text box background seems to not update on a repaint, so we have to force it
					// if there was a change by calling updateUI().
					wasEnabled = nowEnabled;
					SwingUtilities.invokeLater(this::updateUI);
				}
				return nowEnabled;
			}
		};
		this.setting = setting;
		textBox.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, this::reset));
		this.label = label;
	}

	private void setNewValue(String newValue) {
		if (!resetInProgress) {
			setting.set(newValue);
		}
	}

	private void reset() {
		resetInProgress = true;
		try {
			textBox.setText(setting.getDefault());
			setting.delete();
		}
		finally {
			resetInProgress = false;
		}
	}

	public JTextField getTextBoxOnly() {
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
