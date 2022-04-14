package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class TetherEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {

	@Serial
	private static final long serialVersionUID = 7043671273943254143L;
	private final XivCombatant source;
	private final XivCombatant target;
	private final long id;

	public TetherEvent(XivCombatant source, XivCombatant target, long id) {
		this.source = source;
		this.target = target;
		this.id = id;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public long getId() {
		return id;
	}

	public boolean eitherTargetMatches(XivCombatant cbt) {
		return source.equals(cbt) || target.equals(cbt);
	}

	public List<XivCombatant> getTargets() {
		return List.of(source, target);
	}

	public @Nullable XivCombatant getTargetMatching(Predicate<XivCombatant> targetCondition) {
		if (targetCondition.test(source)) {
			return source;
		}
		else if (targetCondition.test(target)) {
			return target;
		}
		else {
			return null;
		}
	}

	public static Set<XivCombatant> getUnitsTetheredTo(XivCombatant combatant, Collection<TetherEvent> tethers) {
		Set<XivCombatant> tetheredCombatants = new HashSet<>();
		for (TetherEvent tether : tethers) {
			if (tether.getSource().equals(combatant)) {
				tetheredCombatants.add(tether.getTarget());
			}
			else if (tether.getTarget().equals(combatant)) {
				tetheredCombatants.add(tether.getSource());
			}
		}
		tetheredCombatants.remove(combatant);
		return tetheredCombatants;
	}
}
