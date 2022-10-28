package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;

public class BuffsBarComponentGui extends JPanel {

	public BuffsBarComponentGui(BuffsBarConfig backend) {
		BooleanSetting timers = backend.getTimers();
		GuiUtil.simpleTopDownLayout(
				this,
				new BooleanSettingGui(timers, "Enable Timers", true).getComponent(),
				new ColorSettingGui(backend.getNormalTextColor(), "Text Color (Normal)", timers::get).getComponentReversed(),
				new ColorSettingGui(backend.getMyBuffTextColor(), "Text Color (Own Buffs)", timers::get).getComponentReversed(),
				new ColorSettingGui(backend.getRemoveableBuffColor(), "Text Color (Removable)", timers::get).getComponentReversed(),
				new BooleanSettingGui(backend.getShadows(), "Shadows on Text", timers::get).getComponent(),
				new IntSettingSpinner(backend.getxPadding(), "Spacing between buffs").getComponent(),
				new BooleanSettingGui(backend.getShowFcBuffs(), "Show FC Buffs").getComponent(),
				new BooleanSettingGui(backend.getShowFoodBuff(), "Show Food Buffs").getComponent()
		);
		timers.addAndRunListener(this::repaint);
	}
}
