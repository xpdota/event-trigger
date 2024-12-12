package gg.xp.xivsupport.gui.components;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

// TODO: still bugged
// Maybe a better approach is to listen for a resize and:
// 1. Recalculate the new preferred height using the new width (possibly tapping into font metrics)
// 2. Set this new preferred size (don't just return it via an override), so that it triggers a revalidation
public class ReadOnlyText extends JTextArea {

	private @Nullable Dimension fakePreferredSize;

	// Implementation notes: In order to get this to behave the way we want to under most circumstances
	// (i.e. stretch to fill available width), we need to:
	// 1. Lock the preferred size
	// 2. Report minimum size as the greater of true minimum vs original preferred. Otherwise, if you expand the window
	// a bit horizontally, it won't fill the new space.
	public ReadOnlyText(String text, boolean lockPrefSize) {
		super(text);
		if (lockPrefSize) {
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

	@Override
	public Dimension getPreferredSize() {
		Dimension fps = fakePreferredSize;
		if (fps != null) {
			return fps;
		}
		return super.getPreferredSize();
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		Rectangle currentBounds = getBounds();
		int currentWidth = currentBounds.width;
//		boolean doResize = width - currentWidth > 3 || width - currentWidth < 0;
		super.setBounds(x, y, width, height);
		if (!isPreferredSizeSet()) {
			fakePreferredSize = super.getPreferredSize();
		}
	}
}
