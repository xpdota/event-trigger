package gg.xp.xivsupport.gui.tables;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

// TODO: use this
public class SelectionHelper<X> {

	private final JPanel detailsPanel;
	private final CustomTableModel<X> model;
	private final Function<X, Component> detailsProvider;
	private X selection;
	private List<X> multiSelections = Collections.emptyList();

	public SelectionHelper(JTable table, CustomTableModel<X> model, Function<X, Component> detailsProvider) {
		table.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		this.model = model;
		this.detailsPanel = new JPanel();
		this.detailsProvider = detailsProvider;
	}

	public JPanel getDetailsPanel() {
		return detailsPanel;
	}

	private void refreshSelection() {
		this.multiSelections = model.getSelectedValues();
		this.selection = multiSelections.size() == 1 ? multiSelections.get(0) : null;
		SwingUtilities.invokeLater(() -> {
			detailsPanel.removeAll();
			if (selection != null) {
				detailsPanel.add(detailsProvider.apply(selection));
			}
			detailsPanel.revalidate();
			detailsPanel.repaint();
		});
	}
}
