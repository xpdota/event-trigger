package gg.xp.xivsupport.gui.tables.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TextFieldWithValidation<X> extends JTextField {

	private static final Logger log = LoggerFactory.getLogger(TextFieldWithValidation.class);

	protected final Color originalBackground;
	private final Function<String, X> parser;
	private final Consumer<X> consumer;
	protected final Color invalidBackground = new Color(62, 27, 27);
	private boolean stopUpdate;

	public TextFieldWithValidation(Function<String, X> parser, Consumer<X> consumer, String initialValue) {
		super(10);
		this.parser = parser;
		this.consumer = consumer;
		setText(initialValue);
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

	private void update() {
		boolean validationError;
		String currentRawText = getText();
		try {
			X parsedValue = parser.apply(currentRawText);
			try {
				consumer.accept(parsedValue);
				validationError = false;
			} catch (Throwable e) {
				log.error("Error consuming new value ({})", parsedValue, e);
				validationError = true;
			}
		}
		catch (Throwable e) {
			validationError = true;
		}
		if (validationError) {
			setBackground(invalidBackground);
		}
		else {
			setBackground(originalBackground);
		}
	}
}
