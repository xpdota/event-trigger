package gg.xp.xivsupport.events.triggers.jails.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.jails.JailSolver;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import java.awt.*;

@ScanMe
public class JailGui implements PluginTab {

	private final JailSolver jails;

	public JailGui(JailSolver jails) {

		this.jails = jails;
	}

	@Override
	public String getTabName() {
		return "Titan Gaols";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Jails");
		panel.add(new BooleanSettingGui(jails.getEnableTts(), "Enable Personal Callout").getComponent());
		panel.add(new BooleanSettingGui(jails.getEnableAutomark(), "Enable Automarks").getComponent());
		return panel;
	}

}
