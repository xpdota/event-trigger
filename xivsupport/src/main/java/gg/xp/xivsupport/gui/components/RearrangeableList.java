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
import java.io.Serial;
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
		@Serial
		private static final long serialVersionUID = -7982645171209919315L;

		@Override
		public boolean canImport(TransferSupport support) {
			if (support.getComponent().equals(RearrangeableList.this)) {
				JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
				return dl.getIndex() != -1;
			}
			return false;
		}

		@Override
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
			RearrangeableList.this.setSelectedIndex(moveTo);
			// If we are moving downwards in the list (index increasing), then we need to
			if (moveDown) {
				model.removeElementAt(model.indexOf(object));
			}
			else {
				model.removeElementAt(model.lastIndexOf(object));
			}
			doCallback();
			return true;
		}
	}

	private void doCallback() {
		DefaultListModel<X> model = (DefaultListModel<X>) RearrangeableList.this.getModel();
		callback.accept(Collections.list(model.elements()));
	}

	public List<X> getValues() {
		DefaultListModel<X> model = (DefaultListModel<X>) RearrangeableList.this.getModel();
		return Collections.list(model.elements());
	}

	// TODO: should this work like the self-refresh in tables?
	public void setValues(List<X> values) {
		DefaultListModel<X> model = (DefaultListModel<X>) RearrangeableList.this.getModel();
		model.clear();
		model.addAll(values);
		// TODO: decide if this should be here
//		doCallback();
	}

	private class MyDragListener extends DragSourceAdapter implements DragGestureListener {

		DragSource ds = new DragSource();

		public MyDragListener() {
			DragGestureRecognizer dgr = ds.createDefaultDragGestureRecognizer(RearrangeableList.this,
					DnDConstants.ACTION_MOVE, this);

		}

		@Override
		public void dragGestureRecognized(DragGestureEvent dge) {
			log.info("Drag and Drop Started");
			currentDrag = RearrangeableList.this.getSelectedValue();
			Transferable transfer = new DummyTransferrable();
			ds.startDrag(dge, DragSource.DefaultMoveDrop, transfer, this);
		}

		@Override
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
