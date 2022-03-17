package gg.xp.xivsupport.gui.lists;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

import javax.swing.*;
import java.awt.*;

public class FriendlyNameListCellRenderer extends DefaultListCellRenderer {
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof HasFriendlyName fn) {
			//noinspection AssignmentToMethodParameter
			value = fn.getFriendlyName();
		}
		return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}
}
