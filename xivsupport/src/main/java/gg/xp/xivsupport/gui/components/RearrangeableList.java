package gg.xp.xivsupport.gui.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class RearrangeableList<X> extends JList<X> {

	private static final Logger log = LoggerFactory.getLogger(RearrangeableList.class);
	private final Consumer<List<X>> callback;

	X currentDrag;

	public RearrangeableList(List<X> items, Consumer<List<X>> callback) {
		super();
		this.callback = callback;
		DefaultListModel<X> model = new DefaultListModel<>();
		model.addAll(items);
		setModel(model);
		setFocusable(false);
		setDragEnabled(true);
		setTransferHandler(new MyListDropHandler());
		new MyDragListener();
		setDropMode(DropMode.INSERT);
	}

	private class MyListDropHandler extends TransferHandler {
		private static final long serialVersionUID = -7982645171209919315L;

		public boolean canImport(TransferSupport support) {
			if (support.getComponent().equals(RearrangeableList.this)) {
				JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
				return dl.getIndex() != -1;
			}
			return false;
		}

		public boolean importData(TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}
			X object = currentDrag;
			if (object == null) {
				return false;
			}

			JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();

			DefaultListModel<X> model = (DefaultListModel<X>) RearrangeableList.this.getModel();
			int moveFrom = model.indexOf(object);
			int moveTo = dl.getIndex();

			if (moveFrom == moveTo) {
				return false;
			}
			boolean moveDown = moveTo > moveFrom;

			currentDrag = null;
			model.add(moveTo, object);
			// If we are moving downwards in the list (index increasing), then we need to
			if (moveDown) {
				model.removeElementAt(model.indexOf(object));
			}
			else {
				model.removeElementAt(model.lastIndexOf(object));
			}
			callback.accept(Collections.list(model.elements()));
			return true;
		}
	}

	private class MyDragListener extends DragSourceAdapter implements DragGestureListener {

		DragSource ds = new DragSource();

		public MyDragListener() {
			DragGestureRecognizer dgr = ds.createDefaultDragGestureRecognizer(RearrangeableList.this,
					DnDConstants.ACTION_MOVE, this);

		}

		public void dragGestureRecognized(DragGestureEvent dge) {
			log.info("Drag and Drop Started");
			currentDrag = RearrangeableList.this.getSelectedValue();
			Transferable transfer = new DummyTransferrable();
			ds.startDrag(dge, DragSource.DefaultMoveDrop, transfer, this);
		}

		public void dragDropEnd(DragSourceDropEvent dsde) {
			if (dsde.getDropSuccess()) {
				log.info("Drag and Drop Succeeded");
			}
			else {
				log.info("Drag and Drop Failed");
			}
		}

	}

}
