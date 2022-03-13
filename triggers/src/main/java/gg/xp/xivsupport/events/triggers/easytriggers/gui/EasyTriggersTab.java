package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.MultiLineTextAreaWithValidation;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class EasyTriggersTab implements PluginTab {

	private final EasyTriggers backend;
	private JPanel detailsInner;
	private CustomTableModel<EasyTrigger<?>> model;
	private TitleBorderFullsizePanel outer;
	private EasyTrigger<?> selection;

	public EasyTriggersTab(EasyTriggers backend) {
		this.backend = backend;
	}

	@Override
	public String getTabName() {
		return "Easy Triggers";
	}

	@Override
	public Component getTabContents() {
		outer = new TitleBorderFullsizePanel("Easy Triggers") {
			@Override
			public void setVisible(boolean aFlag) {
				if (!aFlag && backend != null) {
					backend.commit();
				}
				super.setVisible(aFlag);
			}
		};
		outer.setLayout(new GridBagLayout());
		GridBagConstraints c = GuiUtil.defaultGbc();
		c.weighty = 1;

		model = CustomTableModel.builder(backend::getTriggers)
				.addColumn(new CustomColumn<>("En", EasyTrigger::isEnabled, col -> {
					col.setCellRenderer(StandardColumns.checkboxRenderer);
					col.setCellEditor(new StandardColumns.CustomCheckboxEditor<EasyTrigger<?>>(EasyTrigger::setEnabled));
					col.setMinWidth(22);
					col.setMaxWidth(22);
				}))
				.addColumn(new CustomColumn<>("Name", EasyTrigger::getName))
				.addColumn(new CustomColumn<>("Event Type", t -> t.getEventType().getSimpleName()))
				.addColumn(new CustomColumn<>("Conditions", t -> String.format("(%d) %s", t.getConditions().size(), t.getConditions().stream().map(Condition::dynamicLabel).collect(Collectors.joining("; ")))))
				.addColumn(new CustomColumn<>("TTS", EasyTrigger::getTts))
				.addColumn(new CustomColumn<>("Text", EasyTrigger::getText))
				.addColumn(new CustomColumn<>("Hit", EasyTrigger::getHits, 80))
				.addColumn(new CustomColumn<>("Miss", EasyTrigger::getMisses, 80))
				.build();
		JTable triggerChooserTable = new JTable(model) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 0;
			}
		};
		model.configureColumns(triggerChooserTable);
		triggerChooserTable.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		// TODO: multi-select would actually be very useful here
		triggerChooserTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		{
			JPanel controlsPanel = new JPanel(new FlowLayout());
			{
				JButton newTriggerButton = new JButton("New Trigger");
				controlsPanel.add(newTriggerButton);
				newTriggerButton.addActionListener(l -> addnew());
			}
			{
				JButton deleteTriggerButton = new JButton("Delete Trigger") {
					@Override
					public boolean isEnabled() {
						return selection != null;
					}
				};
				controlsPanel.add(deleteTriggerButton);
				deleteTriggerButton.addActionListener(l -> delete());
			}
			{
				JButton cloneTriggerButton = new JButton("Clone Trigger") {
					@Override
					public boolean isEnabled() {
						return selection != null;
					}
				};
				controlsPanel.add(cloneTriggerButton);
				cloneTriggerButton.addActionListener(l -> cloneCurrent());
			}
			{
				JButton exportTriggerButton = new JButton("Export Trigger") {
					@Override
					public boolean isEnabled() {
						return selection != null;
					}
				};
				controlsPanel.add(exportTriggerButton);
				exportTriggerButton.addActionListener(l -> exportCurrent());
			}
			{
				JButton importTriggerButton = new JButton("Import Trigger") {
					@Override
					public boolean isEnabled() {
						return selection != null;
					}
				};
				controlsPanel.add(importTriggerButton);
				importTriggerButton.addActionListener(l -> showImportDialog());
			}
			bottomPanel.add(controlsPanel, BorderLayout.NORTH);
		}

		{
			this.detailsInner = new JPanel(new BorderLayout());
			JScrollPane detailsScroller = new JScrollPane(detailsInner);
			detailsScroller.setBorder(null);
			detailsScroller.setPreferredSize(detailsScroller.getMaximumSize());
			TitleBorderFullsizePanel detailsOuter = new TitleBorderFullsizePanel("Trigger Details");
			detailsOuter.setLayout(new BorderLayout());
			detailsOuter.add(detailsScroller, BorderLayout.CENTER);
			bottomPanel.add(detailsOuter, BorderLayout.CENTER);
		}

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(triggerChooserTable), bottomPanel);
		split.setOneTouchExpandable(true);
		outer.add(split, c);
		split.setResizeWeight(0.5);
		split.setDividerLocation(300);

		RefreshLoop<EasyTriggersTab> refresher = new RefreshLoop<>("EasyTriggerAutoSave", this, ett -> {
			if (outer.isShowing()) {
				ett.backend.commit();
				saveAnyway = true;
			}
			else if (saveAnyway) {
				ett.backend.commit();
				saveAnyway = false;
			}
		}, (unused) -> 5000L);

		refresher.start();
		return outer;
	}

	private void showImportDialog() {
		Mutable<List<EasyTrigger<?>>> value = new MutableObject<>();
		MultiLineTextAreaWithValidation<List<EasyTrigger<?>>> field = new MultiLineTextAreaWithValidation<>(EasyTriggers::importFromString, value::setValue, "");
		field.setPreferredSize(new Dimension(500, 500));
		field.setLineWrap(true);
		field.setWrapStyleWord(true);
		JOptionPane opt = new JOptionPane(field, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = opt.createDialog("Import Triggers");
		dialog.setVisible(true);
		Object dialogResult = opt.getValue();
		if (dialogResult instanceof Integer dr && dr == JOptionPane.OK_OPTION && value.getValue() != null) {
			addImports(value.getValue());
		}
	}

	private void exportCurrent() {
		EasyTrigger<?> selection = this.selection;
		if (selection != null) {
			GuiUtil.copyToClipboard(EasyTriggers.exportToString(selection));
		}
		JOptionPane.showMessageDialog(outer, "Copied to clipboard");
	}

	// Basically, since I didn't think about a good way to auto-save, the auto-save loop saves continuously when the tab
	// is showing, immediately when the tab is no longer visible, BUT that visibility check only counts for the direct
	// parent tab, not the main tab bar. Thus, we need another check to do one more save when we are no longer showing.
	private boolean saveAnyway;

	private void refreshSelection() {
		setSelection(model.getSelectedValue());
	}

	private void setSelection(@Nullable EasyTrigger<?> selection) {
		this.selection = selection;
		SwingUtilities.invokeLater(() -> {
			detailsInner.removeAll();
			if (selection != null) {
				detailsInner.add(new TriggerConfigPanel(selection));
			}
			detailsInner.revalidate();
			detailsInner.repaint();
			outer.repaint();
		});
	}

	private void refresh() {
		model.signalNewData();
	}

	private void cloneCurrent() {
		EasyTrigger<?> selectedValue = model.getSelectedValue();
		if (selectedValue == null) {
			return;
		}
		EasyTrigger<?> newTrigger = selectedValue.duplicate();
		backend.addTrigger(newTrigger);
		setSelection(newTrigger);
		refresh();
	}

	private void addnew() {
		TableWithFilterAndDetails<EventDescription<?>, Object> table = TableWithFilterAndDetails.builder("Choose Event Type", EasyTriggers::getEventDescriptions)
				.addMainColumn(new CustomColumn<>("Event", d -> d.type().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Description", EventDescription::description))
				.setFixedData(true)
				.build();
		// TODO: owner
		EventDescription<?> eventDescription = ChooserDialog.chooserReturnItem(SwingUtilities.getWindowAncestor(outer), table);
		if (eventDescription != null) {
			EasyTrigger<?> newTrigger = eventDescription.newInst();
			backend.addTrigger(newTrigger);
			refresh();
			SwingUtilities.invokeLater(() -> model.setSelectedValue(newTrigger));
			setSelection(newTrigger);
		}
	}

	private void addImports(List<EasyTrigger<?>> toAdd) {
		toAdd.forEach(backend::addTrigger);
		refresh();
	}

	private void delete() {
		EasyTrigger<?> selectedValue = model.getSelectedValue();
		if (selectedValue == null) {
			return;
		}
		backend.removeTrigger(selectedValue);
		refresh();
		model.setSelectedValue(null);
	}

	private void requestSave() {

	}


	// TODO: autosave

	private class TriggerConfigPanel extends JPanel {

		private final EasyTrigger<?> trigger;

		TriggerConfigPanel(EasyTrigger<?> trigger) {
			setLayout(new GridBagLayout());
			GridBagConstraints c = GuiUtil.defaultGbc();
			c.insets = new Insets(1, 2, 1, 2);
			c.anchor = GridBagConstraints.NORTH;

			this.trigger = trigger;

			JTextField eventTypeField = new JTextField(trigger.getEventType().getSimpleName());
			eventTypeField.setEditable(false);
			TextFieldWithValidation<String> nameField = new TextFieldWithValidation<>(str -> {
				if (str.isBlank()) {
					throw new IllegalArgumentException("Cannot be blank");
				}
				return str;
			}, editTriggerThenSave(trigger::setName), trigger.getName());
			TextFieldWithValidation<String> ttsField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(trigger::setTts), trigger.getTts());
			TextFieldWithValidation<String> textField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(trigger::setText), trigger.getText());

			JPanel conditionsPanel = new ConditionsPanel("Conditions", trigger);

			c.weightx = 0;
			JLabel firstLabel = GuiUtil.labelFor("Name", nameField);
			Dimension pref = firstLabel.getPreferredSize();
			Dimension newSize = new Dimension(pref.width + 20, pref.height);
			firstLabel.setPreferredSize(newSize);
			firstLabel.setMaximumSize(newSize);
			add(firstLabel, c);
			c.weightx = 1;
			c.gridx++;
			add(nameField, c);
			c.weightx = 0;
			c.gridx = 0;
			c.gridy++;
			add(GuiUtil.labelFor("Event", eventTypeField), c);
			c.gridx++;
			add(eventTypeField, c);
			c.gridx = 0;
			c.gridy++;
			add(GuiUtil.labelFor("TTS", ttsField), c);
			c.gridx++;
			add(ttsField, c);
			c.gridx = 0;
			c.gridy++;
			add(GuiUtil.labelFor("Text", textField), c);
			c.gridx++;
			add(textField, c);
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = GridBagConstraints.REMAINDER;
			add(conditionsPanel, c);
			c.gridy++;
			c.weighty = 1;
			add(Box.createGlue(), c);
		}

		private <X> Consumer<X> editTriggerThenSave(Consumer<X> modification) {
			return modification.andThen((unused) -> requestSave());
		}
	}

}
