package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class ColorSettingGui {

	private final JButton button;
	private final JLabel label;
	private final ColorSetting setting;

	public ColorSettingGui(ColorSetting setting, String label, Supplier<Boolean> enabled) {
		this.setting = setting;
		this.label = new JLabel(label);
		this.button = new JButton("") {
			@Override
			public Color getBackground() {
				return setting.get();
			}

			@Override
			public boolean isEnabled() {
				return enabled.get();
			}
		};

		button.addActionListener(l -> {
			Color color = JColorChooser.showDialog(button, label, setting.get(), true);
			setting.set(color);
			button.repaint();
		});
		button.setPreferredSize(new Dimension(50, 20));
		button.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, this::reset));
	}


	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel.add(label);
		panel.add(button);
		return panel;
	}

	private void reset() {
		setting.delete();
		button.repaint();
	}


}
