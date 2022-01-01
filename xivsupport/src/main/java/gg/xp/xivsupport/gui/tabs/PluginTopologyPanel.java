package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.tree.TopologyTreeEditor;
import gg.xp.xivsupport.gui.tree.TopologyTreeModel;
import gg.xp.xivsupport.gui.tree.TopologyTreeRenderer;
import org.picocontainer.PicoContainer;

import javax.swing.*;
import java.awt.*;

public class PluginTopologyPanel extends TitleBorderFullsizePanel {

	public PluginTopologyPanel(PicoContainer container) {
		super("Topology");
		setLayout(new BorderLayout());
		JTree tree = new JTree(new TopologyTreeModel(container.getComponent(AutoEventDistributor.class)));
		TopologyTreeRenderer renderer = new TopologyTreeRenderer();
		tree.setCellRenderer(renderer);
		tree.setCellEditor(new TopologyTreeEditor(tree));
		tree.setEditable(true);
		JScrollPane scrollPane = new JScrollPane(tree);
//			scrollPane.setBorder(new LineBorder(Color.BLUE));
		scrollPane.setPreferredSize(scrollPane.getMaximumSize());
		add(scrollPane);
	}
}
