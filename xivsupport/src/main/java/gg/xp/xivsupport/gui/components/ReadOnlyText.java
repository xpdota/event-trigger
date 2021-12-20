package gg.xp.xivsupport.gui.components;

import javax.swing.*;

public class ReadOnlyText extends JTextArea {
	public ReadOnlyText(String text) {
		super(text);
		setEditable(false);
		setBorder(null);
		setOpaque(false);
		setWrapStyleWord(true);
		setLineWrap(true);
		setFocusable(false);
	}
}
