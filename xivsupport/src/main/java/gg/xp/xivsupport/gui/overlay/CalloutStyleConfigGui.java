package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.CalloutProcessor;
import gg.xp.xivsupport.callouts.gui.CalloutsConfigTab;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.FontSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class CalloutStyleConfigGui implements PluginTab {

	private final FlyingTextOverlay overlay;
	private final CalloutProcessor calloutProcessor;

	public CalloutStyleConfigGui(FlyingTextOverlay overlay, CalloutProcessor calloutProcessor) {
		this.overlay = overlay;
		this.calloutProcessor = calloutProcessor;
	}


	@Override
	public String getTabName() {
		return "Callout Styling";
	}

	@Override
	public Component getTabContents() {
		JTabbedPane tpane = new JTabbedPane();
		{
			JPanel panel = new TitleBorderFullsizePanel("Visual Callouts");
//			panel.setLayout(new GridBagLayout());
			BooleanSetting enabled = overlay.getEnabled();
			enabled.addListener(panel::repaint);

			JCheckBox enableDisable = new BooleanSettingGui(enabled, "Flying Text Enabled").getComponent();
			Component alignment = new EnumSettingGui<>(overlay.getAlignmentSetting(), "Text Alignment", enabled::get).getComponent();
			Component color = new ColorSettingGui(overlay.getTextColorSetting(), "Text Color", enabled::get).getComponent();
			Component font = new FontSettingGui(overlay.getTextFontSetting(), "Text Font", GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).getComponent();


			GuiUtil.simpleTopDownLayout(panel, 400, enableDisable, alignment, color, font);
			tpane.add("Visual Callouts", panel);
		}
		{
			JPanel panel = new TitleBorderFullsizePanel("Name Conversions");
			JCheckBox replaceYou = new BooleanSettingGui(calloutProcessor.getReplaceYou(), "Replace your own name with 'YOU'").getComponent();
			Component playerNameStylePanel = new EnumSettingGui<>(calloutProcessor.getPcNameStyle(), "Player Name Style", () -> true).getComponent();
			GuiUtil.simpleTopDownLayout(panel, 400, replaceYou, playerNameStylePanel);
			tpane.add("Name Conversions", panel);
		}
		return tpane;
	}

	@Override
	public int getSortOrder() {
		return 2;
	}
}
