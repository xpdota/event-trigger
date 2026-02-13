package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.util.EasyAction;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class DummyTestGui implements DutyPluginTab {

	private final EventMaster master;

	public DummyTestGui(EventMaster master) {
		this.master = master;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.None;
	}

	@Override
	public String getTabName() {
		return "Test Callouts";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Test Callouts");
		outer.setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.add(new EasyAction("Test With Event", () -> master.pushEvent(new DebugCommand("testcall"))).asButton());
		panel.add(new EasyAction("Test Without Event", () -> master.pushEvent(new DebugCommand("testcall2"))).asButton());
		panel.add(new EasyAction("Test With Holds (Start)", () -> master.pushEvent(new DebugCommand("testcall_on"))).asButton());
		panel.add(new EasyAction("Test With Holds (Stop)", () -> master.pushEvent(new DebugCommand("testcall_off"))).asButton());
		panel.add(new EasyAction("Test With Vars", () -> master.pushEvent(new DebugCommand("testcall3"))).asButton());
		outer.add(panel, BorderLayout.NORTH);
		return outer;
	}
}
