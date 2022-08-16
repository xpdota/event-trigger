package gg.xp.xivsupport.custompartyoverlay.mpbar;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;

import javax.swing.*;
import java.awt.*;

public class MpBarComponentGui extends JPanel {

	public MpBarComponentGui(MpBarComponentConfig backend) {
		Component bg = new ColorSettingGui(backend.getBackgroundColor(), "Background", () -> true).getComponentReversed();
		Component mp = new ColorSettingGui(backend.getMpColor(), "Bar", () -> true).getComponentReversed();
		Component textColor = new ColorSettingGui(backend.getTextColor(), "Text Color", () -> true).getComponentReversed();
		Component bgOpacity = new IntSettingSpinner(backend.getBgTransparency(), "Background Opacity").getComponent();
		Component fgOpacity = new IntSettingSpinner(backend.getFgTransparency(), "Foreground Opacity").getComponent();
		Component fractionMode = new EnumSettingGui<>(backend.getFractionDisplayMode(), "Fraction Mode", () -> true).getComponent();
		GuiUtil.simpleTopDownLayout(this, bg, mp, textColor, bgOpacity, fgOpacity, fractionMode);
	}
}
