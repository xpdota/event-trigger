package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class UsedOmen implements OmenInfo {

	private final AbilityUsedEvent aue;
	private final AbilityCastStart preCast;

	public UsedOmen(AbilityUsedEvent aue, @Nullable AbilityCastStart preCast) {
		this.aue = aue;
		this.preCast = preCast;
	}

	@Override
	public XivAbility getAbility() {
		return aue.getAbility();
	}

	@Override
	public Instant happensAt() {
		return aue.getEffectiveHappenedAt();
	}

	@Override
	public AbilityUsedEvent event() {
		return aue;
	}

	@Override
	public @Nullable Position position() {
		if (preCast != null) {
			XivCombatant tgt = preCast.getTarget();
			if (!tgt.isEnvironment()) {
				return tgt.getPos();
			}
		}
		return aue.getSource().getPos();
	}

	@Override
	public @Nullable XivCombatant target() {
		if (preCast != null) {
			XivCombatant tgt = preCast.getTarget();
			if (tgt.equals(preCast.getSource())) {
				return null;
			}
			return tgt;
		}
		else {
			return null;
		}
	}

	@Override
	public boolean useLivePosition() {
		return false;
	}

	@Override
	public OmenEventType type() {
		return preCast != null ? OmenEventType.CAST_FINISHED : OmenEventType.INSTANT;
	}
}
