package gg.xp.xivsupport.gui;

import javax.swing.*;
import java.awt.*;

class WrapperPanel extends JPanel {

	WrapperPanel(Component component) {
		setLayout(new FlowLayout());
//		setBorder(new LineBorder(Color.MAGENTA));
		add(component);
		setMaximumSize(getPreferredSize());
	}

}
