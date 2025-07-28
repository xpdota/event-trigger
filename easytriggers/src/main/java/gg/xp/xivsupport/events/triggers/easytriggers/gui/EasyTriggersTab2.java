package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.ActLegacyTriggerImport;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.creators.EasyTriggerCreationQuestions;
import gg.xp.xivsupport.events.triggers.easytriggers.events.EasyTriggersInitEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.tree.EasyTriggersTreeRenderer;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.tree.TriggerTree;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.tree.TriggerTreeEditor;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.tree.TriggerTreeModel;
import gg.xp.xivsupport.events.triggers.easytriggers.model.BaseTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
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
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import static gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext.SOURCE_EASY_TRIGGER_KEY;

@ScanMe
public class EasyTriggersTab2 implements PluginTab {

	private static final Logger log = LoggerFactory.getLogger(EasyTriggersTab2.class);

	private final EasyTriggers backend;
	private final GlobalUiRegistry tabReg;
	private final PicoContainer pico;
	private final ExecutorService exs = Executors.newSingleThreadExecutor();
	private JPanel detailsInner;
	private TriggerTreeModel model;
	private TitleBorderFullsizePanel outer;
	private RefreshLoop<EasyTriggersTab2> autoSave;
	private TriggerTree tree;

	public EasyTriggersTab2(EasyTriggers backend, RightClickOptionRepo rightClicks, GlobalUiRegistry tabReg, PicoContainer pico) {
		this.backend = backend;
		this.tabReg = tabReg;
		this.pico = pico;
		rightClicks.addOption(CustomRightClickOption.forRow(
				"Make Easy Trigger",
				Event.class,
				this::makeTriggerFromEvent));
		rightClicks.addOption(CustomRightClickOption.forRow(
				"Go To Easy Trigger",
				Event.class,
				this::trySelectTriggerFromCallout,
				EasyTriggersTab2::canSelectTriggerFromCallout
		));
		// TODO: good candidate for sub-menus
//				.addRightClickOption(CustomRightClickOption.forRowWithConverter("Make Easy Trigger", Event.class, Function.identity(), e -> {
//					container.getComponent(EasyTrig)
//					GuiUtil.copyTextToClipboard(line.getFields().toString());
//				}))
	}

	private static final Selections NO_SELECTIONS = new Selections(Collections.emptyList());
	private @NotNull Selections currentSelections = NO_SELECTIONS;

	private record Selection(BaseTrigger<?> trigger, TreePath path) {
		@Nullable TriggerFolder getParent() {
			if (path.getParentPath() == null) {
				return null;
			}
			if (path.getParentPath().getLastPathComponent() instanceof TriggerFolder tf) {
				return tf;
			}
			return null;
		}
	}

	private record Selections(List<Selection> selections) {
		boolean hasSelection() {
			return !this.selections.isEmpty();
		}

		boolean hasSingleSelection() {
			return this.selections.size() == 1;
		}

		@Nullable
		Selection getSingleSelection() {
			if (this.selections.size() != 1) {
				return null;
			}
			return this.selections.get(0);
		}

		List<? extends BaseTrigger<?>> getSelectedTriggers() {
			return selections.stream().map(Selection::trigger).toList();
		}

		List<TreePath> getSelectedPaths() {
			return selections.stream().map(Selection::path).toList();
		}
	}

	@Override
	public String getTabName() {
		return "Easy Triggers 2.0";
	}

