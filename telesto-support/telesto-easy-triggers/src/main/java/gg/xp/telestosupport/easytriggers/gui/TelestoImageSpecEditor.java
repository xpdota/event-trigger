package gg.xp.telestosupport.easytriggers.gui;

import gg.xp.telestosupport.easytriggers.IconSpec;
import gg.xp.telestosupport.easytriggers.IconType;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithExternalValidation;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.tables.renderers.ScaledImageComponent;

import javax.swing.*;
import java.awt.*;

public class TelestoImageSpecEditor extends JPanel {
	private final IconSpec iconSpec;
	private final JComboBox<IconType> dropdown;
	private final TextFieldWithExternalValidation<Long> editor;
	private final JPanel iconPreviewPanel;

	public TelestoImageSpecEditor(String labelText, IconSpec iconSpec) {
		this.iconSpec = iconSpec;
		JLabel label = new JLabel(labelText);
		// TODO: restrict to valid values

		dropdown = new JComboBox<>(IconType.values());
		dropdown.setRenderer(new FriendlyNameListCellRenderer());
		dropdown.setSelectedItem(iconSpec.type);
		dropdown.addItemListener(l -> reprocess());

		editor = new TextFieldWithExternalValidation<>(Long::parseLong, v -> {
			iconSpec.value = v;
			SwingUtilities.invokeLater(this::reprocess);
		}, () -> Long.toString(iconSpec.value));

		setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		add(label);
		add(dropdown);
		iconPreviewPanel = new JPanel();
		iconPreviewPanel.setPreferredSize(new Dimension(40, 40));
		add(editor);
		add(iconPreviewPanel);
		reprocess();
	}

	private void reprocess() {
		iconSpec.type = (IconType) dropdown.getSelectedItem();
		iconPreviewPanel.removeAll();
		Long iconId = iconSpec.toIconId(null);
		if (iconId != null) {
			HasIconURL hasIconURL = IconUtils.makeIcon(Math.toIntExact(iconId));
			ScaledImageComponent iconOnly = IconTextRenderer.getIconOnly(hasIconURL);
			if (iconOnly != null) {
				iconPreviewPanel.add(iconOnly);
			}
		}
		repaint();

	}
}
