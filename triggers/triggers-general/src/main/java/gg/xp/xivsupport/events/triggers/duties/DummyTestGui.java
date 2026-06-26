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
	private final DummyTestFight fight;

	public DummyTestGui(EventMaster master, DummyTestFight fight) {
		this.master = master;
		this.fight = fight;
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
		panel.add(new EasyAction("Test Without Event", fight::callDummyNoEvent).asButton());
		panel.add(new EasyAction("Test With Holds (Start)", fight::callDummy2on).asButton());
		panel.add(new EasyAction("Test With Holds (Stop)", fight::callDummy2off).asButton());
		panel.add(new EasyAction("Test With Vars", fight::callDummy3).asButton());
		panel.add(new EasyAction("Test With Icons", fight::callDummy4).asButton());
		panel.add(new EasyAction("Test With Delay", fight::callDummy5).asButton());
		panel.add(new EasyAction("Test With Overridden Delay", fight::callDummy5moreDelay).asButton());
		outer.add(panel, BorderLayout.NORTH);
		return outer;
	}
}
