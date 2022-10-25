package gg.xp.xivsupport.custompartyoverlay.castbar;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;

import javax.swing.*;
import java.awt.*;

public class CastBarComponentGui extends JPanel {

	public CastBarComponentGui(CastBarComponentConfig backend) {
		Component bg = new ColorSettingGui(backend.getBackgroundColor(), "Background", () -> true).getComponentReversed();
		Component inProgress = new ColorSettingGui(backend.getInProgressColor(), "In Progress", () -> true).getComponentReversed();
		Component success = new ColorSettingGui(backend.getSuccessColor(), "Success", () -> true).getComponentReversed();
		Component interrupted = new ColorSettingGui(backend.getInterruptedColor(), "Interrupted", () -> true).getComponentReversed();
		Component unknown = new ColorSettingGui(backend.getUnknownColor(), "Unknown", () -> true).getComponentReversed();
		Component text = new ColorSettingGui(backend.getTextColor(), "Text", () -> true).getComponentReversed();
		GuiUtil.simpleTopDownLayout(this, bg, inProgress, success, interrupted, unknown, text);
	}
}
