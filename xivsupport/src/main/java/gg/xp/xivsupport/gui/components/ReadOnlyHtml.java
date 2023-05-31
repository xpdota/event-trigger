package gg.xp.xivsupport.gui.components;

import org.intellij.lang.annotations.Language;

import javax.swing.*;
import java.awt.*;

public class ReadOnlyHtml extends JTextPane {

	// Implementation notes: In order to get this to behave the way we want to under most circumstances
	// (i.e. stretch to fill available width), we need to:
	// 1. Lock the preferred size
	// 2. Report minimum size as the greater of true minimum vs original preferred. Otherwise, if you expand the window
	// a bit horizontally, it won't fill the new space.
	public ReadOnlyHtml(@Language("html") String html) {
		setContentType("text/html");
		setText(html);
		super.setPreferredSize(super.getPreferredSize());
		setEditable(false);
		setBorder(null);
		setOpaque(false);
		setFocusable(false);
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension min = super.getMinimumSize();
		Dimension act = super.getPreferredSize();
		return new Dimension(Math.max(min.width, act.width), Math.max(min.height, act.height));
	}
}
