package gg.xp.xivsupport.gui.lists;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemList<X> extends JPanel {

	private final Supplier<List<X>> getter;
	private final Consumer<List<X>> setter;
	private final Supplier<@Nullable X> addButtonAction;
	private final JList<X> list;
	private final DefaultListModel<X> model;

	public ItemList(Supplier<List<X>> getter, Consumer<List<X>> setter, ListCellRenderer<X> renderer, Supplier<@Nullable X> addButtonAction) {
		setLayout(new BorderLayout());
		this.getter = getter;
		this.setter = setter;
		this.addButtonAction = addButtonAction;

		list = new JList<>();
		list.setCellRenderer(renderer);

		add(new JScrollPane(list), BorderLayout.CENTER);
		model = new DefaultListModel<>();
		list.setModel(model);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		JButton addButton = new JButton("Add");
		JButton deleteButton = new JButton("Delete");

		deleteButton.addActionListener(l -> {
			model.removeElement(list.getSelectedValue());
		});

		addButton.addActionListener(l -> {
			X newValue = addButtonAction.get();
			if (newValue != null) {
				model.addElement(newValue);
			}
		});

		buttonPanel.add(addButton);
		buttonPanel.add(deleteButton);

		add(buttonPanel, BorderLayout.SOUTH);

		model.addAll(getter.get());
		model.addListDataListener(new ListDataListener() {
			@Override
			public void intervalAdded(ListDataEvent e) {
				doSetter();
//				refresh();
			}

			@Override
			public void intervalRemoved(ListDataEvent e) {
				doSetter();
//				refresh();
			}

			@Override
			public void contentsChanged(ListDataEvent e) {
				doSetter();
//				refresh();
			}
		});
	}

	private void doSetter() {
		setter.accept(Collections.list(model.elements()));
	}

//	public void refresh() {
//
//		list.setListData(new Vector<>(getter.get()));
//	}
}
