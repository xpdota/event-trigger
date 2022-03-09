package gg.xp.xivsupport.gui.library;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class ChooserDialog {
	private ChooserDialog() {
	}

	public static <X> void showChooser(Window owner, TableWithFilterAndDetails<X, ?> table, Consumer<X> callback) {
		JDialog dialog = new JDialog(owner, "Item Chooser");
		Container pane = dialog.getContentPane();
		pane.setLayout(new BorderLayout());
		pane.add(table, BorderLayout.CENTER);
		JPanel buttonsPanel = new JPanel(new WrapLayout());
		JButton select = new JButton("Select") {
			@Override
			public boolean isEnabled() {
				return table.getCurrentSelection() != null;
			}
		};
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
		dialog.setLocationRelativeTo(owner);
		dialog.revalidate();
		dialog.setModal(true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		table.signalNewData();
		table.getMainTable().getSelectionModel().addListSelectionListener(l -> {
			select.repaint();
		});
		dialog.setVisible(true);
	}

	public static <X> @Nullable X chooserReturnItem(Window owner, TableWithFilterAndDetails<X, ?> table) {
		Mutable<X> mutable = new MutableObject<>();
		showChooser(owner, table, mutable::setValue);
		return mutable.getValue();
	}
}
