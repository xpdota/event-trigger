package gg.xp.xivsupport.gui.extra;

import gg.xp.xivsupport.gui.util.GuiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class TopDownSimplePluginTab extends SimplePluginTab {

	private final int width;

	protected TopDownSimplePluginTab(String name, int width) {
		super(name);
		this.width = width;
	}

	@Override
	protected void configurePanel(JPanel outer) {
		GuiUtil.simpleTopDownLayout(outer, width, provideChildren(outer));

	}

	protected abstract Component[] provideChildren(JPanel outer);
}
