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

/**
 * Event representing a tether. Rather than relying on tether events always ordering the two targets in a particular
 * way, consider using order-agnostic methods, such as {@link #eitherTargetMatches}, {@link #getTargetMatching},
 * and {@link #getTargets()}. In addition, for computing multiple tethers, there is a {@link #getUnitsTetheredTo} method
 * which you may find convenient.
 */
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

	public boolean tetherIdMatches(long... expected) {
		for (long e : expected) {
			if (e == id) {
				return true;
			}
		}
		return false;
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

	public boolean eitherTargetMatches(Predicate<XivCombatant> targetCondition) {
		return targetCondition.test(source) || targetCondition.test(target);
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

	@Override
	public String toString() {
		return "TetherEvent{" +
				"source=" + source +
				", target=" + target +
				", id=" + id +
				'}';
	}
}
