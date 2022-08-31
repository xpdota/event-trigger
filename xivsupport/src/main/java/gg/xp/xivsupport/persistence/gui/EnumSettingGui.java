package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.tables.filters.AbilityResolutionFilter;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class EnumSettingGui<X extends Enum<X>> {

	private final JComboBox<X> comboBox;
	private final JLabel label;
	private final EnumSetting<X> setting;

	public EnumSettingGui(EnumSetting<X> setting, String label, Supplier<Boolean> enabled) {
		this.setting = setting;
		Class<X> type = setting.getEnumType();
		comboBox = new JComboBox<>(type.getEnumConstants()) {
			@Override
			public boolean isEnabled() {
				return enabled.get();
			}
		};
		comboBox.setRenderer(new FriendlyNameListCellRenderer());
		comboBox.setSelectedItem(setting.get());
		comboBox.addItemListener(event -> {
			setting.set((X) event.getItem());
		});
		comboBox.setComponentPopupMenu(ResetMenuOption.resetOnlyMenu(setting, this::reset));
		this.label = new JLabel(label);
		this.label.setLabelFor(comboBox);
	}

	private void reset() {
		comboBox.setSelectedItem(setting.getDefault());
		setting.delete();
	}

	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel.add(label);
		panel.add(comboBox);
		return panel;
	}

	public JComboBox<X> getComboBoxOnly() {
		return comboBox;
	}

}
