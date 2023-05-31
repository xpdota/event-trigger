package gg.xp.xivsupport.gui.components;

import javax.swing.*;
import java.awt.*;

public class ReadOnlyText extends JTextArea {

	// Implementation notes: In order to get this to behave the way we want to under most circumstances
	// (i.e. stretch to fill available width), we need to:
	// 1. Lock the preferred size
	// 2. Report minimum size as the greater of true minimum vs original preferred. Otherwise, if you expand the window
	// a bit horizontally, it won't fill the new space.
	public ReadOnlyText(String text, boolean setPrefSize) {
		super(text);
		if (setPrefSize) {
			super.setPreferredSize(super.getPreferredSize());
		}
		setEditable(false);
		setBorder(null);
		setOpaque(false);
		setWrapStyleWord(true);
		setLineWrap(true);
		setFocusable(false);
	}

	public ReadOnlyText(String text) {
		this(text, true);
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension min = super.getMinimumSize();
		Dimension act = super.getPreferredSize();
		return new Dimension(Math.max(min.width, act.width), Math.max(min.height, act.height));
	}
}
