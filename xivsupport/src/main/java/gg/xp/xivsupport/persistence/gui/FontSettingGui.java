package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.persistence.settings.FontSetting;
import gg.xp.xivsupport.persistence.settings.ResetMenuOption;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import java.io.Serializable;
import java.util.function.Supplier;

public class FontSettingGui {

	private final ListSettingGui<String> fontNameGui;
	private final BooleanSettingGui boldGui;
	private final BooleanSettingGui italicGui;
	private final IntSettingGui sizeGui;
	private final FontSetting setting;

	public FontSettingGui(FontSetting setting, String label, String[] fontNames) {
		this.setting = setting;
		this.fontNameGui = new ListSettingGui<>(setting.getFontName(), label, fontNames);
		this.sizeGui = new IntSettingGui(setting.getSize(), "Size", true);
		this.boldGui = new BooleanSettingGui(setting.getBold(), "Bold");
		this.italicGui = new BooleanSettingGui(setting.getItalic(), "Italic");
	}

	public Component getComponent() {
		JPanel panel = new JPanel(new GridLayout(3, 1));
		panel.add(fontNameGui.getComponent());
		JPanel sizePanel = sizeGui.getComponent();
		sizePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(sizePanel);
		JPanel stylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		stylePanel.add(boldGui.getComponent());
		stylePanel.add(italicGui.getComponent());
		panel.add(stylePanel);
		return panel;
	}
}

