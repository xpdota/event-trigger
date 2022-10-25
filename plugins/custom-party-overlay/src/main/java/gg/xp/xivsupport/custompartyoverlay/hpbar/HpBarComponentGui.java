package gg.xp.xivsupport.custompartyoverlay.hpbar;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;

import javax.swing.*;
import java.awt.*;

public class HpBarComponentGui extends JPanel {

	public HpBarComponentGui(HpBarComponentConfig backend) {
		Component bg = new ColorSettingGui(backend.getBackgroundColor(), "Background", () -> true).getComponentReversed();
		Component shield = new ColorSettingGui(backend.getShieldColor(), "Shield", () -> true).getComponentReversed();
		Component gEmpty = new ColorSettingGui(backend.getHpGradientEmpty(), "HP, Empty Blend", () -> true).getComponentReversed();
		Component gFull = new ColorSettingGui(backend.getHpGradientFull(), "HP, Full Blend", () -> true).getComponentReversed();
		Component full = new ColorSettingGui(backend.getFullHpColor(), "HP, Full", () -> true).getComponentReversed();
		Component textColor = new ColorSettingGui(backend.getTextColor(), "Text Color", () -> true).getComponentReversed();
		Component bgOpacity = new IntSettingSpinner(backend.getBgTransparency(), "Background Opacity").getComponent();
		Component fgOpacity = new IntSettingSpinner(backend.getFgTransparency(), "Foreground Opacity").getComponent();
		Component fractionMode = new EnumSettingGui<>(backend.getFractionDisplayMode(), "Fraction Mode", () -> true).getComponent();
		GuiUtil.simpleTopDownLayout(this, bg, shield, gEmpty, gFull, full, textColor, bgOpacity, fgOpacity, fractionMode);
	}
}
