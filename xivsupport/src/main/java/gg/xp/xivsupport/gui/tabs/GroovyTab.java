package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.CompileStatic;
import groovy.transform.TypeChecked;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.picocontainer.PicoContainer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import static org.reflections.scanners.Scanners.SubTypes;

public class GroovyTab extends JPanel {

	private static final Logger log = LoggerFactory.getLogger(GroovyTab.class);
	private static final Color invalidBackground = new Color(62, 27, 27);
	// TODO: way of cancelling computation
	private static final ExecutorService evaluator = Executors.newSingleThreadExecutor();

	private static final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);

	private final PicoContainer container;
	private final JTextArea entryArea;
	private final GroovyShell shell;
	private final JScrollPane resultScroll;

	// TODO: global groovy binding
	public GroovyTab(PicoContainer container) {
		setLayout(new BorderLayout());
		setBorder(new TitledBorder("Groovy"));
		JSplitPane split;
		JPanel top;
		JPanel bottom;

		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addImports(
				Predicate.class.getCanonicalName(),
				CompileStatic.class.getCanonicalName(),
				TypeChecked.class.getCanonicalName());
		importCustomizer.addStarImports(
				"gg.xp.xivsupport.events.actlines.events",
				"javax.swing",
				"gg.xp.xivsupport.gui",
				"gg.xp.xivsupport.gui.tables"
		);
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
						.setUrls(ClasspathHelper.forJavaClassPath())
						.setScanners(Scanners.SubTypes));
		reflections.get(SubTypes.of(Event.class).asClass())
				.stream()
				.map(Class::getCanonicalName)
				.filter(Objects::nonNull)
				.forEach(importCustomizer::addImports);

		compilerConfiguration.addCompilationCustomizers(importCustomizer);
		Binding binding = new Binding() {
			@Override
			public Object getProperty(String property) {
				return super.getProperty(property);
			}
		};
		shell = new GroovyShell(binding, compilerConfiguration);
		container.getComponents().forEach(item -> {
			String simpleName = item.getClass().getSimpleName();
			simpleName = StringUtils.uncapitalize(simpleName);
			binding.setProperty(simpleName, item);
		});
		// TODO: find a way to systematically do these
		binding.setProperty("xivState", container.getComponent(XivState.class));

		this.container = container;
		{
			top = new JPanel(new BorderLayout());
//			top.setPreferredSize(top.getMaximumSize());
			entryArea = new JTextArea(defaultScript);
			entryArea.setFont(mono);
			JScrollPane entryScroll = new JScrollPane(entryArea);
			top.add(entryScroll, BorderLayout.CENTER);
			{
				JButton runButton = new JButton("Execute (Ctrl-Enter)");
				JPanel buttonHolder = new JPanel(new WrapLayout(WrapLayout.LEFT));
				buttonHolder.add(runButton);
				top.add(buttonHolder, BorderLayout.SOUTH);
				runButton.addActionListener(l -> submit());
			}
			top.add(new ReadOnlyText("DO NOT run random scripts from the internet!"), BorderLayout.NORTH);
			entryArea.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					int code = e.getKeyCode();
					if (code == KeyEvent.VK_ENTER && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
						submit();
					}
					super.keyPressed(e);
				}
			});
		}
		{
			this.resultScroll = new JScrollPane();
			bottom = new JPanel(new BorderLayout());
//			bottom.setPreferredSize(bottom.getMaximumSize());
			bottom.add(resultScroll, BorderLayout.CENTER);
		}
		{
			split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
			split.setOneTouchExpandable(true);
			split.setDividerLocation(0.5);
			split.setResizeWeight(0.5);
			split.setDividerSize(10);
			add(split, BorderLayout.CENTER);
		}
	}

	private JTextArea textDisplayComponent(String text) {
		JTextArea resultsArea = new JTextArea(text);
		resultsArea.setFont(mono);
		resultsArea.setLineWrap(true);
		resultsArea.setWrapStyleWord(true);
		resultsArea.setEditable(false);
		resultsArea.setCaretPosition(0);
		return resultsArea;
	}

	private JTextArea errorDisplayComponent(String text) {
		JTextArea resultsArea = textDisplayComponent(text);
		resultsArea.setBackground(invalidBackground);
		return resultsArea;
	}

	private JTable simpleListDisplay(Collection<?> values) {
		return CustomTableModel.builder(() -> new ArrayList<>(values))
				.addColumn(new CustomColumn<>("Value", GroovyTab::singleValueConversion))
				.build()
				.makeTable();
	}

	private JTable simpleMapDisplay(Map<?, ?> map) {
		return CustomTableModel.builder(() -> new ArrayList<>(map.entrySet()))
				.addColumn(new CustomColumn<>("Key", e -> singleValueConversion(e.getKey())))
				.addColumn(new CustomColumn<>("Value", e -> singleValueConversion(e.getValue())))
				.build()
				.makeTable();
	}

	private static String singleValueConversion(Object obj) {
		if (obj instanceof Byte || obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
			return String.format("%d (0x%x)", obj, obj);
		}
		return obj.toString();

	}

	private void submit() {
		setResult("Processing...");
		String text = entryArea.getText();
		evaluator.submit(() -> {
			try {
				Object result = shell.parse(text).run();
				setResult(result);
			}
			catch (Throwable t) {
				setResult(t);
			}
		});
	}

	private void setResult(Object result) {
		if (result == null) {
			setResultDisplay(textDisplayComponent("null"));
		}
		else if (result instanceof Throwable t) {
			setResultDisplay(errorDisplayComponent(ExceptionUtils.getStackTrace(t)));
		}
		else if (result instanceof Map map) {
			setResultDisplay(simpleMapDisplay(map));
		}
		else if (result instanceof Collection coll) {
			setResultDisplay(simpleListDisplay(coll));
		}
		else if (result instanceof Component comp) {
			setResultDisplay(comp);
		}
		else {
			setResultDisplay(textDisplayComponent(result.toString()));
		}
	}

	private void setResultDisplay(Component display) {
		SwingUtilities.invokeLater(() -> resultScroll.setViewportView(display));
	}

	private static final String defaultScript = """
			\"""Hi There!

			This is the Groovy Console. You can run scripts here, written in Groovy (https://groovy-lang.org/).
			For the most part, Java code will also be valid Groovy code, so you can also use this to prototype mainline code.

			By default, everything in the DI container is injected as a variable, with the first letter of the class name lowercased.

			For example, I can see that there are currently ${rawEventStorage.getEvents().size()} events on record. The current player name is ${xivState.getPlayer()?.getName()}.

			Your return type can be a String, a List, Map, or Swing Component. The value will be rendered differently according to its type. In this case, it is a String.
			
			This does NOT have any sandboxing, so don't run random stuff you found on the internet. It can do anything to your system that compiled Java code would be able to do. \"""
			""";
}
