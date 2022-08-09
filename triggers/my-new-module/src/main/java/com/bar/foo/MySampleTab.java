package com.bar.foo;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class MySampleTab implements PluginTab {
	@Override
	public String getTabName() {
		return "My New Plugin";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("My Sample Plugin Tab");
		panel.add(new JLabel("It Works!"));
		return panel;
	}
}
