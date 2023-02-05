package gg.xp.xivsupport.persistence.gui;

import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;

import javax.swing.*;
import java.awt.*;

/**
 * Class for quickly building a panel that contains many constituent auto mark assignment settings
 * from one single MultiSlotAutomarkSettings setting.
 *
 * @param <X> The enum type of the MultiSlotAutomarkSetting
 */
public class BasicAutomarkSettingGroupGui<X extends Enum<X>> extends TitleBorderPanel {

	/**
	 * @param title          A title to put on this panel
	 * @param bigSetting     The automark group setting
	 * @param perRowOrColumn How many rows (if transpose is false) or columns (if transpose is true)
	 * @param transpose      If false, the first setting will be in the top-left cell, then it will move to the right
	 *                       until the desired amount per row has been attained. Then, it will move to the left-most
	 *                       cell on the next row, and repeat.
	 *                       If true, the first setting will still be the top-left cell, but it will move down
	 *                       until the desired amount per column has been attained. Then, it will move to the top-most
	 *                       cell on the next column, and repeat.
	 */
	public BasicAutomarkSettingGroupGui(String title, MultiSlotAutomarkSetting<X> bigSetting, int perRowOrColumn, boolean transpose) {
		super(title);
//		Class<X> enumCls = bigSetting.getEnumCls();
		setLayout(new GridBagLayout());
		GridBagConstraints mc = GuiUtil.defaultGbc();
		mc.ipadx = 5;
		mc.gridy = 0;
//		mc.anchor = GridBagConstraints.EAST;

		bigSetting.getSettings().forEach((enumMember, setting) -> {
			String name = enumMember instanceof HasFriendlyName hfn ? hfn.getFriendlyName() : enumMember.name();
			JPanel settingComp = new AutomarkSettingGui(setting, name).getCombinedLeftPad();
			int ord = enumMember.ordinal();
			int x = ord % perRowOrColumn;
			int y = ord / perRowOrColumn;

			int effX = transpose ? y : x;
			int effY = transpose ? x : y;

			mc.gridx = effX;
			mc.gridy = effY;
//			add(label, mc);
//			mc.gridx++;
			add(settingComp, mc);
		});
		bigSetting.addListener(this::repaint);
	}

}
