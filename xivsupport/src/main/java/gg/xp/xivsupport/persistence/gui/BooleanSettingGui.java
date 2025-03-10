package gg.xp.xivsupport.persistence.gui;

import com.formdev.flatlaf.icons.FlatCheckBoxIcon;
import gg.xp.xivsupport.gui.util.ColorUtils;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * GUI component provider for a boolean setting, including a label and checkbox.
 */
public class BooleanSettingGui {

	private final JCheckBox checkBox;
	private final BooleanSetting setting;
	private volatile boolean ignoreChanges;


	/**
	 * @param setting The BooleanSetting
	 * @param label   The user-facing label to display
	 */
	public BooleanSettingGui(BooleanSetting setting, String label) {
		this(setting, label, true, () -> true);
	}

	/**
	 * @param setting The BooleanSetting
	 * @param label   The user-facing label to display
	 * @param enabled Condition for whether this setting is enabled
	 */
	public BooleanSettingGui(BooleanSetting setting, String label, Supplier<Boolean> enabled) {
		this(setting, label, true, enabled);
	}

	/**
	 * @param setting The BooleanSetting
	 * @param label   The user-facing label to display
	 * @param listen  Whether to listen to the setting and reflect any outside changes to the setting
	 */
	public BooleanSettingGui(BooleanSetting setting, String label, boolean listen) {
		this(setting, label, listen, () -> true);
	}

	/**
	 * @param setting The BooleanSetting
	 * @param label   The user-facing label to display
	 * @param listen  Whether to listen to the setting and reflect any outside changes to the setting
	 * @param enabled Condition for whether this setting is enabled
	 */
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
					// Swap the blue and green channels
					return ColorUtils.modifiedSettingColor(fc);
//					return new Color(fc.getRed(), fc.getBlue(), fc.getGreen());
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
			return new Color(Math.max(dflt.getRed() - 40, 0), Math.min(dflt.getGreen() + 60, 255), Math.min(dflt.getBlue() + 10, 255));
//			return ColorUtils.modifiedSettingColor(dflt);
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


	/**
	 * @return The actual component
	 */
	public JCheckBox getComponent() {
		return checkBox;
	}
}
