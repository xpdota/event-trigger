package gg.xp.xivsupport.gui.addonmgr;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tabs.AddonDef;
import gg.xp.xivsupport.gui.tabs.UpdaterConfig;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;

import javax.swing.*;
import java.awt.*;

@ScanMe
public class AddonMgr implements PluginTab {

	private final UpdaterConfig updaterConfig;

	@Override
	public String getTabName() {
		return "Manage Addons";
	}

	@Override
	public int getSortOrder() {
		return -50_000;
	}

	public AddonMgr(UpdaterConfig updaterConfig) {
		this.updaterConfig = updaterConfig;
	}

	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outer = new TitleBorderFullsizePanel("Manage Addons");
		CustomJsonListSetting<AddonDef> addonSetting = updaterConfig.getAddonSetting();

		CustomTableModel<AddonDef> model = CustomTableModel.builder(addonSetting::getItems)
				.addColumn(new CustomColumn<>("Icon", item -> item.iconUrl))
				.addColumn(new CustomColumn<>("Name", item -> item.name))
				.addColumn(new CustomColumn<>("URL", item -> item.url))
				.build();
		addonSetting.addListener(model::signalNewData);
		JTable table = model.makeTable();
		table.setRowHeight(100);

		return outer;
	}




}
