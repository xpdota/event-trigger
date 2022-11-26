package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class DragonsongLegacyGui implements PluginTab {


	private final GlobalUiRegistry reg;

	public DragonsongLegacyGui(GlobalUiRegistry reg) {
		this.reg = reg;
	}

	@Override
	public String getTabName() {
		return "DSR Automarks";
	}

	@Override
	public Component getTabContents() {
		JPanel panel = new JPanel();
		panel.add(new ReadOnlyText("This has moved to the new Duties tab!"));
		JButton button = new JButton("Take Me There!");
		button.addActionListener(l -> reg.activateItem(DragonsongAmGui.class));
		panel.add(button);
		return panel;
	}
}
