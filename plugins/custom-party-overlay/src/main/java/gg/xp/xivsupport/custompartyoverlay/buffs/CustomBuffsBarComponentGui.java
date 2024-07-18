package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.lists.ItemList;
import gg.xp.xivsupport.gui.tables.renderers.StatusListCellRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.LongListSetting;

import javax.swing.*;
import java.awt.*;

public class CustomBuffsBarComponentGui extends JPanel {

	public CustomBuffsBarComponentGui(CustomBuffsBarConfig backend) {
		super(new BorderLayout());
		BooleanSetting timers = backend.getTimers();
		LongListSetting setting = backend.getBuffs();
		StatusListCellRenderer statusRenderer = new StatusListCellRenderer();
		JPanel controls = new JPanel();
		JPanel statusList = ItemList.multiSelect(
				() -> setting.get().stream()
						.map(XivStatusEffect::new)
						.toList(),
				effects -> setting.set(effects.stream().map(XivStatusEffect::getId).sorted().toList()),
				statusRenderer,
				() -> StatusTable.pickItems(SwingUtilities.getWindowAncestor(this))
						.stream()
						.map(sei -> new XivStatusEffect(sei.statusEffectId()))
						.toList()
		);
		statusList.setMinimumSize(new Dimension(200, 1));
		statusList.setPreferredSize(new Dimension(300, 300));
		GuiUtil.simpleTopDownLayout(
				controls,
				new BooleanSettingGui(timers, "Enable Timers", true).getComponent(),
				new ColorSettingGui(backend.getNormalTextColor(), "Text Color (Normal)", timers::get).getComponentReversed(),
				new ColorSettingGui(backend.getMyBuffTextColor(), "Text Color (Own Buffs)", timers::get).getComponentReversed(),
				new ColorSettingGui(backend.getRemoveableBuffColor(), "Text Color (Removable)", timers::get).getComponentReversed(),
				new BooleanSettingGui(backend.getShadows(), "Shadows on Text", timers::get).getComponent(),
				new BooleanSettingGui(backend.getRtl(), "Right Align").getComponent(),
				new IntSettingSpinner(backend.getxPadding(), "Spacing between buffs").getComponent(),
				new BooleanSettingGui(backend.getShowPreapps(), "Show Snapshotted Buffs (Beta)").getComponent(),
				new IntSettingSpinner(backend.getPreappOpacity(), "Opacity for Snapshotted Buffs").getComponent()
		);
		timers.addAndRunListener(this::repaint);
		add(controls, BorderLayout.NORTH);
		add(statusList, BorderLayout.WEST);
	}
}
