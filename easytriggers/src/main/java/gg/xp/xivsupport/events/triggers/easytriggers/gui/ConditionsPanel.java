package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.AcceptsSaveCallback;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableConditions;
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
//		setLayout(new GridBagLayout());
//		GridBagConstraints c = GuiUtil.defaultGbc();
//		c.fill = GridBagConstraints.NONE;
//		c.anchor = GridBagConstraints.WEST;
//		JButton newButton = new JButton("New");
//		add(newButton, c);
//		c.gridy++;
//		newButton.addActionListener(l -> addNewCondition());
//		trigger.getConditions().forEach(cond -> {
//			add(new ConditionPanel<>(cond), c);
//			c.gridy++;
//		});
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JButton newButton = new JButton("New");
		add(newButton);
		newButton.addActionListener(l -> addNewCondition());
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

	private class ConditionPanel<Y> extends JPanel {

		private final Condition<Y> condition;

		ConditionPanel(Condition<Y> condition) {
			this.condition = condition;
			setAlignmentX(Component.LEFT_ALIGNMENT);
			setBorder(null);
			setLayout(new GridBagLayout());
			GridBagConstraints c = GuiUtil.defaultGbc();
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			JButton deleteButton = new JButton("Delete");
//			JPanel buttonHolder = new JPanel();
//			buttonHolder.add(deleteButton);
//			add(buttonHolder, c);
			add(deleteButton, c);
			c.gridx++;
			JLabel labelLabel = new JLabel(condition.fixedLabel());
			add(labelLabel, c);
			c.gridx++;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			deleteButton.addActionListener(l -> this.delete());
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
//			c.weightx = 1;
//			add(Box.createHorizontalGlue(), c);
		}

		private void delete() {
			trigger.removeCondition((Condition<? super X>) condition);
			ConditionsPanel.this.remove(this);
			ConditionsPanel.this.revalidate();
		}
	}

}
