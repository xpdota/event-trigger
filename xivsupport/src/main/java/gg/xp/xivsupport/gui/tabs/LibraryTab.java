package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.NpcYellTableFactory;
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
	private final TableWithFilterAndDetails<NpcYellInfo, Object> npcYellTable;

	public LibraryTab(ActionTableFactory atf, NpcYellTableFactory nytf) {
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
		{
			npcYellTable = nytf.table();
			addTab("Npc Yells", npcYellTable);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			abilityTable.signalNewData();
			statusTable.signalNewData();
			zonesTable.signalNewData();
			rsvTable.signalNewData();
			npcYellTable.signalNewData();
		}
		super.setVisible(visible);
	}
}
