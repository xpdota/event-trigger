package gg.xp.xivsupport.gui.groovy;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.util.ArrayList;
import java.util.Collection;
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
	private final GroovyScriptHolder script;

	public String getName() {
		return script.scriptName;
	}

	// TODO: global groovy binding
	public GroovyPanel(GroovyScriptHolder script) {
		this.script = script;
		setLayout(new BorderLayout());
		setBorder(new TitledBorder("Groovy"));
		JSplitPane split;
		JPanel top;
		JPanel bottom;

		{
			top = new JPanel(new BorderLayout());
//			top.setPreferredSize(top.getMaximumSize());
			entryArea = new JTextArea(script.scriptContent);
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

	private void update() {
		script.scriptContent = entryArea.getText();
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
		} catch (Throwable t) {
			setResultDisplay(errorDisplayComponent(ExceptionUtils.getStackTrace(t)));
		}
	}

	private void setResultDisplay(Component display) {
		SwingUtilities.invokeLater(() -> resultScroll.setViewportView(display));
	}

}
