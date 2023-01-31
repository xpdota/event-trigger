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
		picker = new EnumSettingGui<>(setting.getWhichMark(), null, () -> setting.getEnabled().get()).getComboBoxOnly();
		enabler = new BooleanSettingGui(setting.getEnabled(), label).getComponent();
		enabler.setHorizontalTextPosition(SwingConstants.LEFT);
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
//		out.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		out.add(enabler, BorderLayout.WEST);
//		out.add(Box.createHorizontalStrut(5));
		out.add(picker, BorderLayout.CENTER);
		return out;
	}

}
