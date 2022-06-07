package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class FlyingTextOverlayConfig implements PluginTab {

	private final FlyingTextOverlay overlay;

	public FlyingTextOverlayConfig(FlyingTextOverlay overlay) {
		this.overlay = overlay;
	}


	@Override
	public String getTabName() {
		return "Visual Callouts";
	}

	@Override
	public Component getTabContents() {
		JPanel panel = new TitleBorderFullsizePanel("Visual Callouts");
		panel.setLayout(new GridBagLayout());
		BooleanSetting enabled = overlay.getEnabled();
		enabled.addListener(panel::repaint);

		JCheckBox enableDisable = new BooleanSettingGui(enabled, "Flying Text Enabled").getComponent();
		Component alignment = new EnumSettingGui<>(overlay.getAlignmentSetting(), "Text Alignment", enabled::get).getComponent();
		Component color = new ColorSettingGui(overlay.getTextColorSetting(), "Text Color", enabled::get).getComponent();

		GuiUtil.simpleTopDownLayout(panel, enableDisable, alignment, color);
		return panel;
	}

	@Override
	public int getSortOrder() {
		return 2;
	}
}
