package gg.xp.xivsupport.gui.nav;

import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class GlobalNavPanel extends JPanel {

	private final GlobalUiRegistry reg;
	private final CustomTableModel<GuiRef> model;
	private final TextFieldWithValidation<String> textBox;
	private @Nullable String currentSearch;
	private Runnable activateHook = () -> {};

	public GlobalNavPanel(GlobalUiRegistry reg) {
		this.reg = reg;
		model = CustomTableModel.builder(() -> currentSearch == null ? Collections.emptyList() : reg.search(currentSearch))
				.addColumn(new CustomColumn<>("Name", GuiRef::primaryName))
				.build();
		JTable table = model.makeTable();
		textBox = new TextFieldWithValidation<>(Function.identity(), value -> {
			currentSearch = (value == null || value.isEmpty()) ? null : value;
			model.fullRefreshSync();
			if (model.getRowCount() > 0) {
				table.setRowSelectionInterval(0, 0);
			}
		}, "");
		textBox.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
					table.dispatchEvent(e);
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
					table.dispatchEvent(e);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
					table.dispatchEvent(e);
				}
			}
		});
		table.setTableHeader(null);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		textBox.addActionListener(l -> go());
		GuiUtil.tableDoubleClickAction(table, model, item -> go());
		JScrollPane sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setLayout(new BorderLayout());
		add(textBox, BorderLayout.NORTH);
		add(sp, BorderLayout.CENTER);
	}


	private void go() {
		GuiRef value = model.getSelectedValue();
		if (value == null) {
			List<GuiRef> data = model.getData();
			if (data.isEmpty()) {
				return;
			}
			data.get(0).activate();
		}
		else {
			value.activate();
		}
		activateHook.run();
		textBox.selectAll();
//		textBox.resetText();
//		currentSearch = null;
//		model.signalNewData();
	}


	public void goingToShow() {
		textBox.selectAll();
		textBox.requestFocus();

	}

	public void setActivateHook(Runnable activateHook) {
		this.activateHook = activateHook;
	}
}
