package gg.xp.xivsupport.gui.tables.filters;

import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.sys.Threading;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class GroovyFilter<X> implements SplitVisualFilter<X> {

	private static final Logger log = LoggerFactory.getLogger(GroovyFilter.class);
	private @Nullable GroovyShell shell;
	private final ExecutorService exs = Executors.newSingleThreadExecutor(Threading.namedDaemonThreadFactory("GroovyFilter"));
	private final TextFieldWithValidation<?> textBox;
	private final String longClassName;
	private final String varName;
	private final GroovyManager mgr;
	private @Nullable Predicate<X> filterScript;
	private boolean strict;
	private String lastFilterText;
	private final Runnable filterUpdatedCallback;
	private final Class<?> dataType;
	private static final AtomicInteger scriptCounter = new AtomicInteger(1);

	public static <X> Function<Runnable, VisualFilter<? super X>> forClass(Class<X> dataType, GroovyManager mgr) {
		return (filterUpdatedCallback) -> new GroovyFilter<>(filterUpdatedCallback, mgr, dataType, dataType.getSimpleName().toLowerCase(Locale.ROOT));
	}

	public static <X> Function<Runnable, VisualFilter<? super X>> forClass(Class<X> dataType, GroovyManager mgr, String varName) {
		return (filterUpdatedCallback) -> new GroovyFilter<>(filterUpdatedCallback, mgr, dataType, varName);
	}

	public GroovyFilter(Runnable filterUpdatedCallback, GroovyManager mgr, Class<X> dataType, String varName) {
		this.filterUpdatedCallback = filterUpdatedCallback;
		this.dataType = dataType;
		this.textBox = new TextFieldWithValidation<>(this::makeFilter, this::setFilter, "");
		this.longClassName = dataType.getCanonicalName();
		this.varName = varName;
		this.mgr = mgr;
		exs.submit(() -> {
			try {
				Thread.sleep(10_000);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			shell = mgr.makeShell();
		});
	}

	private @Nullable Predicate<X> makeFilter(@Nullable String filterText) {
		if (shell == null) {
			shell = mgr.makeShell();
		}
		lastFilterText = filterText;
		if (filterText == null || filterText.isBlank()) {
			return null;
		}
		try {
			String checkType = strict ? "@CompileStatic" : "";
			String scriptName = "GroovyFilterScript" + scriptCounter.getAndIncrement();
			String inJavaForm = """
					%s
					boolean test(%s %s) {
						%s
					}
					Predicate<%s> myPredicate = this::test;
					return myPredicate;
					""".formatted(checkType, longClassName, varName, filterText, longClassName);
			Predicate<X> compiled;
			try (SandboxScope ignored = mgr.getSandbox().enter()) {
//				Script script = shell.parse(inJavaForm, scriptName);
				compiled = (Predicate<X>) shell.evaluate(inJavaForm);
//				Script script = shell.parse(inJavaForm, scriptName);
//				compiled = (Predicate<X>) script.invokeMethod("run", null);
			}
			textBox.setToolTipText(null);
			return (x) -> {
				try (SandboxScope ignored = mgr.getSandbox().enter()) {
					return compiled.test(x);
				}
			};
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
//		shell.setVariable(varName, item);
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
			textBox.recheck();
		});
		panel.add(label, BorderLayout.WEST);
		panel.add(textBox, BorderLayout.CENTER);
		panel.add(cb, BorderLayout.EAST);
		cb.setSelected(true);
		strict = cb.isSelected();
		return panel;
	}

	@Override
	public String getName() {
		return "Freeform";
	}
}
