package gg.xp.xivsupport.gui.lists;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class FriendlyNameListCellRenderer extends DefaultListCellRenderer {

	private static final Logger log = LoggerFactory.getLogger(FriendlyNameListCellRenderer.class);
	private final JPanel splitPanel = new JPanel(new BorderLayout());

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		Object fallbackValue;
		if (value instanceof HasFriendlyName fn) {
			fallbackValue = fn.getFriendlyName();
		}
		else {
			fallbackValue = value;
		}
		Component icon;
		try {
			if (value instanceof HasIconURL hiu) {
				icon = IconTextRenderer.getIconOnly(hiu);
			}
			else if (value instanceof HasOptionalIconURL hoiu) {
				icon = IconTextRenderer.getIconOnly(hoiu.getIconUrl());
			}
			else {
				icon = null;
			}
		}
		catch (Throwable t) {
			log.error("Error loading icon for value {}", value);
			icon = null;
		}
		Component label = super.getListCellRendererComponent(list, fallbackValue, index, isSelected, cellHasFocus);
		if (icon == null) {
			return label;
		}
		else {
			splitPanel.removeAll();
			splitPanel.setBackground(label.getBackground());
			splitPanel.setOpaque(label.isOpaque());
			splitPanel.add(icon, BorderLayout.WEST);
			splitPanel.add(label, BorderLayout.CENTER);
			return splitPanel;
		}
	}
}
