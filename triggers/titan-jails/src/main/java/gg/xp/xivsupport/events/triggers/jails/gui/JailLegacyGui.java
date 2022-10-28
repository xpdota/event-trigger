package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tabs.GlobalUiRegistry;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class JailLegacyGui implements PluginTab {

	private final GlobalUiRegistry reg;

	public JailLegacyGui(GlobalUiRegistry reg) {
		this.reg = reg;
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
		button.addActionListener(l -> reg.activateItem(JailLegacyGui.class));
		panel.add(button);
		return panel;
	}
}
