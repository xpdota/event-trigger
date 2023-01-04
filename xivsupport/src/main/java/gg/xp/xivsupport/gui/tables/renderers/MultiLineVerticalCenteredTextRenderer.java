package gg.xp.xivsupport.gui.tables.renderers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class MultiLineVerticalCenteredTextRenderer extends DefaultTableCellRenderer {

	private final String nullText;
	private final JPanel panel;
	private final JTextArea rot;
	{
		rot = new JTextArea();
		panel = new JPanel(null) {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				super.setBounds(x, y, width, height);
				rot.setBounds(0, 0, width, height);
				rot.invalidate();
				int prefHeight = rot.getPreferredSize().height;
				if (prefHeight > height) {
					rot.setBounds(0, 0, width, height);
				}
				else {
					int delta = height - prefHeight;
					rot.setBounds(0, delta / 2, width, prefHeight);
				}
			}
		};
		rot.setAlignmentY(0.5f);
		rot.setEditable(false);
		rot.setWrapStyleWord(true);
		rot.setLineWrap(true);
		rot.setFocusable(false);
		rot.setOpaque(false);
		panel.add(rot);
		panel.setOpaque(true);
		panel.setAlignmentY(0.5f);

	}

	public MultiLineVerticalCenteredTextRenderer() {
		this("");
	}

	public MultiLineVerticalCenteredTextRenderer(String nullText) {
		this.nullText = nullText;
	}

	protected String objectToText(@Nullable Object value) {
		return value == null ? nullText : nonNullObjectToText(value);
	}

	protected String nonNullObjectToText(@NotNull Object value) {
		return value.toString();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		String text = objectToText(value);
		rot.setText(text);
		Component dflt = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		panel.setBackground(dflt.getBackground());
		panel.setForeground(dflt.getForeground());
		return panel;
	}
}
