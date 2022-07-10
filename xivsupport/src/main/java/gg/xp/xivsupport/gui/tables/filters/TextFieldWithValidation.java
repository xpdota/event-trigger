package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.gui.ResettableField;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TextFieldWithValidation<X> extends JTextField implements ResettableField {

	private static final Logger log = LoggerFactory.getLogger(TextFieldWithValidation.class);

	protected final Color originalBackground;
	private final Function<String, X> parser;
	private final Consumer<X> consumer;
	protected final Color invalidBackground = new Color(62, 27, 27);
	private boolean stopUpdate;
	private Supplier<String> valueGetter;
	private boolean validationError;

	public TextFieldWithValidation(Function<String, X> parser, Consumer<X> consumer, String initialValue) {
		this(parser, consumer, () -> initialValue);
	}

	public TextFieldWithValidation(Function<String, X> parser, Consumer<X> consumer, Supplier<String> valueGetter) {
		super(10);
		this.parser = parser;
		this.consumer = consumer;
		this.valueGetter = valueGetter;
		resetText();
		setEditable(true);
		getDocument().addDocumentListener(new DocumentListener() {
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
		originalBackground = getBackground();
	}

	private volatile boolean ignoreUpdate;

	public void resetText() {
		ignoreUpdate = true;
		try {
			String value = valueGetter.get();
			if (value == null) {
				value = "";
			}
			setText(value);
		}
		finally {
			ignoreUpdate = false;
		}
	}

	@Override
	public void reset() {
		resetText();
	}

	public void recheck() {
		update();
	}

	private @Nullable String validationErrorMessage;

	@Override
	public String getToolTipText() {
		if (validationErrorMessage != null) {
			return validationErrorMessage;
		}
		return super.getToolTipText();
	}

	public boolean hasValidationError() {
		return validationError;
	}

	private void update() {
		String currentRawText = getText();
		try {
			X parsedValue = parser.apply(currentRawText);
			try {
				if (!ignoreUpdate) {
					consumer.accept(parsedValue);
				}
				validationError = false;
			} catch (ValidationError e) {
				validationError = true;
				validationErrorMessage = e.getMessage();
			} catch (Throwable e) {
				log.error("Error consuming new value ({})", parsedValue, e);
				validationError = true;
			}
		}
		catch (ValidationError e) {
			validationError = true;
			validationErrorMessage = e.getMessage();
		}
		catch (Throwable e) {
			validationError = true;
		}
		if (validationError) {
			setBackground(invalidBackground);
		}
		else {
			setBackground(originalBackground);
			validationErrorMessage = null;
		}
	}
}
