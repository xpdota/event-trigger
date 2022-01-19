package gg.xp.xivsupport.gui.map;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;

import java.awt.*;

@ScanMe
public class MapTab extends TitleBorderFullsizePanel {
	public MapTab(MapPanel panel) {
		super("Map");
		setPreferredSize(getMaximumSize());
		setLayout(new BorderLayout());
		add(panel);
	}
}
