package gg.xp.xivsupport.gui.extra;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderPanel;

import javax.swing.*;
import java.awt.*;

@ScanMe
public abstract class SimplePluginTab implements PluginTab {

	protected final String name;

	protected SimplePluginTab(String name) {
		this.name = name;
	}

	@Override
	public String getTabName() {
		return name;
	}

	@Override
	public Component getTabContents() {
		TitleBorderPanel outer = new TitleBorderPanel(name);
		configurePanel(outer);
		return outer;
	}

	protected abstract void configurePanel(JPanel outer);
}
