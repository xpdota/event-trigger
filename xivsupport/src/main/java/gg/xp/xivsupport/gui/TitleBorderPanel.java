package gg.xp.xivsupport.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TitleBorderPanel extends JPanel {
	public TitleBorderPanel(String title) {
		setBorder(new TitledBorder(title));
	}

	public TitleBorderPanel(String title, Component... components) {
		this(title);
		for (Component component : components) {
			add(component);
		}
	}
}
