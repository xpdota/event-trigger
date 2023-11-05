package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.ActLegacyTriggerImport;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.creators.EasyTriggerCreationQuestions;
import gg.xp.xivsupport.events.triggers.easytriggers.events.EasyTriggersInitEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerConcurrencyMode;
import gg.xp.xivsupport.gui.GuiMain;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.lists.FriendlyNameListCellRenderer;
import gg.xp.xivsupport.gui.nav.GlobalUiRegistry;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@ScanMe
public class EasyTriggersTab implements PluginTab {

	private static final Logger log = LoggerFactory.getLogger(EasyTriggersTab.class);

	private final EasyTriggers backend;
	private final GlobalUiRegistry tabReg;
	private final PicoContainer pico;
	private final ExecutorService exs = Executors.newSingleThreadExecutor();
	private JPanel detailsInner;
	private CustomTableModel<EasyTrigger<?>> model;
	private TitleBorderFullsizePanel outer;
	private EasyTrigger<?> selection;
	private List<EasyTrigger<?>> multiSelections = Collections.emptyList();
	private RefreshLoop<EasyTriggersTab> autoSave;

	public EasyTriggersTab(EasyTriggers backend, RightClickOptionRepo rightClicks, GlobalUiRegistry tabReg, PicoContainer pico) {
		this.backend = backend;
		this.tabReg = tabReg;
		this.pico = pico;
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
					col.setCellEditor(new StandardColumns.CustomCheckboxEditor<EasyTrigger<?>>((easyTrigger, enabled) -> {
						easyTrigger.setEnabled(enabled);
						requestSave();
					}));
					col.setMinWidth(22);
					col.setMaxWidth(22);
				}))
				.addColumn(new CustomColumn<>("Name", EasyTrigger::getName))
				.addColumn(new CustomColumn<>("Event Type", t -> t.getEventType().getSimpleName()))
				.addColumn(new CustomColumn<>("Actions", t -> String.format("(%d) %s", t.getActions().size(), t.getActions().stream().map(Action::dynamicLabel).collect(Collectors.joining("; ")))))
				.addColumn(new CustomColumn<>("Conditions", t -> String.format("(%d) %s", t.getConditions().size(), t.getConditions().stream().map(Condition::dynamicLabel).collect(Collectors.joining("; ")))))
				// TODO: replace these with a description of actions
