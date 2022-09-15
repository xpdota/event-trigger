package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.conversions.DutySpecificArenaSectorConverter;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArenaSectorConverterGui extends TitleBorderFullsizePanel {
	public ArenaSectorConverterGui(DutySpecificArenaSectorConverter asc) {
		super("Arena Positions");
		List<Component> components = new ArrayList<>();

		components.add(new JLabel("Custom Values (leave blank to use global defaults):"));
		for (var entry : asc.getPerSectorSettings().entrySet()) {
			ArenaSector sector = entry.getKey();
			components.add(
					new StringSettingGui(entry.getValue(),
							sector == ArenaSector.UNKNOWN ? "Unknown/Error" : sector.getFriendlyName(),
							() -> true).getComponent());
		}

		GuiUtil.simpleTopDownLayout(this, 400, components.toArray(Component[]::new));

	}
}
