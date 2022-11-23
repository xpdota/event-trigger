package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.RearrangeableEnumListSetting;
import gg.xp.xivsupport.gui.components.RearrangeableList;
import gg.xp.xivsupport.gui.extra.DutyPluginTab;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class UptimeExaflareGui implements DutyPluginTab {

	private final DragonsongUptimeExas backend;

	public UptimeExaflareGui(DragonsongUptimeExas backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Uptime Exaflares";
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel panel = new TitleBorderFullsizePanel("Uptime Exaflares");
		JCheckBox cb = new BooleanSettingGui(backend.getCalloutGroupEnabledSetting(), "Enabled", true).getComponent();
		RearrangeableList<UptimeExaflareMovement> listGui = new RearrangeableEnumListSetting<>(backend.getPriority()).getListGui();
		listGui.setCellRenderer(new FriendlyNameListCellRenderer());
		JLabel label = GuiUtil.labelFor("Priority (drag and drop to rearrange):", listGui);
		GuiUtil.simpleTopDownLayout(panel, cb, Box.createVerticalStrut(10), label, listGui);
		return panel;
	}

	@Override
	public KnownDuty getDuty() {
		return KnownDuty.Dragonsong;
	}
}
