package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.xivsupport.events.actlines.events.HasEffects;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HasSeverity;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HealEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HitSeverityFilter implements SimpleCondition<HasEffects> {

	@Description("Damage")
	public boolean damage = true;
	@Description("Heal Target")
	public boolean healTarget = true;
	@Description("Secondary Self Heal (ED, Bloodbath, etc)")
	public boolean healSelf;
	@Description("Normal")
	public boolean normal = true;
	@Description("Crit")
	public boolean crit = true;
	@Description("Direct Hit")
	public boolean dhit = true;
	@Description("Direct Hit+Crit")
	public boolean dcrit = true;


	@Override
	public @Nullable String fixedLabel() {
		return "Hit Severity";
	}

	@Override
	public String dynamicLabel() {
		List<String> values = new ArrayList<>();
		if (normal) {
			values.add("Normal");
		}
		if (crit) {
			values.add("Crit");
		}
		if (dhit) {
			values.add("Direct Hit");
		}
		if (dcrit) {
			values.add("Direct Hit+Crit");
		}
		String formattedList = values.isEmpty() ? "NONE" : String.join(" or ", values);
		return "Hit severity is " + formattedList;
	}

	@Override
	public boolean test(HasEffects event) {
		return event.getEffects().stream().anyMatch(effect -> {
			if (effect instanceof HasSeverity hs) {
				if (hs instanceof DamageEffect) {
					if (!damage) {
						return false;
					}
				}
				else if (hs instanceof HealEffect he) {
					if (he.isOnTarget()) {
						if (!healTarget) {
							return false;
						}
					}
					else {
						if (!healSelf) {
							return false;
						}
					}
				}
				else {
					return false;
				}
				HitSeverity severity = hs.getSeverity();
				switch (severity) {
					case NORMAL -> {
						return normal;
					}
					case CRIT -> {
						return crit;
					}
					case DHIT -> {
						return dhit;
					}
					case CRIT_DHIT -> {
						return dcrit;
					}
				}
			}
			return false;
		});
	}

	@Override
	public Class<HasEffects> getEventType() {
		return HasEffects.class;
	}
}
