package gg.xp.xivsupport.gui.groovy;

import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ScriptNameDialog extends JDialog {

	private final TextFieldWithValidation<String> fileField;
	private final JCheckBox autoFileName;
	private final TextFieldWithValidation<String> nameField;
	private final JButton selectButton;
	private final Consumer<ScriptNameAndFileStub> consumer;
	private String name = "New Script";

	public ScriptNameDialog(String title, GroovyManager mgr, Component parent, Consumer<ScriptNameAndFileStub> consumer) {
		super(SwingUtilities.getWindowAncestor(parent), title);
		this.consumer = consumer;
		Container pane = getContentPane();
		pane.setLayout(new BorderLayout());
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.PAGE_AXIS));
		autoFileName = new JCheckBox("Automatic File Name");
		fileField = new TextFieldWithValidation<>(Function.identity(), filenameStub -> {
			mgr.validateNewScriptFile(filenameStub);
			repaintButton();
		}, () -> name.replaceAll("[^A-Za-z0-9_.-]", "_"));
		autoFileName.addActionListener(l -> {
			boolean sel = autoFileName.isSelected();
			fileField.setEnabled(!sel);
			if (sel) {
				update();
			}
		});
		nameField = new TextFieldWithValidation<>(Function.identity(), scriptName -> {
			mgr.validateNewScriptName(scriptName);
			name = scriptName;
			update();
		}, name);
		fileField.addActionListener(l -> submit());
		nameField.addActionListener(l -> submit());
		autoFileName.setSelected(true);
		fileField.setEnabled(false);
		controlsPanel.add(nameField);
		controlsPanel.add(autoFileName);
		controlsPanel.add(fileField);

		pane.add(controlsPanel, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel(new WrapLayout());
		selectButton = new JButton("Select") {
			@Override
			public boolean isEnabled() {
				return !nameField.hasValidationError() && !fileField.hasValidationError();
			}
		};
		JButton cancel = new JButton("Cancel");
		selectButton.addActionListener(l -> {
			submit();
		});
		cancel.addActionListener(l -> cancel());

		buttonsPanel.add(selectButton);
		buttonsPanel.add(cancel);
		pane.add(buttonsPanel, BorderLayout.SOUTH);

		nameField.recheck();
		fileField.recheck();
		setLocationRelativeTo(parent);
		pack();
		validate();
		setModal(true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private void cancel() {
		setVisible(false);
		dispose();
	}

	private void submit() {
		consumer.accept(new ScriptNameAndFileStub(name, fileField.getText()));
		setVisible(false);
		dispose();
	}

	private boolean autoFileName() {
		return autoFileName.isSelected();
	}

	private void repaintButton() {
		selectButton.repaint();
	}

	private void update() {
		boolean auto = autoFileName();
		fileField.setEnabled(!auto);
		if (auto) {
			fileField.resetText();
			fileField.recheck();
		}
		selectButton.repaint();
	}


}
