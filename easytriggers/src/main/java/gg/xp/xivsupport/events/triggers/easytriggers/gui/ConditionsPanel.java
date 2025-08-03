package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.AcceptsSaveCallback;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableConditions;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ConditionsPanel<X> extends TitleBorderFullsizePanel {
	private static final Logger log = LoggerFactory.getLogger(ConditionsPanel.class);
	private final HasMutableConditions<X> trigger;
	private final Runnable saveCallback;
	private final EasyTriggers backend;

	public ConditionsPanel(EasyTriggers backend, String label, HasMutableConditions<X> trigger, Runnable saveCallback) {
		super(label);
		this.backend = backend;
		this.trigger = trigger;
		this.saveCallback = saveCallback;
		setPreferredSize(null);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		var buttonsArea = new JPanel();
		buttonsArea.setLayout(new GridBagLayout());
		GridBagConstraints c = GuiUtil.defaultGbc();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.insets = new Insets(0, 2, 2, 3);
		buttonsArea.setAlignmentX(LEFT_ALIGNMENT);

		JButton newButton = EtGuiUtils.smallButton("New", this::addNewCondition);
		buttonsArea.add(newButton, c);
		JButton pasteButton = EtGuiUtils.smallButton("Paste", this::tryPasteCondition);
		c.gridx++;
		buttonsArea.add(pasteButton, c);
		c.gridx++;
		c.weightx = 1;
		buttonsArea.add(Box.createHorizontalGlue(), c);

		add(buttonsArea);

		trigger.getConditions().forEach(cond -> {
			add(new ConditionPanel<>(cond));
		});
	}

	private void addNewCondition() {
		TableWithFilterAndDetails<ConditionDescription<?, ?>, Object> table = TableWithFilterAndDetails.builder(
						"Choose Condition Type",
						() -> backend.getConditionsApplicableTo(trigger))
				.addMainColumn(new CustomColumn<>("Condition", c -> c.clazz().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Description", ConditionDescription::description))
				.setFixedData(true)
				.build();
		// TODO: owner
		ConditionDescription<?, ?> desc = ChooserDialog.chooserReturnItem(SwingUtilities.getWindowAncestor(this), table);
		if (desc != null) {
			Condition<?> newInst = desc.newInst();
			trigger.addCondition((Condition<? super X>) newInst);
			add(new ConditionPanel<>(newInst));
			revalidate();
			saveCallback.run();
		}
	}

	private void showError(String msg) {
		JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void tryPasteCondition() {
		String text = GuiUtil.getTextFromClipboard();
		if (text == null) {
			showError("No condition on clipboard");
			return;
		}
		Condition<?> condition;
		try {
			condition = backend.importCondition(text);
		}
		catch (Exception e) {
			showError("No condition on clipboard");
			return;
		}
		if (condition == null) {
			showError("No condition on clipboard");
			return;
		}
		Class<?> triggerType = trigger.getEventType();
		Class<?> conditionType = condition.getEventType();
		if (!conditionType.isAssignableFrom(triggerType)) {
			if (trigger instanceof TriggerFolder) {
				showError("Condition type " + condition.getClass().getSimpleName() + " is not applicable to a trigger folder");
			}
			else {
				showError("Condition type " + condition.getClass().getSimpleName() + " is not applicable to a trigger for " + triggerType.getSimpleName());
			}
			return;
		}
		trigger.addCondition((Condition<? super X>) condition);
		add(new ConditionPanel<>(condition));
		revalidate();
		saveCallback.run();
	}

	private class ConditionPanel<Y> extends JPanel {

		private final Condition<Y> condition;

		ConditionPanel(Condition<Y> condition) {
			this.condition = condition;
			setAlignmentX(LEFT_ALIGNMENT);
			setLayout(new GridBagLayout());
			GridBagConstraints c = GuiUtil.defaultGbc();
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;

			c.insets = new Insets(2, 2, 2, 3);
			JButton deleteButton = EtGuiUtils.smallButton("Delete", this::delete);
			add(deleteButton, c);
			c.gridx++;
			JButton cutButton = EtGuiUtils.smallButton("Cut", this::cut);
			add(cutButton, c);
			c.gridx++;
			JButton copyButton = EtGuiUtils.smallButton("Copy", this::copy);
			add(copyButton, c);
			c.gridx++;
			JLabel labelLabel = new JLabel(condition.fixedLabel());
			add(labelLabel, c);
			c.gridx++;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			Component component;
			Class<? extends Condition> condClass = condition.getClass();
			ConditionDescription<Condition<Y>, Y> desc = backend.getConditionDescription(condClass);
			try {
				if (desc == null) {
					component = new JLabel("Error: cannot find component");
				}
				else {
					component = desc.guiprovider().apply(condition, trigger);
					if (component == null) {
						component = new JLabel("Error: null component");
					}
				}
			}
			catch (Throwable t) {
				log.error("Error making condition component", t);
				component = new JLabel("Error making component");
			}
			add(component, c);
			if (component instanceof AcceptsSaveCallback asc) {
				asc.setSaveCallback(saveCallback);
			}
		}

		private void copy() {
			GuiUtil.copyTextToClipboard(backend.exportCondition(condition));
		}

		private void delete() {
			trigger.removeCondition((Condition<? super X>) condition);
			ConditionsPanel.this.remove(this);
			ConditionsPanel.this.revalidate();
		}

		private void cut() {
			copy();
			delete();
		}

	}

}
