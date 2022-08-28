package gg.xp.xivsupport.gui.groovy;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tabs.GroovyTab;
import gg.xp.xivsupport.gui.util.EasyAction;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroovyPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger(GroovyPanel.class);
	private static final Color invalidBackground = new Color(62, 27, 27);
	// TODO: way of cancelling computation
	private static final ExecutorService evaluator = Executors.newSingleThreadExecutor();

	private static final Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);

	private final JTextArea entryArea;
	private final JScrollPane resultScroll;
	private final GroovyManager mgr;
	private final GroovyTab tab;
	private final GroovyScriptHolder script;

	public String getName() {
		return script.getScriptName();
	}

	public GroovyScriptHolder getScript() {
		return script;
	}

	public GroovyPanel(GroovyManager mgr, GroovyTab tab, GroovyScriptHolder script) {
		this.mgr = mgr;
		this.tab = tab;
		this.script = script;
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
		newScript.configureComponent(this);
		save.configureComponent(this);
		saveAll.configureComponent(this);
		saveAs.configureComponent(this);
		run.configureComponent(this);
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
			entryArea = new JTextArea(script.getScriptContent());
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
			entryArea.setFont(mono);
			JScrollPane entryScroll = new JScrollPane(entryArea);
			top.add(entryScroll, BorderLayout.CENTER);
			{
				JButton runButton = run.asButtonWithKeyLabel();
				JPanel buttonHolder = new JPanel(new WrapLayout(WrapLayout.LEFT));
				buttonHolder.add(runButton);
				top.add(buttonHolder, BorderLayout.SOUTH);
//				runButton.addActionListener(l -> submit());
			}
			top.add(new ReadOnlyText("DO NOT run random scripts from the internet!"), BorderLayout.NORTH);
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

	private JTable simpleMapDisplay(Map<?, ?> map) {
		return CustomTableModel.builder(() -> new ArrayList<>(map.entrySet()))
				.addColumn(new CustomColumn<>("Key", e -> singleValueConversion(e.getKey())))
				.addColumn(new CustomColumn<>("Value", e -> singleValueConversion(e.getValue())))
				.build()
				.makeTable();
	}

	private static String singleValueConversion(Object obj) {
		if (obj == null) {
			return "(null)";
		}
		if (obj instanceof Byte || obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
			return String.format("%d (0x%x)", obj, obj);
		}
		return obj.toString();

	}

	private void submit() {
		setResultDisplay(textDisplayComponent("Processing..."));
		evaluator.submit(() -> {
			GroovyScriptResult result = script.run();
			setResult(result);
		});
	}

	private void setResult(GroovyScriptResult resultHolder) {
		try {
			if (resultHolder.success()) {
				Object result = resultHolder.result();
				if (result == null) {
					setResultDisplay(textDisplayComponent("null"));
				}
				else if (result instanceof Throwable t) {
					setResultDisplay(textDisplayComponent(ExceptionUtils.getStackTrace(t)));
				}
				else if (result instanceof Map map) {
					setResultDisplay(simpleMapDisplay(map));
				}
				else if (result instanceof Collection coll) {
					setResultDisplay(simpleListDisplay(coll));
				}
				else if (result.getClass().isArray()) {
					try {
						int length = Array.getLength(result);
						List<Object> converted = new ArrayList<>();
						for (int i = 0; i < length; i++) {
							converted.add(Array.get(result, i));
						}
						setResultDisplay(simpleListDisplay(converted));
					}
					catch (Throwable t) {
						log.error("Error converting array to list", t);
						setResultDisplay(textDisplayComponent("This was supposed to be an array, but there was an error converting it to a list.\n\n" + result));
					}

				}
				else if (result instanceof Component comp) {
					setResultDisplay(comp);
				}
				else {
					setResultDisplay(textDisplayComponent(result.toString()));
				}
			}
			else {
				//noinspection ConstantConditions
				setResultDisplay(errorDisplayComponent(ExceptionUtils.getStackTrace(resultHolder.failure())));
			}
		}
		catch (Throwable t) {
			setResultDisplay(errorDisplayComponent(ExceptionUtils.getStackTrace(t)));
		}
	}

	private void setResultDisplay(Component display) {
		SwingUtilities.invokeLater(() -> resultScroll.setViewportView(display));
	}

}
