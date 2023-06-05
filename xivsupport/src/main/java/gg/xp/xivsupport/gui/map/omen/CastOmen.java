package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class CastOmen implements OmenInfo {

	private final AbilityCastStart acs;

	public CastOmen(AbilityCastStart acs) {
		this.acs = acs;
	}

	@Override
	public XivAbility getAbility() {
		return acs.getAbility();
	}

	@Override
	public Instant happensAt() {
		return acs.getEffectiveHappenedAt().plus(acs.getInitialDuration());
	}

	@Override
	public AbilityCastStart event() {
		return acs;
	}

	@Override
	public @Nullable Position position() {
		return null;
	}

	@Override
	public boolean useLivePosition() {
		return true;
	}

	@Override
	public @Nullable XivCombatant target() {
		XivCombatant tgt = acs.getTarget();
		if (tgt.isEnvironment()) {
			return null;
		}
		return tgt;
	}

	@Override
	public OmenEventType type() {
		return OmenEventType.INSTANT;
	}

}
