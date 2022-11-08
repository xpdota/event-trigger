package gg.xp.xivsupport.events.triggers.easytriggers.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.AcceptsSaveCallback;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableActions;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.library.ChooserDialog;
import gg.xp.xivsupport.gui.tables.CustomColumn;
import gg.xp.xivsupport.gui.tables.TableWithFilterAndDetails;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ActionsPanel<X> extends TitleBorderFullsizePanel {
	private static final Logger log = LoggerFactory.getLogger(ActionsPanel.class);
	private final HasMutableActions<X> trigger;
	private final Runnable saveCallback;
	private final EasyTriggers backend;

	public ActionsPanel(EasyTriggers backend, String label, HasMutableActions<X> trigger, Runnable saveCallback) {
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
		newButton.addActionListener(l -> addNewAction());
		trigger.getActions().forEach(cond -> {
			add(new ActionPanel<>(cond));
		});
	}

	private void addNewAction() {
		TableWithFilterAndDetails<ActionDescription<?, ?>, Object> table = TableWithFilterAndDetails.builder(
						"Choose Action Type",
						() -> backend.getActionsApplicableTo(trigger))
				.addMainColumn(new CustomColumn<>("Action", c -> c.clazz().getSimpleName()))
				.addMainColumn(new CustomColumn<>("Description", ActionDescription::description))
				.setFixedData(true)
				.build();
		// TODO: owner
		ActionDescription<?, ?> desc = ChooserDialog.chooserReturnItem(SwingUtilities.getWindowAncestor(this), table);
		if (desc != null) {
			Action<?> newInst = desc.newInst();
			trigger.addAction((Action<? super X>) newInst);
			add(new ActionPanel<>(newInst));
			revalidate();
		}
	}

	private class ActionPanel<Y> extends JPanel {

		private final Action<Y> action;

		ActionPanel(gg.xp.xivsupport.events.triggers.easytriggers.model.Action<Y> action) {
			this.action = action;
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
			String fixedLabel = action.fixedLabel();
			if (fixedLabel != null) {
				JLabel labelLabel = new JLabel(fixedLabel);
				add(labelLabel, c);
				c.gridx++;
			}
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			deleteButton.addActionListener(l -> this.delete());
			Component component;
			Class<? extends Action> actionClass = action.getClass();
			ActionDescription<Action<Y>, Y> desc = backend.getActionDescription(actionClass);
			try {
				if (desc == null) {
					component = new JLabel("Error: cannot find component");
				}
				else {
					component = desc.guiprovider().apply(action, (EasyTrigger<? super Y>) trigger);
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
			trigger.removeAction((Action<? super X>) action);
			ActionsPanel.this.remove(this);
			ActionsPanel.this.revalidate();
		}
	}

}
