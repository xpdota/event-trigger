package gg.xp.xivsupport.custompartyoverlay.selection;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.EnumSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;

import javax.swing.*;
import java.awt.*;

public class SelectionComponentGui extends JPanel {

	public SelectionComponentGui(SelectionComponentConfig backend) {
		// TODO: enable/disable status
		var enableBorder = new BooleanSettingGui(backend.getEnableBorder(), "Draw Border", true).getComponent();
		var borderColor = new ColorSettingGui(backend.getBorderColor(), "Border Color", () -> true).getComponent();
		var borderThickness = new IntSettingSpinner(backend.getBorderThickness(), "Border Thickness", () -> true).getComponent();
		var radiusX = new IntSettingSpinner(backend.getBorderRadiusX(), "Roundness (X)", () -> true).getComponent();
		var radiusY = new IntSettingSpinner(backend.getBorderRadiusY(), "Roundness (Y)", () -> true).getComponent();
		var enableBg = new BooleanSettingGui(backend.getEnableBg(), "Draw Background", true).getComponent();
		var bgColor = new ColorSettingGui(backend.getBgColor(), "Background Color", () -> true).getComponent();
		var enableGradient = new BooleanSettingGui(backend.getEnableGradient(), "Enable Gradient", true).getComponent();
		var gradAngle = new IntSettingSpinner(backend.getGradientAngle(), "Gradient Angle", () -> true).getComponent();
		var gradStart = new IntSettingSpinner(backend.getGradientStart(), "Gradient Start (px)", () -> true).getComponent();
		var gradLength = new IntSettingSpinner(backend.getGradientLength(), "Gradient Length (px)", () -> true).getComponent();
		GuiUtil.simpleTopDownLayout(this, enableBorder, borderColor, borderThickness, radiusX, radiusY, enableBg, bgColor, enableGradient, gradAngle, gradStart, gradLength);
	}
}
