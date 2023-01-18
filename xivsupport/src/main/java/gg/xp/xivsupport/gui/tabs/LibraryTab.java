package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.RsvTable;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.library.ZonesTable;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.rsv.RsvEntry;

import javax.swing.*;

@ScanMe
public class LibraryTab extends JTabbedPane {

	private final TableWithFilterAndDetails<ActionInfo, Object> abilityTable;
	private final TableWithFilterAndDetails<StatusEffectInfo, Object> statusTable;
	private final TableWithFilterAndDetails<ZoneInfo, Object> zonesTable;
	private final TableWithFilterAndDetails<RsvEntry, Object> rsvTable;

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
		{
			zonesTable = ZonesTable.table();
			addTab("Zones", zonesTable);
		}
		{
			rsvTable = RsvTable.table();
			addTab("RSV Entries", rsvTable);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			abilityTable.signalNewData();
			statusTable.signalNewData();
			zonesTable.signalNewData();
			rsvTable.signalNewData();
		}
		super.setVisible(visible);
	}
}
