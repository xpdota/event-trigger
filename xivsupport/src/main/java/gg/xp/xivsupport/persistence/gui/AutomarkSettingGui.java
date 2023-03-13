package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.persistence.settings.AutomarkSetting;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AutomarkSettingGui {

	private final JComboBox<MarkerSign> picker;
	private final JCheckBox enabler;

	public AutomarkSettingGui(AutomarkSetting setting, @Nullable String label) {
		picker = new EnumSettingGui<>(setting.getWhichMark(), null, () -> setting.getEnabled().get(), true).getComboBoxOnly();
		enabler = new BooleanSettingGui(setting.getEnabled(), label).getComponent();
		enabler.setHorizontalTextPosition(SwingConstants.LEFT);
		setting.addListener(picker::updateUI);
	}

	public JComboBox<MarkerSign> getPicker() {
		return picker;
	}

	public JCheckBox getCheckbox() {
		return enabler;
	}

	public JPanel getCombined() {
		JPanel out = new JPanel();
		out.setLayout(new BorderLayout());
		out.add(enabler, BorderLayout.WEST);
		out.add(picker, BorderLayout.CENTER);
		return out;
	}

	public JPanel getCombinedLeftPad() {
		JPanel out = new JPanel();
		out.setLayout(new BorderLayout());
		enabler.setHorizontalAlignment(SwingConstants.RIGHT);
		out.add(enabler, BorderLayout.CENTER);
		out.add(picker, BorderLayout.EAST);
		return out;
	}

}
