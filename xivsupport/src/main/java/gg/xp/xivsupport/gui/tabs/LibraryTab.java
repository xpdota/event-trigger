package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.ActionInfo;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;

import javax.swing.*;

@ScanMe
public class LibraryTab extends JTabbedPane {

	private final TableWithFilterAndDetails<ActionInfo, Object> abilityTable;
	private final TableWithFilterAndDetails<StatusEffectInfo, Object> statusTable;

	public LibraryTab(ActionTableFactory atf) {
		super(LEFT);
		{
			abilityTable = atf.table();
			addTab("Actions/Abilities", abilityTable);
		}
		{
			statusTable = StatusTable.table();
			addTab("Status Effects", statusTable);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			abilityTable.signalNewData();
			statusTable.signalNewData();
		}
		super.setVisible(visible);
	}
}
