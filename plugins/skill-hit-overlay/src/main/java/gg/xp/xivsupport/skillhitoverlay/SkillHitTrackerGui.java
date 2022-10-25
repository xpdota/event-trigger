package gg.xp.xivsupport.skillhitoverlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.extra.SimplePluginTab;
import gg.xp.xivsupport.gui.extra.TopDownSimplePluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class SkillHitTrackerGui extends TopDownSimplePluginTab {

	private final SkillHitTracker overlay;

	public SkillHitTrackerGui(SkillHitTracker overlay) {
		super("Skill Hit Tracker", 400);
		this.overlay = overlay;
	}

	@Override
	public int getSortOrder() {
		return 105;
	}

	@Override
	protected Component[] provideChildren(JPanel outer) {
		BooleanSetting enabledSetting = overlay.getEnabled();
		JCheckBox enabledCb = new BooleanSettingGui(enabledSetting, "Overlay Enabled", true).getComponent();
		JPanel rowSpinner = new IntSettingSpinner(overlay.getNumberOfRows(), "Number of Rows", enabledSetting::get).getComponent();
		JPanel intervalSpinner = new IntSettingSpinner(overlay.getUpdateInterval(), "Interval (ms)", enabledSetting::get).getComponent();
		Component textColor = new ColorSettingGui(overlay.getFg(), "Text Color", enabledSetting::get).getComponentReversed();
		Component bgColor = new ColorSettingGui(overlay.getBg(), "Background Color", enabledSetting::get).getComponentReversed();
		return new Component[]{enabledCb, rowSpinner, intervalSpinner, textColor, bgColor};
	}
}
