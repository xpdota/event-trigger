package gg.xp.xivsupport.gui.library;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class ChooserDialog {
	private ChooserDialog() {
	}

	public static <X> void showChooser(TableWithFilterAndDetails<X, ?> table, Consumer<X> callback) {
		JDialog dialog = new JDialog();
		Container pane = dialog.getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(table, BorderLayout.CENTER);
		JPanel buttonsPanel = new JPanel(new WrapLayout());
		JButton select = new JButton("Select");
		JButton cancel = new JButton("Cancel");
		select.addActionListener(l -> {
			X selection = table.getCurrentSelection();
			dialog.setVisible(false);
			dialog.dispose();
			callback.accept(selection);
		});
		cancel.addActionListener(l -> {
			dialog.setVisible(false);
			dialog.dispose();
		});
		buttonsPanel.add(select);
		buttonsPanel.add(cancel);
		pane.add(buttonsPanel, BorderLayout.SOUTH);
		dialog.setSize(new Dimension(800, 800));
		dialog.revalidate();
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		table.signalNewData();
		dialog.setVisible(true);

	}
}
