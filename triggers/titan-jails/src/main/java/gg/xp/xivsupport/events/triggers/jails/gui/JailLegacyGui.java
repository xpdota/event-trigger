package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class JailLegacyGui implements PluginTab {

	private final JailGui realGui;

	public JailLegacyGui(JailGui realGui) {
		this.realGui = realGui;
	}

	@Override
	public String getTabName() {
		return "Titan Jails";
	}

	@Override
	public Component getTabContents() {
		JPanel panel = new JPanel();
		panel.add(new ReadOnlyText("This has moved to the new Duties tab!"));
		JButton button = new JButton("Take Me There!");
		button.addActionListener(l -> realGui.tryBringToFront());
		panel.add(button);
		return panel;
	}
}
