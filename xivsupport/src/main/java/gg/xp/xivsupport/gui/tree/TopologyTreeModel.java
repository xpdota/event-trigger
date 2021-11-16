package gg.xp.xivsupport.gui.tree;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.topology.TopoItem;
import gg.xp.reevent.topology.TopoMethod;
import gg.xp.reevent.topology.Topology;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TopologyTreeModel implements TreeModel {

	private final Topology topology;

	public TopologyTreeModel(AutoEventDistributor auto) {
		topology = auto.getTopology();
	}

	@Override
	public Object getRoot() {
		return topology;
	}

	@Override
	public Object getChild(Object parent, int index) {
		return ((TopoItem) parent).getChildren().get(index);
	}

	@Override
	public int getChildCount(Object parent) {
		return ((TopoItem) parent).getChildren().size();
	}

	@Override
	public boolean isLeaf(Object node) {
		return node instanceof TopoMethod;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// TODO: what is this?

	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return ((TopoItem) parent).getChildren().indexOf(child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
//			throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
//			throw new UnsupportedOperationException("Not supported");
	}
}
