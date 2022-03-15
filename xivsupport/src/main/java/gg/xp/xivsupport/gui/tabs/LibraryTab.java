package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.library.ActionTable;
import gg.xp.xivsupport.gui.library.StatusTable;

import javax.swing.*;

public class LibraryTab extends JTabbedPane {

	public LibraryTab() {
		super(LEFT);
		{
			addTab("Status Effects", StatusTable.table());
		}
		{
			addTab("Actions/Abilities", ActionTable.table());
		}
	}
}
