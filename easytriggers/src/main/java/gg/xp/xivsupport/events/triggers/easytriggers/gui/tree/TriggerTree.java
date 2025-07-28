package gg.xp.xivsupport.events.triggers.easytriggers.gui.tree;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasChildTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Enumeration;

import static gg.xp.xivsupport.events.triggers.easytriggers.gui.tree.TriggerTransferable.TRIGGER_FLAVOR;

public class TriggerTree extends JTree {
	private static final Logger log = LoggerFactory.getLogger(TriggerTree.class);
	private final TriggerTreeModel model;

//	private @Nullable BaseTrigger<?> trigger;

	public TriggerTree(TriggerTreeModel model, EasyTriggers root) {
		super(model);
		this.model = model;
		setShowsRootHandles(true);
		setRootVisible(false);
		setDragEnabled(true);
//		setEditable(true);
		setDropMode(DropMode.ON_OR_INSERT);
		setTransferHandler(new TransferHandler() {
			@Override
			public boolean importData(TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}
				if (!support.isDrop()) {
					return false;
				}
				if (support.isDataFlavorSupported(TRIGGER_FLAVOR)) {
					try {
						// Now I remember why I didn't do it the "right" way for the dndlist
						if (support.getTransferable().getTransferData(TRIGGER_FLAVOR) instanceof TriggerTransferData data) {
							if (support.getDropLocation() instanceof JTree.DropLocation dl) {
								Object last = dl.getPath().getLastPathComponent();
								// We can only drop onto or into a folder
								if (last instanceof HasChildTriggers folder) {
									int ci = dl.getChildIndex();
									BaseTrigger<?> draggedTrigger = data.trigger();
									// We need to remove it from its current parent first
									@Nullable TriggerFolder parentFolder = data.sourceParentFolder();
									root.removeTrigger2(parentFolder, draggedTrigger);
									if (ci >= 0) {
										// If we drop into a specific place in the parent, add it at that index
										folder.addChildTrigger(draggedTrigger, ci);
									}
									else {
										// If we drop directly onto a directory, add it to the end
										folder.addChildTrigger(draggedTrigger);
									}
									SwingUtilities.invokeLater(TriggerTree.this::refresh);
									TreePath newPath = dl.getPath().pathByAddingChild(draggedTrigger);
									SwingUtilities.invokeLater(() -> TriggerTree.this.setSelectionPath(newPath));
									return true;
								}
							}
						}
					}
					catch (UnsupportedFlavorException | IOException e) {
						throw new RuntimeException(e);
					}
				}
				return false;
			}

			@Override
			public boolean canImport(TransferSupport support) {
				if (support.getComponent().equals(TriggerTree.this)) {
					if (support.isDataFlavorSupported(TRIGGER_FLAVOR)) {
						try {
							if (support.getTransferable().getTransferData(TRIGGER_FLAVOR) instanceof TriggerTransferData) {
								if (support.getDropLocation() instanceof JTree.DropLocation dl) {
									Object last = dl.getPath().getLastPathComponent();
									// We can only drop onto or into a folder or the root
									if (last instanceof TriggerFolder) {
										TriggerTree.this.setCursor(DragSource.DefaultMoveDrop);
										return true;
									}
									else if (last instanceof EasyTriggers) {
										TriggerTree.this.setCursor(DragSource.DefaultMoveDrop);
										return true;
									}
								}
							}
						}
						catch (UnsupportedFlavorException | IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
				TriggerTree.this.setCursor(DragSource.DefaultMoveNoDrop);
				return false;
			}

			@Override
			protected @Nullable Transferable createTransferable(JComponent c) {
				TreePath path = getSelectionPath();
				if (path == null) {
					return null;
				}
				if (path.getLastPathComponent() instanceof BaseTrigger<?> bt) {
					return new TriggerTransferable(new TriggerTransferData(bt, path));
				}
				return null;
			}

			@Override
			public int getSourceActions(JComponent c) {
				return MOVE;
			}

			@Override
			protected void exportDone(JComponent source, Transferable data, int action) {
				log.info("exportDone: {} {} {}", source, data, action);
				// Cursor manipulation is a workaround for Java bug https://bugs.openjdk.org/browse/JDK-6700748
				TriggerTree.this.setCursor(Cursor.getDefaultCursor());
				super.exportDone(source, data, action);
			}
		});
		setCellRenderer(new EasyTriggersTreeRenderer());
//		setCellEditor(new TriggerTreeEditor(this));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Since we only care about clicks (not drags) for the sake of toggling the checkbox,
				// instead we can just directly identify clicks that happen approximately where the checkbox
				// would be.
				TreePath path = TriggerTree.this.getPathForLocation(e.getX(), e.getY());
				if (path != null) {
					Rectangle bounds = TriggerTree.this.getPathBounds(path);
					if (bounds != null) {
						// Assume checkbox is square, i.e. its width == the row height
						if (e.getX() > bounds.x && e.getX() <= bounds.x + bounds.height) {
							if (path.getLastPathComponent() instanceof BaseTrigger<?> bt) {
								bt.setEnabled(!bt.isEnabled());
								SwingUtilities.invokeLater(() -> TriggerTree.this.repaint(bounds));
								// TODO: this should also prevent a double-click from expanding/collapsing
								e.consume();
							}
						}
					}
				}
			}
		});

	}


	public void refresh() {
		TreePath[] selectionPaths = getSelectionPaths();
		model.notifyChange();
		// I don't know why we have to do this, but it works.
		setExpandedState(new TreePath(model.getRoot()), false);
		setExpandedState(new TreePath(model.getRoot()), true);
		setSelectionPaths(selectionPaths);
	}

	@Override
	protected void clearToggledPaths() {
		// Don't clear on a reload
	}

	@Override
	protected void removeDescendantToggledPaths(Enumeration<TreePath> toRemove) {
		// Also don't cleawr stuff on a refresh
	}


}
