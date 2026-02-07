package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;

public class JTextFieldWithPlaceholder extends JTextField {
	public JTextFieldWithPlaceholder() {
	}

	public JTextFieldWithPlaceholder(String text) {
		super(text);
	}

	public JTextFieldWithPlaceholder(int columns) {
		super(columns);
	}

	public JTextFieldWithPlaceholder(String text, int columns) {
		super(text, columns);
	}

	public JTextFieldWithPlaceholder(Document doc, String text, int columns) {
		super(doc, text, columns);
	}

	public @Nullable String getPlaceholderText() {
		return placeholderLabel.getText();
	}

	public JTextFieldWithPlaceholder setPlaceholderText(@Nullable String placeholderText) {
		this.placeholderLabel.setText(placeholderText);
		return this;
	}

	private final JLabel placeholderLabel = new JLabel() {
		@Override
		public void paint(Graphics g) {
			String placeholder = this.getText();
			// Only render placeholder text if there is a non-empty placeholder, and the actual text entry is not empty
			if (placeholder == null || placeholder.isEmpty() || !JTextFieldWithPlaceholder.this.getText().isEmpty()) {
				return;
			}
			super.paint(g);
		}
	};

	{
		add(placeholderLabel);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		Insets insets = getInsets();
		placeholderLabel.setBounds(insets.left, insets.top, width - insets.left - insets.right, height - insets.top - insets.bottom);
		placeholderLabel.setForeground(getDisabledTextColor());
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}
