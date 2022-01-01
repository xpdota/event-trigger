package gg.xp.xivsupport.gui;

import javax.swing.*;
import java.awt.*;

public class WrapperPanel extends JPanel {

	public WrapperPanel(Component component) {
		setLayout(new FlowLayout());
//		setBorder(new LineBorder(Color.MAGENTA));
		add(component);
		setMaximumSize(getPreferredSize());
	}

}
