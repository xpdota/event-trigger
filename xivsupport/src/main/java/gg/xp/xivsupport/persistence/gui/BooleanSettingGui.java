package gg.xp.xivsupport.persistence.gui;

import com.formdev.flatlaf.icons.FlatCheckBoxIcon;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class BooleanSettingGui {

	private final JCheckBox checkBox;
	private final BooleanSetting setting;
	private volatile boolean ignoreChanges;

	public BooleanSettingGui(BooleanSetting setting, String label, boolean listen) {
		this(setting, label, listen, () -> true);
	}

	public BooleanSettingGui(BooleanSetting setting, String label, boolean listen, Supplier<Boolean> enabled) {
		this.setting = setting;
		checkBox = new JCheckBox(label) {
			@Override
			public boolean isEnabled() {
				return super.isEnabled() && enabled.get();
			}

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
			}
		};

		FlatCheckBoxIcon icon = new FlatCheckBoxIcon() {
			// flatlaf 1.x
//			@Override
//			protected Color getCheckmarkColor(Component c, boolean selected, boolean isFocused) {
//				return colorize(super.getCheckmarkColor(c, selected, isFocused));
//			}

			// flatlaf 2.x and above
			@Override
			protected Color getCheckmarkColor(Component c) {
				return colorize(super.getCheckmarkColor(c));
			}

			@Override
			protected Color getBorderColor(Component c, boolean selected) {
				return colorize(super.getBorderColor(c, selected));
			}

			@Override
			protected Color getFocusColor(Component c) {
				Color fc = super.getFocusColor(c);
				if (isOverriding()) {
					return new Color(fc.getBlue(), fc.getGreen(), fc.getRed());
				}
				else {
					return fc;
				}
			}
		};
		checkBox.setIcon(icon);
		checkBox.setSelectedIcon(icon);
		checkBox.setSelected(setting.get());
		checkBox.addItemListener(l -> {
			if (!ignoreChanges) {
				setting.set(checkBox.isSelected());
			}
		});
		if (listen) {
			setting.addListener(this::refresh);
		}
		checkBox.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, this::reset));
		refresh();
	}

	private void reset() {
		setting.delete();
		refresh();
	}

	private boolean isOverriding() {
		return (setting.isSet()) && setting.hasParent();
	}

	private Color colorize(Color dflt) {
		if (isOverriding()) {
//			return new Color(dflt.getRed(), dflt.getGreen(), Math.min(dflt.getBlue() + 40, 255));
			return new Color(Math.min(dflt.getRed() + 40, 255), dflt.getGreen(), dflt.getBlue());
		}
		else {
			return dflt;
		}
	}

	private void refresh() {
		SwingUtilities.invokeLater(() -> {
			ignoreChanges = true;
			try {
				checkBox.getModel().setSelected(setting.get());
//				checkBox.setBackground(setting.isSet() ? Color.PINK : null);
				checkBox.repaint();
			}
			finally {
				ignoreChanges = false;
			}
		});
	}


	public BooleanSettingGui(BooleanSetting setting, String label) {
		this(setting, label, true, () -> true);
	}

	public BooleanSettingGui(BooleanSetting setting, String label, Supplier<Boolean> enabled) {
		this(setting, label, true, enabled);
	}

	public JCheckBox getComponent() {
		return checkBox;
	}
}