	@Override
	public Component getTabContents() {
		GridBagConstraints c = GuiUtil.defaultGbc();
		c.weighty = 1;
		model = new TriggerTreeModel(backend);

		tree = new TriggerTree(model, backend);

		outer = new TitleBorderFullsizePanel("Easy Triggers") {
			@Override
			public void setVisible(boolean visible) {
				if (visible) {
					tree.refresh();
				}
				else {
					backend.commit();
				}
				super.setVisible(visible);
			}
		};

		outer.setLayout(new GridBagLayout());
		tree.getSelectionModel().addTreeSelectionListener(l -> {
			refreshSelection();
		});
//		triggerChooserTree.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		{
			// TODO: most of these could also be right click options
			JPanel controlsPanel = new JPanel(new FlowLayout());
			{
				JButton newTriggerButton = new JButton("New Trigger");
				controlsPanel.add(newTriggerButton);
				newTriggerButton.addActionListener(l -> addNew());
			}
			{
				JButton newTriggerButton = new JButton("New Folder");
				controlsPanel.add(newTriggerButton);
				newTriggerButton.addActionListener(l -> addNewFolder());
			}
			{
				JButton rfBtn = new JButton("Refresh");
				controlsPanel.add(rfBtn);
				rfBtn.addActionListener(l -> refresh());
			}
			{
				JButton deleteTriggerButton = new JButton("Delete Selected") {
					@Override
					public boolean isEnabled() {
						return currentSelections.hasSelection();
					}
				};
				controlsPanel.add(deleteTriggerButton);
				deleteTriggerButton.addActionListener(l -> delete());
			}
			{
				JButton cloneTriggerButton = new JButton("Clone Trigger") {
					@Override
					public boolean isEnabled() {
						// TODO: should this support multi-selection?
						return currentSelections.hasSingleSelection();
					}
				};
				controlsPanel.add(cloneTriggerButton);
				cloneTriggerButton.addActionListener(l -> cloneCurrent());
			}
			{
				JButton exportTriggerButton = new JButton("Export Selected") {
					@Override
					public boolean isEnabled() {
						return currentSelections.hasSelection();
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

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tree), bottomPanel);
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
		// TODO: include parents? Maybe try to match by uuid? Maybe just have it be an import property?
		List<? extends BaseTrigger<?>> triggers = currentSelections.getSelectedTriggers();
		if (!triggers.isEmpty()) {
			GuiUtil.copyTextToClipboard(backend.exportToString(triggers));
			JOptionPane.showMessageDialog(outer, "Copied to clipboard");
		}
	}

	// Basically, since I didn't think about a good way to auto-save, the auto-save loop saves continuously when the tab
	// is showing, immediately when the tab is no longer visible, BUT that visibility check only counts for the direct
	// parent tab, not the main tab bar. Thus, we need another check to do one more save when we are no longer showing.
	private boolean saveAnyway;

	private void refreshSelection() {
		TreePath[] selections = this.tree.getSelectionModel().getSelectionPaths();
		Selections sels = new Selections(Arrays.stream(selections)
				// Don't include the useless root node as a selection
				.filter(path -> path != null && path.getLastPathComponent() instanceof BaseTrigger<?>)
				.map(path -> new Selection((BaseTrigger<?>) path.getLastPathComponent(), path))
				.toList());
		this.currentSelections = sels;
		SwingUtilities.invokeLater(() -> {
			detailsInner.removeAll();
			Selection sel = sels.getSingleSelection();
			if (sel != null) {
				detailsInner.add(makeConfigPanel(sel.trigger));
			}
			detailsInner.revalidate();
			detailsInner.repaint();
			outer.repaint();
		});
	}

	private void refresh() {
		// TODO: consider large model mode
		tree.refresh();
	}

	private void cloneCurrent() {
		if (true) {
			JOptionPane.showMessageDialog(outer, "Not Implemented");
			return;
		}
//		BaseTrigger<?> selectedValue = this.selection;
//		if (selectedValue == null) {
//			return;
//		}
//		// TODO: needs to import into correct parent
//		List<EasyTrigger<?>> newTriggers = backend.importFromString(backend.exportToString(Collections.singletonList(selectedValue)));
//		EasyTrigger<?> newTrigger = newTriggers.get(0);
//		backend.addTrigger(newTrigger);
//		refresh();
//		tree.setSelectionPaths();
//		model.setSelectedValue(newTrigger);
//		model.scrollToSelectedValue();
//		refreshSelection();
	}

	/**
	 * Try to find a TreePath for a particular trigger via brute force
	 *
	 * @param trigger the trigger to search for
	 * @return the TreePath if found, null if not found
	 */
	private @Nullable TreePath pathForTrigger(BaseTrigger<?> trigger) {
		return pathForTrigger(new TreePath(backend), null, trigger);
	}

	/**
	 * Recursive implementation for {@link #pathForTrigger(BaseTrigger)}. It will search the children of `curNode`.
	 * If a child matches, it returns the path. If a child is another folder, then it will recurse into it and repeat.
	 * If nothing matches, returns null.
	 *
	 * @param treePath The current search path
	 * @param curNode  The current search node
	 * @param trigger  The item we are looking for
	 * @return the TreePath if found, null if not found
	 */
	private @Nullable TreePath pathForTrigger(TreePath treePath, @Nullable TriggerFolder curNode, BaseTrigger<?> trigger) {
		List<BaseTrigger<?>> children;
		if (curNode != null) {
			children = curNode.getChildTriggers();
		}
		else {
			children = backend.getChildTriggers();
		}
		for (BaseTrigger<?> child : children) {
			if (child instanceof EasyTrigger<?> leaf) {
				if (leaf == trigger) {
					return treePath.pathByAddingChild(child);
				}
			}
			else if (child instanceof TriggerFolder folder) {
				TreePath newPath = pathForTrigger(treePath.pathByAddingChild(child), folder, trigger);
				if (newPath != null) {
					return newPath;
				}
			}
		}
		return null;
	}

	private void addExisting(EasyTrigger<?> trigger) {
		// TODO: needs to look at current parent selection and add under that
		backend.addTrigger(trigger);
		selectTrigger(trigger);
	}

	public void selectTrigger(BaseTrigger<?> trigger) {
		SwingUtilities.invokeLater(() -> {
			if (tree != null) {
				refresh();
				SwingUtilities.invokeLater(() -> {
					TreePath treePath = pathForTrigger(trigger);
					if (treePath != null) {
						tree.setSelectionPath(treePath);
						tree.scrollPathToVisible(treePath);
					}
				});
			}
		});
	}

	private @Nullable TriggerFolder getFolderForAdds() {
		Selections sel = currentSelections;
		Selection single = sel.getSingleSelection();
		if (single != null) {
			if (single.trigger instanceof TriggerFolder) {
				return (TriggerFolder) single.trigger;
			}
			else {
				return single.getParent();
			}
		}
		else {
			return null;
		}
	}

	private void addNewFolder() {
		TriggerFolder folder = new TriggerFolder();
		folder.setName("New Folder");
		backend.addTrigger2(getFolderForAdds(), folder);
		refresh();
		selectTrigger(folder);
	}

	private void addNew() {
		TableWithFilterAndDetails<EventDescription<?>, Object> table = TableWithFilterAndDetails.builder("Choose Event Type", backend::getEventDescriptions)
				.addMainColumn(new CustomColumn<>("Event", d -> d.type().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Description", EventDescription::description))
				.setFixedData(true)
				.build();
		// TODO: owner
		EventDescription<?> eventDescription = ChooserDialog.chooserReturnItem(SwingUtilities.getWindowAncestor(outer), table);
		if (eventDescription != null) {
			EasyTrigger<?> newTrigger = eventDescription.newDefaultInst();
			backend.addTrigger2(getFolderForAdds(), newTrigger);
			refresh();
			SwingUtilities.invokeLater(() -> {
				// TODO
//				model.setSelectedValue(newTrigger);
//				refreshSelection();
//				model.scrollToSelectedValue();
			});
		}
	}

	private void addImports(List<EasyTrigger<?>> toAdd) {
		// TODO: consider path, or add that as an argument
		if (toAdd.isEmpty()) {
			return;
		}
		toAdd.forEach(backend::addTrigger);
		refresh();
	}

	private void delete() {
		currentSelections.selections.forEach(sel -> {
			backend.removeTrigger2(sel.getParent(), sel.trigger);
		});
		refresh();
		tree.clearSelection();
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

	private JPanel makeConfigPanel(BaseTrigger<?> item) {
		if (item instanceof EasyTrigger<?> et) {
			return new TriggerConfigPanel(et);
		}
		if (item instanceof TriggerFolder folder) {
			return new FolderConfigPanel(folder);
		}
		throw new IllegalArgumentException("Unknown trigger type: " + item.getClass().getName());
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

			JPanel actionsPanel = new ActionsPanel<>(backend, "Actions", trigger, EasyTriggersTab2.this::requestSave);
			JPanel conditionsPanel = new ConditionsPanel<>(backend, "Conditions", trigger, EasyTriggersTab2.this::requestSave);

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
				// TODO: EasyTriggersInitEvent should ignore parent conditions
				// TODO: add special icon for startup trigger
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

	private class FolderConfigPanel extends JPanel {

		FolderConfigPanel(TriggerFolder trigger) {
			setLayout(new GridBagLayout());
			GridBagConstraints c = GuiUtil.defaultGbc();
			c.insets = new Insets(1, 2, 1, 2);
			c.anchor = GridBagConstraints.NORTH;


			JTextField eventTypeField = new JTextField(trigger.getEventType().getSimpleName());
			eventTypeField.setEditable(false);
			TextFieldWithValidation<String> nameField = new TextFieldWithValidation<>(str -> {
				if (str.isBlank()) {
					throw new IllegalArgumentException("Cannot be blank");
				}
				return str;
			}, editTriggerThenSave(trigger::setName), trigger.getName());

			JPanel conditionsPanel = new ConditionsPanel<>(backend, "Conditions for All Triggers in this Folder", trigger, EasyTriggersTab2.this::requestSave);

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

	private static boolean canSelectTriggerFromCallout(Event event) {
		return getEasyTriggerFromCallout(event) != null;
	}

	private void trySelectTriggerFromCallout(Event event) {
		EasyTrigger<?> et = getEasyTriggerFromCallout(event);
		if (et != null) {
			bringToFront();
			this.selectTrigger(et);
		}
	}

	private static @Nullable EasyTrigger<?> getEasyTriggerFromCallout(Event event) {
		RawModifiedCallout<?> rawModified = event.getThisOrParentOfType(RawModifiedCallout.class);
		if (rawModified == null) {
			return null;
		}
		Object source = rawModified.getArguments().get(SOURCE_EASY_TRIGGER_KEY);
		if (source instanceof EasyTrigger<?> et) {
			return et;
		}
		else {
			return null;
		}

	}
}
