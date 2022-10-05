package gg.xp.xivsupport.callouts.gui;

import gg.xp.xivsupport.callouts.conversions.GlobalCallReplacer;
import gg.xp.xivsupport.callouts.conversions.GlobalReplacement;
import gg.xp.xivsupport.gui.NoCellEditor;
import gg.xp.xivsupport.gui.TitleBorderPanel;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.persistence.settings.CustomJsonListSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public class GlobalReplacementGui extends TitleBorderPanel {
	private static final Logger log = LoggerFactory.getLogger(GlobalReplacementGui.class);
	private final JTable table;
	private final CustomJsonListSetting<GlobalReplacement> setting;
	private final CustomTableModel<GlobalReplacement> model;

	private List<GlobalReplacement> multiSelections = Collections.emptyList();

	public GlobalReplacementGui(GlobalCallReplacer gcr) {
		super("Text Replacements");
		setLayout(new BorderLayout());
		this.setting = gcr.getReplacements();
		model = CustomTableModel.builder(setting::getItems)
				.addColumn(new CustomColumn<>("Find (Regex)", gr -> gr.find, c -> c.setCellEditor(StandardColumns.regexEditorEmptyToNull(safeEdit(false, (item, value) -> item.find = value), Pattern.CASE_INSENSITIVE))))
				.addColumn(new CustomColumn<>("Replace With", gr -> gr.replaceWith, c -> c.setCellEditor(StandardColumns.stringEditorNonNull(safeEdit(false, (item, value) -> item.replaceWith = value)))))
				.addColumn(new CustomColumn<>("TTS", gr -> gr.tts, c -> {
					c.setCellRenderer(StandardColumns.checkboxRenderer);
					c.setCellEditor(new StandardColumns.CustomCheckboxEditor<>(safeEdit(true, (item, value) -> item.tts = value)));
					c.setMinWidth(40);
					c.setMaxWidth(40);
				}))
				.addColumn(new CustomColumn<>("Text", gr -> gr.text, c -> {
					c.setCellRenderer(StandardColumns.checkboxRenderer);
					c.setCellEditor(new StandardColumns.CustomCheckboxEditor<>(safeEdit(true, (item, value) -> item.text = value)));
					c.setMinWidth(40);
					c.setMaxWidth(40);
				}))
				.build();
		table = new JTable(model) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return !(getCellEditor(row, column) instanceof NoCellEditor);
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				super.editingStopped(e);
				commitEdit();
			}

			@Override
			public void editingCanceled(ChangeEvent e) {
				super.editingCanceled(e);
				cancelEdit();
			}
		};
		model.configureColumns(table);
		table.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		setting.addAndRunListener(model::signalNewData);
		{
			JPanel controlsPanel = new JPanel(new FlowLayout());
			{
				JButton newButton = new JButton("Add New");
				controlsPanel.add(newButton);
				newButton.addActionListener(l -> addnew());
			}
			{
				JButton deleteButton = new JButton("Delete") {
					@Override
					public boolean isEnabled() {
						return !multiSelections.isEmpty();
					}
				};
				controlsPanel.add(deleteButton);
				deleteButton.addActionListener(l -> delete());
			}
			add(controlsPanel, BorderLayout.SOUTH);
		}
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	private void delete() {
		multiSelections.forEach(setting::removeItem);
		refresh();
		model.setSelectedValue(null);
	}

	private void addnew() {
		GlobalReplacement gr = new GlobalReplacement();
		setting.addItem(gr);
		refresh();
		SwingUtilities.invokeLater(() -> {
			model.setSelectedValue(gr);
			refreshSelection();
		});

	}

	private static final Runnable NOTHING = () -> {
	};
	private volatile Runnable pendingEdit = NOTHING;

	private <X> BiConsumer<GlobalReplacement, X> safeEdit(boolean stopEditing, BiConsumer<GlobalReplacement, X> editFunc) {
		return (item, value) -> {
			pendingEdit = () -> {
				editFunc.accept(item, value);
			};
			if (stopEditing) {
				stopEditing();
			}
		};
	}

	private void stopEditing() {
		TableCellEditor editor = table.getCellEditor();
		if (editor != null) {
			editor.stopCellEditing();
		}
	}


	private void refresh() {
		model.signalNewData();
		refreshSelection();
	}

	private void refreshSelection() {
		this.multiSelections = model.getSelectedValues();
		repaint();
	}

	private void commitEdit() {
		log.info("Committing edit");
		pendingEdit.run();
		pendingEdit = NOTHING;
		commitAndRefresh();
	}

	private void commitAndRefresh() {
		setting.commit();
		refresh();
	}

	private void cancelEdit() {
		log.info("Cancelling edit");
		pendingEdit = NOTHING;
		commitAndRefresh();
	}

}
