package gg.xp.xivsupport.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TitleBorderFullsizePanel extends JPanel {
	public TitleBorderFullsizePanel(String title) {
		setBorder(new TitledBorder(title));
		setPreferredSize(getMinimumSize());
	}

	public TitleBorderFullsizePanel(String title, Component... components) {
		this(title);
		for (Component component : components) {
			add(component);
		}
	}
}
