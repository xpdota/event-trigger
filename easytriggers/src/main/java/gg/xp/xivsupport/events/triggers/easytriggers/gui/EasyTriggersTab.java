package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.ActLegacyTriggerImport;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.InputValidationState;
import gg.xp.xivsupport.gui.tables.filters.MultiLineTextAreaWithValidation;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import javax.print.attribute.standard.JobKOctets;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
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
	private List<EasyTrigger<?>> multiSelections = Collections.emptyList();

	public EasyTriggersTab(EasyTriggers backend, RightClickOptionRepo rightClicks) {
		this.backend = backend;
		rightClicks.addOption(CustomRightClickOption.forRow(
				"Make Easy Trigger",
				Event.class,
				this::makeTriggerFromEvent));
		// TODO: good candidate for sub-menus
//				.addRightClickOption(CustomRightClickOption.forRowWithConverter("Make Easy Trigger", Event.class, Function.identity(), e -> {
//					container.getComponent(EasyTrig)
//					GuiUtil.copyTextToClipboard(line.getFields().toString());
//				}))
	}

	@Override
	public String getTabName() {
		return "Easy Triggers";
	}

	@Override
	public Component getTabContents() {
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

		outer = new TitleBorderFullsizePanel("Easy Triggers") {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					model.signalNewData();
				}
				else {
					backend.commit();
				}
				super.setVisible(visible);
			}
		};

		outer.setLayout(new GridBagLayout());
		triggerChooserTable.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
		triggerChooserTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		{
			// TODO: most of these could also be right click options
			JPanel controlsPanel = new JPanel(new FlowLayout());
			{
				JButton newTriggerButton = new JButton("New Trigger");
				controlsPanel.add(newTriggerButton);
				newTriggerButton.addActionListener(l -> addnew());
			}
			{
				JButton deleteTriggerButton = new JButton("Delete Selected") {
					@Override
					public boolean isEnabled() {
						return !multiSelections.isEmpty();
					}
				};
				controlsPanel.add(deleteTriggerButton);
				deleteTriggerButton.addActionListener(l -> delete());
			}
			{
				JButton cloneTriggerButton = new JButton("Clone Trigger") {
					@Override
					public boolean isEnabled() {
						return multiSelections.size() == 1;
					}
				};
				controlsPanel.add(cloneTriggerButton);
				cloneTriggerButton.addActionListener(l -> cloneCurrent());
			}
			{
				JButton exportTriggerButton = new JButton("Export Selected") {
					@Override
					public boolean isEnabled() {
						return !multiSelections.isEmpty();
					}
				};
				controlsPanel.add(exportTriggerButton);
				exportTriggerButton.addActionListener(l -> exportCurrent());
			}
			{
				JButton importTriggerButton = new JButton("Import Triggers");
				controlsPanel.add(importTriggerButton);
				importTriggerButton.addActionListener(l -> showEasyImportDialog());
			}
			{
				JButton importLegacyTriggerButton = new JButton("Import Legacy ACT Triggers");
				controlsPanel.add(importLegacyTriggerButton);
				importLegacyTriggerButton.addActionListener(l -> showActImportDialog());
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
		}, (unused) -> 15000L);

		refresher.start();
		return outer;
	}

	private static <X> @Nullable X doImportDialog(String title, Function<String, X> converter) {
		Mutable<X> value = new MutableObject<>();
		JButton okButton = new JButton("Import");
		MultiLineTextAreaWithValidation<X> field = new MultiLineTextAreaWithValidation<>(converter, value::setValue, "", (vs -> okButton.setEnabled(vs == InputValidationState.VALID)));
		JButton cancelButton = new JButton("Cancel");
		field.setPreferredSize(null);
		field.setLineWrap(true);
		field.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(field);
		scrollPane.setPreferredSize(new Dimension(720, 480));
		JOptionPane opt = new JOptionPane(scrollPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new Object[]{okButton, cancelButton});
		okButton.addActionListener(l -> opt.setValue(JOptionPane.OK_OPTION));
		cancelButton.addActionListener(l -> opt.setValue(JOptionPane.CANCEL_OPTION));
		JDialog dialog = opt.createDialog(title);
		dialog.setVisible(true);
		Object dialogResult = opt.getValue();
		X theValue = value.getValue();
		if (dialogResult instanceof Integer dr && dr == JOptionPane.OK_OPTION && theValue != null) {
			return theValue;
		}
		else {
			return null;
		}
	}

	private void showEasyImportDialog() {
		List<EasyTrigger<?>> newTriggers = doImportDialog("Import Triggers", backend::importFromString);
		if (newTriggers != null && !newTriggers.isEmpty()) {
			addImports(newTriggers);
		}
	}

	@SuppressWarnings("unchecked")
	private void showActImportDialog() {
		List<EasyTrigger<ACTLogLineEvent>> newTriggers = doImportDialog("Import Legacy Triggers", ActLegacyTriggerImport::parseMultipleTriggerXmlNonEmpty);
		if (newTriggers != null && !newTriggers.isEmpty()) {
			// :clown_emoji:
			addImports((List<EasyTrigger<?>>) (Object) newTriggers);
		}
	}

	private void exportCurrent() {
		if (!multiSelections.isEmpty()) {
			GuiUtil.copyToClipboard(backend.exportToString(multiSelections));
			JOptionPane.showMessageDialog(outer, "Copied to clipboard");
		}
	}

	// Basically, since I didn't think about a good way to auto-save, the auto-save loop saves continuously when the tab
	// is showing, immediately when the tab is no longer visible, BUT that visibility check only counts for the direct
	// parent tab, not the main tab bar. Thus, we need another check to do one more save when we are no longer showing.
	private boolean saveAnyway;

	private void refreshSelection() {
		this.multiSelections = model.getSelectedValues();
		this.selection = multiSelections.size() == 1 ? multiSelections.get(0) : null;
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
		List<EasyTrigger<?>> newTriggers = backend.importFromString(backend.exportToString(Collections.singletonList(selectedValue)));
		EasyTrigger<?> newTrigger = newTriggers.get(0);
		backend.addTrigger(newTrigger);
		refresh();
		model.setSelectedValue(newTrigger);
//		refreshSelection();
	}

	private void addExisting(EasyTrigger<?> trigger) {
		backend.addTrigger(trigger);
		refresh();
		model.setSelectedValue(trigger);
	}

	private void addnew() {
		TableWithFilterAndDetails<EventDescription<?>, Object> table = TableWithFilterAndDetails.builder("Choose Event Type", backend::getEventDescriptions)
				.addMainColumn(new CustomColumn<>("Event", d -> d.type().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Description", EventDescription::description))
				.setFixedData(true)
				.build();
		// TODO: owner
		EventDescription<?> eventDescription = ChooserDialog.chooserReturnItem(SwingUtilities.getWindowAncestor(outer), table);
		if (eventDescription != null) {
			EasyTrigger<?> newTrigger = eventDescription.newDefaultInst();
			backend.addTrigger(newTrigger);
			refresh();
			SwingUtilities.invokeLater(() -> {
				model.setSelectedValue(newTrigger);
				refreshSelection();
			});
		}
	}

	private void addImports(List<EasyTrigger<?>> toAdd) {
		toAdd.forEach(backend::addTrigger);
		refresh();
	}

	private void delete() {
		multiSelections.forEach(backend::removeTrigger);
		refresh();
		model.setSelectedValue(null);
	}

	private void requestSave() {
		// TODO figure out what the plan is here
	}

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

			JPanel conditionsPanel = new ConditionsPanel(backend, "Conditions", trigger);

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
			int yReset = c.gridy + 1;
			c.gridx = 2;

			{
				TextFieldWithValidation<Long> hangTime = new TextFieldWithValidation<>(Long::parseLong, trigger::setHangTime, () -> Long.toString(trigger.getHangTime()));
				JCheckBox plusDuration = new JCheckBox();
				plusDuration.setSelected(trigger.isUseDuration());
				plusDuration.addActionListener(l -> trigger.setUseDuration(plusDuration.isSelected()));

				JCheckBox useIcon = new JCheckBox();
				useIcon.setSelected(trigger.isUseIcon());
				useIcon.addActionListener(l -> trigger.setUseIcon(useIcon.isSelected()));

				c.gridy = 0;
				add(GuiUtil.labelFor("Hang Time", hangTime), c);
				c.gridy++;
				add(GuiUtil.labelFor("Plus cast/buff duration", plusDuration), c);
				c.gridy++;
				add(GuiUtil.labelFor("Use ability/buff icon", useIcon), c);
				c.gridy = 0;
				c.gridx++;
				add(hangTime, c);
				c.gridy++;
				add(plusDuration, c);
				c.gridy++;
				add(useIcon, c);

			}


			c.gridx = 0;
			c.gridy = yReset;
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


	private void makeTriggerFromEvent(Event event) {
		EasyTrigger<?> newTrigger = backend.makeTriggerFromEvent(event);
		if (newTrigger == null) {
			JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(outer), "Unfortunately, this event type is not possible to automatically make a trigger for.");
		}
		else {
			addExisting(newTrigger);
			GuiUtil.bringToFront(outer);
		}
	}


}
