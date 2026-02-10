package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.FailedDeserializationTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasChildTriggers;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TriggerTreeModel implements TreeModel {

	private final EasyTriggers root;

	public TriggerTreeModel(EasyTriggers root) {
		this.root = root;
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof HasChildTriggers tf) {
			return tf.getChildTriggers().get(index);
		}
		return root.getChildTriggers().get(index);
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof HasChildTriggers tf) {
			return tf.getChildTriggers().size();
		}
		return root.getChildTriggers().size();
	}

	@Override
	public boolean isLeaf(Object node) {
		return node instanceof EasyTrigger || node instanceof FailedDeserializationTrigger;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		Object parent = path.getParentPath().getLastPathComponent();
//		TreeModelEvent e = new TreeModelEvent(this, new Object[]{parent}, new int[]{getIndexOfChild(parent, path.getLastPathComponent())}, new Object[]{newValue});
		TreeModelEvent e = new TreeModelEvent(this, new Object[]{parent});
		for (TreeModelListener listener : listenerList.getListeners(TreeModelListener.class)) {
			listener.treeNodesChanged(e);
//			listener.treeStructureChanged(e);
		}
	}

	@SuppressWarnings("SuspiciousMethodCalls")
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof HasChildTriggers et) {
			return et.getChildTriggers().indexOf(child);
		}
		return -1;
	}

	void notifyChange() {
		TreeModelEvent e = new TreeModelEvent(this, new Object[]{root});
		for (TreeModelListener listener : listenerList.getListeners(TreeModelListener.class)) {
			listener.treeStructureChanged(e);
		}
	}


	// Everything below here is copy-paste from AbstractTreeTableModel
	private final EventListenerList listenerList = new EventListenerList();

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}
}
