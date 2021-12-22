package gg.xp.xivsupport.events.triggers.marks.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkHandler;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import java.awt.*;

@ScanMe
public class AutoMarkGui implements PluginTab {

	private final AutoMarkHandler marks;

	public AutoMarkGui(AutoMarkHandler marks) {
		this.marks = marks;
	}

	@Override
	public String getTabName() {
		return "AutoMark";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Marks");
		Component toggle = new BooleanSettingGui(marks.getUseFkeys(), "Use F1-F9 (Instead of NumPad 1-9)").getComponent();
		outer.add(toggle);
		return outer;
	}
}
