package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class DragonsongGui implements PluginTab {

	private final Dragonsong ds;

	public DragonsongGui(Dragonsong ds) {
		this.ds = ds;
	}

	@Override
	public String getTabName() {
		return "DSR Automarks";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Dragonsong Automarks");
		JCheckBox p6marks = new BooleanSettingGui(ds.getP6_useAutoMarks(), "P6 Wroth Flames Automarks").getComponent();
		ReadOnlyText helpText = new ReadOnlyText("""
				The four players with the 'spread' debuffs will receive 'attack' markers.
				In addition, if Telesto is in use:
				The two players with 'stack' debuffs will receive 'bind1' and 'ignore1' markers,
				and the two players with nothing will receive 'bind2' and 'ignore2' markers.
				""");

		GuiUtil.simpleTopDownLayout(outer, 500, p6marks, helpText);
		return outer;
	}

	@Override
	public int getSortOrder() {
		return 101;
	}
}
