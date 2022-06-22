package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.persistence.settings.ValueSetting;

import javax.swing.*;
import java.awt.*;

public class ListSettingGui<X> {

	private final JComboBox<X> comboBox;
	private final JLabel label;
	private final ValueSetting<X> setting;

	public ListSettingGui(ValueSetting<X> setting, String label, X[] values) {
		this.setting = setting;
		this.comboBox = new JComboBox<X>(values);
		this.comboBox.setRenderer(new FriendlyNameListCellRenderer());
		this.comboBox.addItemListener(event -> {
			this.setting.set((X) event.getItem());
		});
		this.comboBox.setSelectedItem(setting.get());
		this.comboBox.addItemListener(l -> {
			this.setting.set((X)comboBox.getSelectedItem());
		});
		this.label = new JLabel(label);
		this.label.setLabelFor(comboBox);
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
