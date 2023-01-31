package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;

public class TetherEntityTypeFilter implements SimpleCondition<TetherEvent> {

	@Description("One Target Is")
	public EntityType firstType = EntityType.ANY;
	@Description("Other Target Is")
	public EntityType secondType = EntityType.ANY;

	@Override
	public String fixedLabel() {
		return "Tether Combatant Types";
	}

	@Override
	public String dynamicLabel() {
		return "One target is " + firstType.getFriendlyName() + ", other target is " + secondType.getFriendlyName();
	}

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean test(TetherEvent tether) {
		if (firstType.test(tether.getSource()) && secondType.test(tether.getTarget())) {
			return true;
		}
		else if (secondType.test(tether.getSource()) && firstType.test(tether.getTarget())) {
			return true;
		}
		else {
			return false;
		}
	}
}
