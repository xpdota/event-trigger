package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.CustomTableModel;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Function;

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
		outer = new TitleBorderFullsizePanel("Easy Triggers");
		outer.setLayout(new GridBagLayout());
		GridBagConstraints c = GuiUtil.defaultGbc();
		c.weighty = 1;

		model = CustomTableModel.builder(backend::getTriggers)
				.addColumn(new CustomColumn<>("Name", EasyTrigger::getName))
				.addColumn(new CustomColumn<>("Event Type", t -> t.getEventType().getSimpleName()))
				.addColumn(new CustomColumn<>("Conditions", t -> t.getConditions().size()))
				.addColumn(new CustomColumn<>("TTS", EasyTrigger::getTts))
				.addColumn(new CustomColumn<>("Text", EasyTrigger::getText))
				.build();
		JTable triggerChooserTable = model.makeTable();
		triggerChooserTable.getSelectionModel().addListSelectionListener(l -> {
			refreshSelection();
		});
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
		outer.add(split, c);

		return outer;
	}

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
		TableWithFilterAndDetails<EventDescription, Object> table = TableWithFilterAndDetails.builder("Choose Event Type", EasyTriggers::getEventDescriptions)
				.addMainColumn(new CustomColumn<>("Event", d -> d.type().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Description", EventDescription::description))
				.build();
		// TODO: owner
		EventDescription eventDescription = ChooserDialog.chooserReturnItem(SwingUtilities.getWindowAncestor(outer), table);
		EasyTrigger<Event> newTrigger = new EasyTrigger<>();
		newTrigger.setEventType((Class<Event>) eventDescription.type());
		backend.addTrigger(newTrigger);
		refresh();
		setSelection(newTrigger);
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

			TextFieldWithValidation<String> nameField = new TextFieldWithValidation<>(str -> {
				if (str.isBlank()) {
					throw new IllegalArgumentException("Cannot be blank");
				}
				return str;
			}, editTriggerThenSave(trigger::setName), trigger.getName());
			TextFieldWithValidation<String> ttsField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(trigger::setTts), trigger.getTts());
			TextFieldWithValidation<String> textField = new TextFieldWithValidation<>(Function.identity(), editTriggerThenSave(trigger::setText), trigger.getText());

			TitleBorderFullsizePanel conditionsPanel = new TitleBorderFullsizePanel("Conditions");
			{
				conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.PAGE_AXIS));
				conditionsPanel.add(new JButton("New Condition"));
			}

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
