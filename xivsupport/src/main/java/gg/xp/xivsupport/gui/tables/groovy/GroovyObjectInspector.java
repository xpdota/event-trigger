package gg.xp.xivsupport.gui.tables.groovy;

import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import groovy.lang.PropertyValue;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroovyObjectInspector extends JPanel {

	private final JTree tree;
	private final CustomTableModel<PropertyValue> detailsModel;

	public GroovyObjectInspector(Object rootObject, RightClickOptionRepo rc) {
		setLayout(new BorderLayout());

		// Tree view
		GroovyTreeModel treeModel = new GroovyTreeModel(rootObject);
		tree = new JTree(treeModel);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (value instanceof PropertyValue pv) {
					String name = pv.getName();
					Object val;
					try {
						val = pv.getValue();
					}
					catch (Throwable t) {
						val = t;
					}
					setText(name + ": " + GroovyColumns.singleValueConversion(val));
				}
				else if (value != null) {
					setText(value.toString());
				}
				return this;
			}
		});

		// Details view
		detailsModel = CustomTableModel.builder(() -> GroovyColumns.getValues(getSelectedObject()))
				.addColumn(GroovyColumns.propName)
				.addColumn(GroovyColumns.propVal)
				.addColumn(GroovyColumns.propType)
				.build();
		JTable detailsTable = detailsModel.makeTable();

		// Toolbar
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton fullRefreshBtn = new JButton("Full Refresh");
		fullRefreshBtn.addActionListener(e -> {
			treeModel.refreshAll();
			detailsModel.fullRefresh();
		});
		JButton refreshSelectionBtn = new JButton("Refresh Selection");
		refreshSelectionBtn.addActionListener(e -> {
			TreePath selectionPath = tree.getSelectionPath();
			if (selectionPath != null) {
				treeModel.fireTreeStructureChanged(selectionPath);
				detailsModel.fullRefresh();
			}
		});
		toolbar.add(fullRefreshBtn);
		toolbar.add(refreshSelectionBtn);
		add(toolbar, BorderLayout.NORTH);

		tree.addTreeSelectionListener(e -> {
			detailsModel.fullRefresh();
		});

		rc.configureTree(tree, this::getSelectedObject);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tree), new JScrollPane(detailsTable));
		splitPane.setDividerLocation(300);
		add(splitPane, BorderLayout.CENTER);
	}

	private @Nullable Object getSelectedObject() {
		Object lastSelected = tree.getLastSelectedPathComponent();
		return unwrap(lastSelected);
	}

	private static @Nullable Object unwrap(Object node) {
		if (node instanceof PropertyValue pv) {
			try {
				return pv.getValue();
			}
			catch (Throwable t) {
				return t;
			}
		}
		return node;
	}

	private static class GroovyTreeModel implements TreeModel {
		private final Object root;
		private final List<TreeModelListener> listeners = new CopyOnWriteArrayList<>();

		public GroovyTreeModel(Object root) {
			this.root = root;
		}

		@Override
		public Object getRoot() {
			return root;
		}

		@Override
		public Object getChild(Object parent, int index) {
			List<PropertyValue> props = GroovyColumns.getValues(unwrap(parent));
			return props.get(index);
		}

		@Override
		public int getChildCount(Object parent) {
			return GroovyColumns.getValues(unwrap(parent)).size();
		}

		@Override
		public boolean isLeaf(Object node) {
			if (node == root) {
				return false;
			}
			Object val = unwrap(node);
			if (val == null) {
				return true;
			}
			Class<?> clazz = val.getClass();
			if (clazz.isPrimitive() || isBoxedPrimitive(val) || val instanceof String) {
				return true;
			}
			return false;
		}

		private boolean isBoxedPrimitive(Object val) {
			return val instanceof Boolean || val instanceof Byte || val instanceof Character || val instanceof Short || val instanceof Integer || val instanceof Long || val instanceof Float || val instanceof Double;
		}

		@Override
		public void valueForPathChanged(TreePath path, Object newValue) {
		}

		@Override
		public int getIndexOfChild(Object parent, Object child) {
			List<PropertyValue> props = GroovyColumns.getValues(unwrap(parent));
			return props.indexOf(child);
		}

		@Override
		public void addTreeModelListener(TreeModelListener l) {
			listeners.add(l);
		}

		@Override
		public void removeTreeModelListener(TreeModelListener l) {
			listeners.remove(l);
		}

		public void refreshAll() {
			fireTreeStructureChanged(new TreePath(root));
		}

		public void fireTreeStructureChanged(TreePath path) {
			TreeModelEvent e = new TreeModelEvent(this, path);
			for (TreeModelListener listener : listeners) {
				listener.treeStructureChanged(e);
			}
		}
	}
}
