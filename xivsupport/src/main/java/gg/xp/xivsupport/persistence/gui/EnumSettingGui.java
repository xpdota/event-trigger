package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.function.Supplier;

public class EnumSettingGui<X extends Enum<X>> {

	private static final Logger log = LoggerFactory.getLogger(EnumSettingGui.class);

	private final JComboBox<X> comboBox;
	private final JLabel label;
	private final EnumSetting<X> setting;
	private volatile boolean ignoreChange;

	public EnumSettingGui(EnumSetting<X> setting, String label, Supplier<Boolean> enabled) {
		this(setting, label, enabled, false);
	}

	public EnumSettingGui(EnumSetting<X> setting, String label, Supplier<Boolean> enabled, boolean listen) {
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
			log.info("{} {}", event.getStateChange(), event.getItem());
			if (event.getStateChange() == ItemEvent.SELECTED) {
				ignoreChange = true;
				setting.set((X) event.getItem());
				ignoreChange = false;
			}
		});
		if (listen) {
			setting.addListener(() -> {
				log.info("{} {}", ignoreChange, setting.get());
				if (!ignoreChange) {
					comboBox.setSelectedItem(setting.get());
				}
			});
		}
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

	public JLabel getLabel() {
		return label;
	}
}
