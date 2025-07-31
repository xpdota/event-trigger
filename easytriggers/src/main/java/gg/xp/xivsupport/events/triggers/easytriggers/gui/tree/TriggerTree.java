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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static gg.xp.xivsupport.events.triggers.easytriggers.gui.tree.TriggerTransferable.TRIGGER_FLAVOR;

public class TriggerTree extends JTree {
	private static final Logger log = LoggerFactory.getLogger(TriggerTree.class);
	private final TriggerTreeModel model;
	private final Supplier<Selections> selectionsSupplier;

//	private @Nullable BaseTrigger<?> trigger;

	public TriggerTree(TriggerTreeModel model, EasyTriggers root, Supplier<Selections> selectionsSupplier) {
		super(model);
		this.model = model;
		this.selectionsSupplier = selectionsSupplier;
		setShowsRootHandles(true);
		setRootVisible(false);
		setDragEnabled(true);
		setEditable(true);
		setDropMode(DropMode.ON_OR_INSERT);
		setLargeModel(true);
		setCellEditor(new DummyTreeCellEditor());
		setTransferHandler(new TriggerTransferHandler(root));
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
								SwingUtilities.invokeLater(TriggerTree.this::repaint);
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


	private class TriggerTransferHandler extends TransferHandler {
		private final EasyTriggers root;

		public TriggerTransferHandler(EasyTriggers root) {
			this.root = root;
		}

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
							if (last instanceof HasChildTriggers newParentFolder) {
								// Don't allow a trigger to be added to itself, or to a child of itself.
								HasChildTriggers current = newParentFolder;
								while (current instanceof TriggerFolder currentFolder) {
									HasChildTriggers finalCurrent = current;
									if (data.selections().getSelectedTriggers().stream().anyMatch(trigger -> trigger == finalCurrent)) {
										log.warn("Invalid drop attempted!");
										return false;
									}
									current = currentFolder.getParent();
								}

								int ci = dl.getChildIndex();
								List<BaseTrigger<?>> draggedTriggers = data.selections().getSelectedTriggers();
								List<TreePath> newPaths = new ArrayList<>(draggedTriggers.size());
								@Nullable HasChildTriggers oldParentFolder = data.sourceParentFolder();

								if (ci >= 0) {
									// Due to the case of dragging-and-dropping within a single folder, where target indices
									// may change due to items being removed from that same folder, and dragging non-contiguous
									// ranges, we instead want to find a child of the parent to use as an "anchor".
									// The anchor acts as an "insert after this object". We will continuously ask for the indexOf
									// that object, but it shouldn't be too large of a performance hit. We can always optimize it
									// later for the majority of scenarios with some simple optimizations, like re-using the old
									// index and checking validity.

									@Nullable BaseTrigger<?> dropAfterAnchor = null;
									Set<BaseTrigger<?>> ourTriggers = new HashSet<>(draggedTriggers);
									// Start at the index that we're dropping onto.
									// Search upwards for a valid drop-after location - i.e. something that is not in
									// the dragged set.
									// We subtract 1 to start, since we want to look at the previous item.
									List<BaseTrigger<?>> childTriggers = newParentFolder.getChildTriggers();
									for (int i = ci - 1; i >= 0; i--) {
										BaseTrigger<?> possibleTarget = childTriggers.get(i);
										if (!ourTriggers.contains(possibleTarget)) {
											dropAfterAnchor = possibleTarget;
											break;
										}
									}
									// If we didn't find anything, defaults to null.

									for (BaseTrigger<?> draggedTrigger : draggedTriggers) {
										if (oldParentFolder == null) {
											root.removeChildTriggers(draggedTrigger);
										}
										else {
											oldParentFolder.removeChildTriggers(draggedTrigger);
										}
									}

									// Re-check index because it might have changed
									int toIndex = dropAfterAnchor == null ? -1 : childTriggers.indexOf(dropAfterAnchor);
									for (BaseTrigger<?> draggedTrigger : draggedTriggers) {
										if (toIndex < 0) {
											newParentFolder.addChildTrigger(draggedTrigger);
										}
										else {
											// We add back the 1 that we subtracted previously.
											newParentFolder.addChildTrigger(draggedTrigger, toIndex + 1);
										}
										toIndex++;
										TreePath newPath = dl.getPath().pathByAddingChild(draggedTrigger);
										newPaths.add(newPath);
									}

								}
								else {
									// Direct drop case. Much simpler. Just append to end of list.
									for (BaseTrigger<?> draggedTrigger : draggedTriggers) {
										root.removeTrigger(oldParentFolder, draggedTrigger);
									}
									for (BaseTrigger<?> draggedTrigger : draggedTriggers) {
										newParentFolder.addChildTrigger(draggedTrigger);
										TreePath newPath = dl.getPath().pathByAddingChild(draggedTrigger);
										newPaths.add(newPath);
									}
								}
								SwingUtilities.invokeLater(TriggerTree.this::refresh);
								SwingUtilities.invokeLater(() -> setSelectionPaths(newPaths.toArray(TreePath[]::new)));
								root.commit();
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

		private boolean canImportInternal(TransferSupport support) {
			if (support.getComponent().equals(TriggerTree.this)) {
				if (support.isDataFlavorSupported(TRIGGER_FLAVOR)) {
					try {
						if (support.getTransferable().getTransferData(TRIGGER_FLAVOR) instanceof TriggerTransferData data) {
							if (support.getDropLocation() instanceof JTree.DropLocation dl) {
								Object last = dl.getPath().getLastPathComponent();
								// We can only drop onto or into a folder or the root
								if (last instanceof TriggerFolder folder) {
									// Don't allow a trigger to be added to itself, or to a child of itself.
									HasChildTriggers current = folder;
									while (current instanceof TriggerFolder currentFolder) {
										HasChildTriggers finalCurrent = current;
										if (data.selections().getSelectedTriggers().stream().anyMatch(trigger -> trigger == finalCurrent)) {
											return false;
										}
										current = currentFolder.getParent();
									}
									return true;
								}
								else if (last instanceof EasyTriggers) {
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
			return false;

		}

		@Override
		public boolean canImport(TransferSupport support) {
			boolean canImport = this.canImportInternal(support);
			// Cursor manipulation is a workaround for Java bug https://bugs.openjdk.org/browse/JDK-6700748
			if (canImport) {
				TriggerTree.this.setCursor(DragSource.DefaultMoveDrop);
			}
			else {
				TriggerTree.this.setCursor(DragSource.DefaultMoveNoDrop);
			}
			return canImport;
		}

		@Override
		protected @Nullable Transferable createTransferable(JComponent c) {
			@Nullable Selections selections = selectionsSupplier.get();
			if (selections == null || !selections.hasConsistentParentSelection()) {
				return null;
			}
			return new TriggerTransferable(new TriggerTransferData(selections));
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
	}
}