//				.addColumn(new CustomColumn<>("TTS", EasyTrigger::getTts))
//				.addColumn(new CustomColumn<>("Text", EasyTrigger::getText))
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
			detailsScroller.getVerticalScrollBar().setUnitIncrement(20);
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

		this.autoSave = new RefreshLoop<>("EasyTriggerAutoSave", this, ett -> {
			if (outer.isShowing()) {
				ett.backend.commit();
				saveAnyway = true;
			}
			else if (saveAnyway) {
				ett.backend.commit();
				saveAnyway = false;
			}
		}, (unused) -> 15000L);

		autoSave.start();
		return outer;
	}

	private static <X> @Nullable X doImportDialog(String title, Function<String, X> converter) {
		return GuiUtil.doImportDialog(title, converter);
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
			GuiUtil.copyTextToClipboard(backend.exportToString(multiSelections));
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
		model.scrollToSelectedValue();
//		refreshSelection();
	}

	private void addExisting(EasyTrigger<?> trigger) {
		backend.addTrigger(trigger);
		selectTrigger(trigger);
	}

	public void selectTrigger(EasyTrigger<?> trigger) {
		SwingUtilities.invokeLater(() -> {
			if (model != null) {
				refresh();
				SwingUtilities.invokeLater(() -> {
					model.setSelectedValue(trigger);
					model.scrollToSelectedValue();
				});
			}
		});
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
				model.scrollToSelectedValue();
			});
		}
	}

	private void addImports(List<EasyTrigger<?>> toAdd) {
		if (toAdd.isEmpty()) {
			return;
		}
		toAdd.forEach(backend::addTrigger);
		refresh();
		model.setSelectedValue(toAdd.get(0));
		model.scrollToSelectedValue();
	}

	private void delete() {
		multiSelections.forEach(backend::removeTrigger);
		refresh();
		model.setSelectedValue(null);
	}

	private volatile long saveAt;
	private static final int saveDelay = 500;

	private void requestSave() {
		boolean submitTask = saveAt <= 0;
		saveAt = System.currentTimeMillis() + saveDelay;
		if (submitTask) {
			exs.submit(() -> {
				while (true) {
					long delta = saveAt - System.currentTimeMillis();
					if (delta <= 0) {
						break;
					}
					else {
						try {
							Thread.sleep(delta);
						}
						catch (InterruptedException e) {
							break;
						}
					}
				}
				saveAt = 0;
				backend.commit();
			});
		}
	}

	private class TriggerConfigPanel extends JPanel {

		private final EasyTrigger<?> trigger;

		@SuppressWarnings("unchecked")
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
//			TextFieldWithValidation<String> ttsField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(trigger::setTts), trigger.getTts());
//			TextFieldWithValidation<String> textField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(trigger::setText), trigger.getText());

			JPanel actionsPanel = new ActionsPanel<>(backend, "Actions", trigger, EasyTriggersTab.this::requestSave);
			JPanel conditionsPanel = new ConditionsPanel<>(backend, "Conditions", trigger, EasyTriggersTab.this::requestSave);

			c.weightx = 0;
//			c.gridx++;

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
			c.gridy++;
			c.gridx = 0;
			JComboBox<SequentialTriggerConcurrencyMode> concModeSelector = new JComboBox<>(SequentialTriggerConcurrencyMode.values());
			concModeSelector.setRenderer(new FriendlyNameListCellRenderer());
			concModeSelector.setSelectedItem(trigger.getConcurrency());
			concModeSelector.addItemListener(l -> {
				trigger.setConcurrency((SequentialTriggerConcurrencyMode) concModeSelector.getSelectedItem());
			});
			add(GuiUtil.labelFor("Concurrency", concModeSelector), c);
			c.gridx++;
			add(concModeSelector, c);

			c.gridx = 0;
			c.gridy++;
			c.gridwidth = GridBagConstraints.REMAINDER;
			if (trigger.getEventType().equals(EasyTriggersInitEvent.class)) {
				JButton button = new JButton("Re-Run");
				button.addActionListener(l -> backend.initSpecificTrigger((EasyTrigger<EasyTriggersInitEvent>) trigger));
				int fillBefore = c.fill;
				int anchorBefore = c.anchor;
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.NORTHWEST;
				add(button, c);
				c.gridy++;
				c.fill = fillBefore;
				c.anchor = anchorBefore;
			}
			add(conditionsPanel, c);
			c.gridy++;
			add(actionsPanel, c);
			c.gridy++;
			c.weighty = 1;
			add(Box.createGlue(), c);
		}

		private <X> Consumer<X> editTriggerThenSave(Consumer<X> modification) {
			return modification.andThen((unused) -> requestSave());
		}
	}

	private class QuestionAnswerer implements EasyTriggerCreationQuestions {

		private Component getParent() {
			return pico.getComponent(GuiMain.class).getMainFrame();
		}

		@Override
		public @Nullable String askOptionalString(String label) {
			String text = JOptionPane.showInputDialog(getParent(), label);
			if (text == null) {
				throw new TriggerCreationCancelledException("User cancelled trigger creation");
			}
			if (text.isEmpty()) {
				text = null;
			}
			return text;
		}

		@Override
		public boolean askYesNo(String label, String yesButton, String noButton) {
			int result = JOptionPane.showOptionDialog(getParent(),
					label,
					"Question",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE,
					null,
					new Object[]{yesButton, noButton, "Cancel"},
					JOptionPane.YES_OPTION
			);
			switch (result) {
				case JOptionPane.YES_OPTION -> {
					return true;
				}
				case JOptionPane.NO_OPTION -> {
					return false;
				}
			}
			throw new TriggerCreationCancelledException("User cancelled trigger creation");
		}
	}

	private void makeTriggerFromEvent(Event event) {
		EasyTrigger<?> newTrigger;
		bringToFront();
		try {
			newTrigger = backend.makeTriggerFromEvent(event, new QuestionAnswerer());
		}
		catch (TriggerCreationCancelledException e) {
			return;
		}
		if (newTrigger == null) {
			JOptionPane.showMessageDialog(pico.getComponent(GuiMain.class).getMainFrame(), "Unfortunately, this event type is not possible to automatically make a trigger for.");
		}
		else {
			addExisting(newTrigger);
		}
	}

	public void bringToFront() {
		tabReg.activateItem(this);
	}
}
