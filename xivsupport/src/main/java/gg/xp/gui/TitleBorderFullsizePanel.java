package gg.xp.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;

// TODO: system for plugins to install their own guis
public class TitleBorderFullsizePanel extends JPanel {
	public TitleBorderFullsizePanel(String title) {
		setBorder(new TitledBorder(title));
		setPreferredSize(getMinimumSize());
	}
}
