package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.function.Function;

public class CastOmen implements OmenInstance {

	private final AbilityCastStart acs;
	private final ActionOmenInfo info;

	public CastOmen(AbilityCastStart acs, ActionOmenInfo info) {
		this.acs = acs;
		this.info = info;
	}

	@Override
	public @NotNull ActionOmenInfo info() {
		return info;
	}

	@Override
	public XivAbility getAbility() {
		return acs.getAbility();
	}

	public AbilityCastStart getEvent() {
		return acs;
	}

	@Override
	public @Nullable Position omenPosition(Function<XivCombatant, Position> freshPosLookup) {
		DescribesCastLocation<AbilityCastStart> locInfo = acs.getLocationInfo();
		Position sourcePosSnapshot = source().getPos();
		if (locInfo != null) {
			Position pos = locInfo.getPos();
			if (pos != null) {
				return pos;
			}
			Double heading = locInfo.getHeadingOnly();
			if (heading != null) {
				return sourcePosSnapshot.facing(heading);
			}
		}
		if (info.type().locationType() == OmenLocationType.CASTER) {
			return sourcePosSnapshot;
		}
		XivCombatant tgt = acs.getTarget();
		if (tgt == null || tgt.isEnvironment() || tgt.getId() == source().getId()) {
			return sourcePosSnapshot;
		}
		else {
			Position tgtLocationNow = freshPosLookup.apply(tgt);
			if (info.type().locationType() == OmenLocationType.CASTER_FACE_TARGET) {
				return sourcePosSnapshot.facing(tgtLocationNow);
			}
			return tgtLocationNow;
		}
	}

	@Override
	public Instant happensAt() {
		return acs.getEffectiveHappenedAt().plus(acs.getInitialDuration());
	}

	@Override
	public @NotNull XivCombatant source() {
		return acs.getSource();
	}

	@Override
	public OmenEventType type() {
		return OmenEventType.PRE_CAST;
	}

}
