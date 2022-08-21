package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class DragonsongLegacyGui implements PluginTab {

	private final DragonsongAmGui realGui;

	public DragonsongLegacyGui(DragonsongAmGui realGui) {
		this.realGui = realGui;
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
		button.addActionListener(l -> realGui.tryBringToFront());
		panel.add(button);
		return panel;
	}
}
