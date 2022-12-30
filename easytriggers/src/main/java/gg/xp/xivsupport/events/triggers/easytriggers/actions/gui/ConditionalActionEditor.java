package gg.xp.xivsupport.events.triggers.easytriggers.actions.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.ConditionalAction;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.ActionsPanel;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.ConditionsPanel;

import javax.swing.*;

public class ConditionalActionEditor extends JPanel {

	private final EasyTriggers backend;
	private final ConditionalAction action;

	public ConditionalActionEditor(EasyTriggers backend, ConditionalAction action) {
		this.action = action;
		this.backend = backend;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(Box.createHorizontalGlue());
		add(new ConditionsPanel<>(backend, "Conditions", action.conditionsController(), this::save));
		add(new ActionsPanel<>(backend, "True Actions", action.trueActionsController(), this::save));
		add(new ActionsPanel<>(backend, "False Actions", action.falseActionsController(), this::save));
	}

	private void save() {
		action.recalc();
		backend.commit();
	}

}
