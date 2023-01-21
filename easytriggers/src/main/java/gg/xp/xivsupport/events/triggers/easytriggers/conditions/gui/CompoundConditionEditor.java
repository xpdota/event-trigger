package gg.xp.xivsupport.events.triggers.easytriggers.conditions.gui;

import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.OrFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.gui.ConditionsPanel;

import javax.swing.*;

public class CompoundConditionEditor<X> extends JPanel {

	private final EasyTriggers backend;
	private final OrFilter<X> condition;

	public CompoundConditionEditor(EasyTriggers backend, OrFilter<X> condition) {
		this.backend = backend;
		this.condition = condition;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(Box.createHorizontalGlue());
		add(new ConditionsPanel<>(backend, "Conditions", condition.conditionsController(), this::save));
	}

	private void save() {
		condition.recalc();
		backend.commit();
	}
}
