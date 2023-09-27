package gg.xp.xivsupport.gui.groovy;

import gg.xp.xivsupport.groovy.GroovyScriptManager;
import gg.xp.xivsupport.groovy.GroovyScriptResult;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.LateAdjustJSplitPane;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tabs.GroovyTab;
import gg.xp.xivsupport.gui.util.EasyAction;
import gg.xp.xivsupport.persistence.gui.BoundCheckbox;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.GroovySandbox;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxScope;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class GroovyPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger(GroovyPanel.class);
	private static final Color invalidBackground = new Color(62, 27, 27);
	// TODO: way of cancelling computation
	private static final ExecutorService evaluator = Executors.newSingleThreadExecutor();
	private static final ExecutorService saver = Executors.newSingleThreadExecutor();

	private static final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);

	private final RSyntaxTextArea entryArea;
	private final JScrollPane resultScroll;
	private final JScrollPane resultPropertiesScroll;
	private final GroovyScriptManager mgr;
	private final GroovyTab tab;
	private final GroovyScriptHolder script;
	private final GroovySandbox sbx;
	private final BoundCheckbox startupCb;
	private boolean suppressStartupRequest;

	public String getName() {
		return script.getScriptName();
	}

	public GroovyScriptHolder getScript() {
		return script;
	}

	public GroovyPanel(GroovyScriptManager mgr, GroovyTab tab, GroovyScriptHolder script) {
		this.mgr = mgr;
		this.tab = tab;
		this.script = script;
		this.sbx = mgr.getGroovyManager().getSandbox();
		EasyAction newScript = new EasyAction("New", this::newScript, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		EasyAction save = new EasyAction("Save", this::save, script::isSaveable, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		EasyAction saveAll = new EasyAction("Save All", this::saveAll, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
		EasyAction saveAs = new EasyAction("Save As...", this::saveAs, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		// TODO
		EasyAction delete = new EasyAction("Delete", this::deleteSelf, script::isDeletable, null);
		EasyAction rename = new EasyAction("Rename", this::rename, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
//		EasyAction reloadOne = new EasyAction("Reload", this::reload, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
		EasyAction reloadAll = new EasyAction("Reload All", this::reloadAll, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		EasyAction openExt = new EasyAction("Open in External Editor", this::openExt, () -> true, null);
		EasyAction run = new EasyAction("Execute", this::submit, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK));
//		EasyAction run1 = new EasyAction("Execute", this::submit, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK));
//		// Annoying workaround
//		EasyAction run2 = new EasyAction("Execute", this::submit, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_GRAPH_DOWN_MASK));
//		EasyAction run3 = new EasyAction("Execute", this::submit, () -> true, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK));

		newScript.configureComponent(this);
		save.configureComponent(this);
		saveAll.configureComponent(this);
		saveAs.configureComponent(this);
		run.configureComponent(this);
//		run1.configureComponent(this);
//		run2.configureComponent(this);
//		run3.configureComponent(this);
		delete.configureComponent(this);
		rename.configureComponent(this);
//		reloadOne.configureComponent(this);
		reloadAll.configureComponent(this);
		setLayout(new BorderLayout());
		setBorder(new TitledBorder("Groovy"));
		JSplitPane split;
		JToolBar toolbar;
		JPanel top;
		JPanel bottom;

		{
			toolbar = new JToolBar();
			toolbar.add(newScript.asButton());
			toolbar.add(save.asButton());
			toolbar.add(saveAs.asButton());
			toolbar.add(saveAll.asButton());
			toolbar.add(delete.asButton());
			toolbar.add(rename.asButton());
//			toolbar.add(reloadOne.asButton());
			toolbar.add(reloadAll.asButton());
			toolbar.add(openExt.asButton());
			add(toolbar, BorderLayout.NORTH);
		}

		{
			top = new JPanel(new BorderLayout());
			entryArea = new RSyntaxTextArea(script.getScriptContent());
//			run.configureComponent(entryArea);
			entryArea.getDocument().addDocumentListener(new DocumentListener() {
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
			entryArea.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
						run.run();
						e.consume();
					}
					super.keyPressed(e);
				}
			});
			entryArea.setFont(mono);
			// TODO: this is adding some heavy deps
			entryArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
			entryArea.setCodeFoldingEnabled(true);
//			AutoCompletion ac = new AutoCompletion(new LanguageAwareCompletionProvider(new DefaultCompletionProvider()));
//			ac.install(entryArea);
//			DefaultCompletionProvider dcp = new DefaultCompletionProvider();
//			GroovyCompletionProvider completion = new GroovyCompletionProvider() {
//				@Override
//				protected CompletionProvider createCodeCompletionProvider() {
//					JarManager jarMan = new JarManager();
//					try {
////						jarMan.addClassFileSource(LibraryInfo.getMainJreJarInfo());
//						jarMan.addClassFileSource(new File("C:\\Users\\Matt\\Desktop\\triggevent\\deps\\xivsupport-1.0-SNAPSHOT.jar"));
//						jarMan.addClassFileSource(new File("./xivsupport/target/xivsupport-1.0-SNAPSHOT.jar"));
////						jarMan.addClassFileSource(LibraryInfo.getMainJreJarInfo());
//					}
//					catch (IOException e) {
//						throw new RuntimeException(e);
//					}
//					return new GroovySourceCompletionProvider(jarMan);
//				}
//			};
//			completion.setParent(dcp);
//			AutoCompletion ac = new AutoCompletion(completion);
//			ac.setAutoActivationEnabled(true);
//			ac.setAutoActivationDelay(10);
////			ac.setAutoCompleteEnabled(true);
//			ac.setShowDescWindow(true);
//			ac.install(entryArea);
			try {
				Theme theme = Theme.load(GroovyPanel.class.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
				theme.apply(entryArea);
			}
			catch (IOException e) {
				log.error("Error loading editor theme!", e);
			}
			JScrollPane entryScroll = new RTextScrollPane(entryArea);
			top.add(entryScroll, BorderLayout.CENTER);
			{
				JButton runButton = run.asButtonWithKeyLabel();
				JPanel buttonHolder = new JPanel(new WrapLayout(WrapLayout.LEFT));
				buttonHolder.add(runButton);
				startupCb = new BoundCheckbox("Run on Startup", script::isStartup, startup -> {
					script.setStartup(startup);
					script.save();
				});
				buttonHolder.add(startupCb);
				top.add(buttonHolder, BorderLayout.SOUTH);
//				runButton.addActionListener(l -> submit());
			}
			top.add(new ReadOnlyText("DO NOT run random scripts from the internet!"), BorderLayout.NORTH);
		}
		{
			this.resultScroll = new JScrollPane();
			this.resultPropertiesScroll = new JScrollPane();
			JSplitPane jsp = new LateAdjustJSplitPane(JSplitPane.VERTICAL_SPLIT, resultScroll, resultPropertiesScroll);
			bottom = new JPanel(new BorderLayout());
//			bottom.setPreferredSize(bottom.getMaximumSize());
			bottom.add(jsp, BorderLayout.CENTER);
			jsp.setOneTouchExpandable(true);
			jsp.setResizeWeight(0.8);
			jsp.setDividerLocation(0.8);
			jsp.setDividerSize(10);
		}
		{
			split = new LateAdjustJSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
			add(split, BorderLayout.CENTER);
			split.setOneTouchExpandable(true);
			split.setResizeWeight(0.5);
			split.setDividerLocation(0.5);
			split.setDividerSize(10);
		}
		GroovyScriptResult result = script.getLastResult();
		if (result != null) {
			setResult(result);
		}
	}

	private void deleteSelf() {
		// TODO: confirmation dialog
		mgr.delete(script);
	}

	private void newScript() {
		ScriptNameDialog dialog = new ScriptNameDialog("New Script", null, mgr, this, newNameAndFile -> {
			GroovyScriptHolder newScript = mgr.createAndAddNew(newNameAndFile);
			tab.selectScript(newScript);
		});
		dialog.setVisible(true);
	}
//
//	private void reload() {
//		mgr.reloadScript(script);
//	}

	private void reloadAll() {
		mgr.reloadAll();
	}

	private void save() {
		script.save();
	}

	private void saveAll() {
		mgr.saveAll();
	}

	private void saveAs() {
		ScriptNameDialog dialog = new ScriptNameDialog("Save As", script.getScriptName() + " copy", mgr, this, newNameAndFile -> {
			GroovyScriptHolder newScript = mgr.cloneAs(script, newNameAndFile);
			tab.selectScript(newScript);
			newScript.save();
		});
		dialog.setVisible(true);
	}

	private void rename() {
		ScriptNameDialog dialog = new ScriptNameDialog("Rename", script.getScriptName(), mgr, this,
				newNameAndFile -> mgr.renameScript(script, newNameAndFile));
		dialog.setVisible(true);
	}

	private void openExt() {
		script.save();
		try {
			Desktop.getDesktop().open(script.getFile());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void update() {
		script.setScriptContent(entryArea.getText());
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
				.addColumn(new CustomColumn<>("Value", GroovyPanel::singleValueConversion))
				.build()
				.makeTable();
	}

	private JTable customTableListDisplay(DisplayControl dc, Collection<?> values) {
		return dc.getListDisplay().makeTable(sbx, values);
	}

	private JTable simpleMapDisplay(Map<?, ?> map) {
		return CustomTableModel.builder(() -> new ArrayList<>(map.entrySet()))
				.addColumn(new CustomColumn<>("Key", e -> singleValueConversion(e.getKey())))
				.addColumn(new CustomColumn<>("Value", e -> singleValueConversion(e.getValue())))
				.build()
				.makeTable();
	}

	// TODO: move this
	@SuppressWarnings("MalformedFormatString")
	public static String singleValueConversion(Object obj) {
		if (obj == null) {
			return "(null)";
		}
		if (obj instanceof Byte || obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
			return String.format("%d (0x%x)", obj, obj);
		}
		// TODO: arrays
//		if (obj instanceof Array arr) {
//			arr.getClass().arrayType()
//		}
		if (obj.getClass().isArray()) {
			int length = Array.getLength(obj);
			List<Object> converted = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				converted.add(Array.get(obj, i));
			}
			return converted.stream().map(GroovyPanel::singleValueConversion).collect(Collectors.joining(", ", "[", "]"));
		}
		return obj.toString();

	}

	private void submit() {
		setResultDisplay(null, textDisplayComponent("Processing..."));
		if (script.isSaveable()) {
			script.save();
		}
		evaluator.submit(() -> {
			AtomicBoolean scriptActive = new AtomicBoolean(true);
			ScriptSettingsControl ssc = new ScriptSettingsControl() {
				@Override
				public void requestRunOnStartup() {
					// Only ask for startup permissions if all of the following are true:
					// 1. Script is not already set to run on startup
					// 2. User has not already opted out of having the script run on startup (TODO make this persistent)
					// 3. Script is still running, e.g. don't allow the script to request startup as an asynchronous action
					if (script.isSaveable() && !script.isStartup() && !suppressStartupRequest && scriptActive.get()) {
						startupRequest();
					}
				}
			};
			GroovyScriptResult result = script.run(ssc);
			scriptActive.set(false);
			setResult(result);
		});
	}

	private void startupRequest() {
		SwingUtilities.invokeLater(() -> {
			Component dialogParent = tab;
			if (!dialogParent.isVisible()) {
				dialogParent = tab.getRootPane();
			}
			// TODO: allow this message to be suppressed persistently
			int result = JOptionPane.showConfirmDialog(dialogParent, "This script has requested to be run on startup. Allow?", "Run on Startup?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (result == JOptionPane.YES_OPTION) {
				script.setStartup(true);
			}
			else {
				suppressStartupRequest = true;
			}
			startupCb.repaint();
			save();
		});
	}

	private void setResult(GroovyScriptResult resultHolder) {
		try (var ignored = sbx.enter()){
			if (resultHolder.success()) {
				Object result = resultHolder.result();
				if (result == null) {
					setResultDisplay(result, textDisplayComponent("null"));
				}
				else if (result instanceof Throwable t) {
					setResultDisplay(result, textDisplayComponent(ExceptionUtils.getStackTrace(t)));
				}
				else if (result instanceof Map map) {
					setResultDisplay(result, simpleMapDisplay(map));
				}
				else if (result instanceof Collection coll) {
					setResultDisplay(result, listDisplay(resultHolder, coll));
				}
				else if (result.getClass().isArray()) {
					try {
						int length = Array.getLength(result);
						List<Object> converted = new ArrayList<>();
						for (int i = 0; i < length; i++) {
							converted.add(Array.get(result, i));
						}
						setResultDisplay(result, simpleListDisplay(converted));
					}
					catch (Throwable t) {
						log.error("Error converting array to list", t);
						setResultDisplay(result, textDisplayComponent("This was supposed to be an array, but there was an error converting it to a list.\n\n" + result));
					}

				}
				else if (result instanceof Component comp) {
					setResultDisplay(result, comp);
				}
//				else if (result instanceof String string) {
//					setResultDisplay(textDisplayComponent(string));
//				}
				else {
					setResultDisplay(result, textDisplayComponent(result.toString()));
				}
			}
			else {
				//noinspection ConstantConditions
				Throwable failure = resultHolder.failure();
				setResultDisplay(failure, errorDisplayComponent(ExceptionUtils.getStackTrace(failure)));
			}
		}
		catch (Throwable t) {
			setResultDisplay(t, errorDisplayComponent(ExceptionUtils.getStackTrace(t)));
		}
	}

	private Component listDisplay(GroovyScriptResult result, Collection<?> coll) {
		return customTableListDisplay(result.displayControl(), coll);
	}

	private void setResultDisplay(@Nullable Object obj, Component display) {
		Map<?, ?> props;
		if (obj == null) {
			props = Collections.emptyMap();
		}
		else {
			try {
				props = DefaultGroovyMethods.getProperties(obj);
			}
			catch (Throwable t) {
				props = Collections.emptyMap();
			}
		}
		JTable md = simpleMapDisplay(props);
		SwingUtilities.invokeLater(() -> {
			resultPropertiesScroll.setViewportView(md);
			resultScroll.setViewportView(display);
		});
	}

}
