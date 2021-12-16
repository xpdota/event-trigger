package gg.xp.xivsupport.gui.map;

import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import zoomable.panel.ZoomPanel;

import javax.swing.*;
import java.awt.*;

public class MapPanel extends TitleBorderFullsizePanel {


	public MapPanel() {
		super("Map");
		// TODO: revisit zooming later
		setPreferredSize(getMaximumSize());
		setLayout(new BorderLayout());
		JPanel zoomPanel = new ZoomPanel();
		zoomPanel.setPreferredSize(getMaximumSize());
		zoomPanel.add(new JLabel("Test Text Here"));
		add(zoomPanel);
	}


}
