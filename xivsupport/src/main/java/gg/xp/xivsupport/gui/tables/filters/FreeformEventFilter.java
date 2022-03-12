package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.reevent.events.Event;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class FreeformEventFilter implements VisualFilter<Event> {

	private static final Logger log = LoggerFactory.getLogger(FreeformEventFilter.class);
	private final GroovyShell shell = new GroovyShell();
	private final TextFieldWithValidation<?> textBox;
	private volatile @Nullable Closure<Boolean> filterScript;
	private final Runnable filterUpdatedCallback;

	public FreeformEventFilter(Runnable filterUpdatedCallback) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		this.textBox = new TextFieldWithValidation<>(this::makeFilter, this::setFilter, "");
	}

	private @Nullable Closure<Boolean> makeFilter(@Nullable String filterText) {
		if (filterText == null || filterText.isBlank()) {
			return null;
		}
		try {
			return (Closure<Boolean>) shell.evaluate(" { event -> " + filterText + "}");
		}
		catch (Throwable t) {
			textBox.setToolTipText(t.getMessage());
			throw t;
		}
	}

	private void setFilter(@Nullable Closure<Boolean> filter) {
		filterScript = filter;
		filterUpdatedCallback.run();
	}

	@Override
	public boolean passesFilter(Event item) {
		Closure<Boolean> filterScript = this.filterScript;
		if (filterScript == null) {
			return true;
		}
		shell.setVariable("event", item);
		boolean result;
		try {
			result = filterScript.call(item);
		}
		catch (Throwable t) {
			return false;
		}
		return result;
	}

	@Override
	public Component getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JLabel label = new JLabel("Groovy: ");
		label.setLabelFor(textBox);
		panel.add(label);
		panel.add(textBox);
		return panel;
	}
}
