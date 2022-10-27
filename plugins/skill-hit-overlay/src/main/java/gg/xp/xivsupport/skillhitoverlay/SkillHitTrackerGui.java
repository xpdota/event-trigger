package gg.xp.xivsupport.skillhitoverlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.extra.SimplePluginTab;
import gg.xp.xivsupport.gui.extra.TopDownSimplePluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.apache.maven.model.plugin.LifecycleBindingsInjector;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

@ScanMe
public class SkillHitTrackerGui extends TopDownSimplePluginTab {

	private final SkillHitTracker overlay;

	public SkillHitTrackerGui(SkillHitTracker overlay) {
		super("Skill Hit Tracker", 306);
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
		JPanel rowSpinner = new IntSettingSpinner(overlay.getNumberOfRows(), "Number of Rows", () -> true).getComponent();
		JPanel intervalSpinner = new IntSettingSpinner(overlay.getUpdateInterval(), "Interval (ms)", () -> true).getComponent();
		Component textColor = new ColorSettingGui(overlay.getFg(), "Text Color", () -> true). getComponentReversed();
		Component bgColor = new ColorSettingGui(overlay.getBg(), "Background Color", () -> true).getComponentReversed();
		JCheckBox snapCb = new BooleanSettingGui(overlay.getUseSnapshot(), "Use Snapshots", true).getComponent();
		ReadOnlyText text = new ReadOnlyText("Using snapshots (rather than when the damage actually applies) may work better for some users, but will count ghosted abilities. Turn this on if you are having issues with abilities not registering.");
		Component[] components = {enabledCb, rowSpinner, intervalSpinner, textColor, bgColor, snapCb, text};
		for (int i = 1; i < components.length; i++) {
			Component component = components[i];
			enabledSetting.addAndRunListener(() -> component.setVisible(enabledSetting.get()));
		}
		enabledSetting.addAndRunListener(outer::revalidate);
		return components;
	}
}
