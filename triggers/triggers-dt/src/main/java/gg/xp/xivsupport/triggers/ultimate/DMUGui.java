package gg.xp.xivsupport.triggers.ultimate;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.settings.EnumSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class DMUGui implements DutyPluginTab {

	private final DMU backend;

	public DMUGui(DMU backend) {
		this.backend = backend;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.DMU;
	}

	@Override
	public String getTabName() {
		return "Settings";
	}

	@Override
	public Component getTabContents() {
		EnumSetting<DMU.CleanseCallOption> cleanseCallSetting = backend.getCleanseCallSetting();
		EnumSettingGui<DMU.CleanseCallOption> cleanseSettingGui = new EnumSettingGui<>(cleanseCallSetting, "Earthquake Cleanse Calls: ", () -> true);

		var outer = new TitleBorderFullsizePanel("DMU Settings");
		GuiUtil.simpleTopDownLayout(outer, cleanseSettingGui.getComponent());
		return outer;
	}
}
