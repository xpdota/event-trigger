package gg.xp.xivsupport.gui.tables.filters;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextBasedFilter<X> implements VisualFilter<X> {

	private static final Logger log = LoggerFactory.getLogger(TextBasedFilter.class);

	protected final JTextField textBox;
	protected final Runnable filterUpdatedCallback;
	protected final Function<X, String> textExtractor;
	protected final Color originalBackground;
	protected volatile Predicate<X> currentFilter;
	protected boolean validationError;
	protected final String fieldLabel;
	protected final boolean ignoreCase = true;
	protected final Color invalidBackground = new Color(62, 27, 27);

	public TextBasedFilter(Runnable filterUpdatedCallback, String fieldLabel, Function<X, String> textExtractor) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		this.textExtractor = textExtractor;
		this.fieldLabel = fieldLabel;
		textBox = new JTextField(10);
		textBox.setEditable(true);
		textBox.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
		});
		originalBackground = textBox.getBackground();
		update();
	}

	private void update() {
		String currentRawText = textBox.getText();
		currentFilter = getFilterForInput(currentRawText);
		if (validationError) {
			textBox.setBackground(invalidBackground);
		}
		else {
			textBox.setBackground(originalBackground);
		}
		filterUpdatedCallback.run();
	}

	protected @Nullable Predicate<X> getFilterForInput(String input) {
		validationError = false;
		if (input.isEmpty()) {
			return null;
		}
		else {
			if (input.startsWith("/") && input.endsWith("/") && input.length() >= 2) {
				try {
					Pattern regex;
					if (ignoreCase) {
						regex = Pattern.compile(input.substring(1, input.length() - 1), Pattern.CASE_INSENSITIVE);
					}
					else {
						regex = Pattern.compile(input.substring(1, input.length() - 1));
					}
					return item -> regex.matcher(textExtractor.apply(item)).find();
				}
				catch (PatternSyntaxException e) {
					validationError = true;
					return item -> false;
				}
			}
			if (ignoreCase) {
				return item -> textExtractor.apply(item)
						.toUpperCase(Locale.ROOT)
						.contains(input.toUpperCase(Locale.ROOT));
			}
			else {
				return item -> textExtractor.apply(item).contains(input);
			}
		}
	}

	/**
	 * Meant to be overridden. Pre-filter items in a smart way. Basically, allow this
	 * filter to specify whether it actually cares about a particular item. If there is
	 * a filter set, then any item that returns false when passed into this will fail
	 * automatically. If there is no filter set, everything will pass.
	 * <p>
	 * If there is no filter whatsoever, let everything pass.
	 * <p>
	 * If there is any filter, then apply this pre-filter.
	 *
	 * @param item
	 * @return
	 */
	protected boolean preFilter(X item) {
		return true;
	}

	@Override
	public boolean passesFilter(X item) {
		if (currentFilter == null) {
			return true;
		}
		if (!preFilter(item)) {
			return false;
		}
		return currentFilter.test(item);
	}


	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel(fieldLabel + ": ");
		label.setLabelFor(textBox);
		panel.add(label);
		panel.add(textBox);
		return panel;
	}


}
