package gg.xp.xivsupport.gui.tables.filters;

import groovy.lang.GroovyShell;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public class GroovyFilter<X> implements SplitVisualFilter<X> {

	private static final Logger log = LoggerFactory.getLogger(GroovyFilter.class);
	private final GroovyShell shell = new GroovyShell();
	private final TextFieldWithValidation<?> textBox;
	private final String shortClassName;
	private final String longClassName;
	private final String varName;
	private @Nullable Predicate<X> filterScript;
	private boolean strict;
	private String lastFilterText;
	private final Runnable filterUpdatedCallback;
	private final Class<?> dataType;

	public static <X> Function<Runnable, VisualFilter<? super X>> forClass(Class<X> dataType) {
		return (filterUpdatedCallback) -> new GroovyFilter<>(filterUpdatedCallback, dataType);
	}

	public GroovyFilter(Runnable filterUpdatedCallback, Class<X> dataType) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		this.dataType = dataType;
		this.textBox = new TextFieldWithValidation<>(this::makeFilter, this::setFilter, "");
		this.shortClassName = dataType.getSimpleName();
		this.longClassName = dataType.getCanonicalName();
		this.varName = shortClassName.toLowerCase(Locale.ROOT);
	}

	private @Nullable Predicate<X> makeFilter(@Nullable String filterText) {
		lastFilterText = filterText;
		if (filterText == null || filterText.isBlank()) {
			return null;
		}
		try {
			String checkType = strict ? "@CompileStatic" : "";
			String inJavaForm =
					"""
					import java.util.function.Predicate;
					import %s;
					import groovy.transform.CompileStatic;
					import groovy.transform.TypeChecked;
					
					new Predicate<%s>() {
						%s
						@Override
						public boolean test(%s %s) {
							%s
						}
					};
					""".formatted(longClassName, shortClassName, checkType, shortClassName, varName, filterText);
			Predicate<X> compiled = (Predicate<X>) shell.evaluate(inJavaForm);
			textBox.setToolTipText(null);
			return compiled;
		}
		catch (Throwable t) {
			textBox.setToolTipText(t.getMessage());
			throw t;
		}
	}

	private void setFilter(@Nullable Predicate<X> filter) {
		filterScript = filter;
		filterUpdatedCallback.run();
	}

	@Override
	public boolean passesFilter(X item) {
		Predicate<X> filterScript = this.filterScript;
		if (filterScript == null) {
			return true;
		}
		shell.setVariable(varName, item);
		boolean result;
		try {
			result = filterScript.test(item);
		}
		catch (Throwable t) {
			return false;
		}
		return result;
	}


	@Override
	public JPanel getComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JLabel label = new JLabel("Groovy: ");
		label.setLabelFor(textBox);
		JCheckBox cb = new JCheckBox("Strict");
		cb.addActionListener(l -> {
			strict = cb.isSelected();
			setFilter(makeFilter(lastFilterText));
		});
		panel.add(label, BorderLayout.WEST);
		panel.add(textBox, BorderLayout.CENTER);
		panel.add(cb, BorderLayout.EAST);
		cb.setSelected(true);
		return panel;
	}

	@Override
	public String getName() {
		return "Freeform";
	}
}
