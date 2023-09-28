package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.topology.TopologyProvider;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
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
		JTree tree = new JTree(new TopologyTreeModel(container.getComponent(TopologyProvider.class).getTopology()));
		TopologyTreeRenderer renderer = new TopologyTreeRenderer();
		tree.setCellRenderer(renderer);
		tree.setCellEditor(new TopologyTreeEditor(tree));
		tree.setEditable(true);
		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setPreferredSize(scrollPane.getMaximumSize());
		add(new ReadOnlyText(helpText), BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
	}

	private static final String helpText = """
			Changes made here are not persisted. Everything will re-enable when you restart the program.

			To persistently disable something, you should disable it via whatever configuration interface the plugin provides.""";
}
