package gg.xp.xivsupport.custompartyoverlay.name;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;

import javax.swing.*;
import java.awt.*;

public class NameComponentGui extends JPanel {

	public NameComponentGui(NameComponentConfig backend) {
		JCheckBox shadow = new BooleanSettingGui(backend.getDropShadow(), "Shadow", true).getComponent();
		Component color = new ColorSettingGui(backend.getFontColor(), "Font Color", () -> true).getComponent();
		Component alignment = new EnumSettingGui<>(backend.getAlignment(), "Text Alignment", () -> true).getComponent();
		GuiUtil.simpleTopDownLayout(this, shadow, color, alignment);
	}
}
