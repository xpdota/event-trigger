package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.function.Function;

public class CastFinishedOmen implements OmenInstance {

	private final AbilityUsedEvent aue;
	private final @NotNull AbilityCastStart preCast;
	private final ActionOmenInfo info;

	public CastFinishedOmen(AbilityUsedEvent aue, @NotNull AbilityCastStart preCast, ActionOmenInfo info) {
		this.aue = aue;
		this.preCast = preCast;
		this.info = info;
	}

	@Override
	public @NotNull ActionOmenInfo info() {
		return info;
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
	public @NotNull XivCombatant source() {
		return aue.getSource();
	}

	@Override
	public @Nullable Position omenPosition(Function<XivCombatant, Position> freshPosLookup) {
		// Logic: use a priority:
		// 1. Snapshot location
		// 2. Cast location
		// 3. Snapshot angle (+ animation target)
		// 4. Cast angle
		// 5. Caster position at time of snapshot
		DescribesCastLocation<?> snapLoc = aue.getLocationInfo();
		DescribesCastLocation<?> castLoc = preCast.getLocationInfo();
		// 1. Snapshot location
		if (snapLoc != null && snapLoc.getPos() != null) {
			return snapLoc.getPos();
		}
		// 2. Cast location
		if (castLoc != null && castLoc.getPos() != null) {
			return castLoc.getPos();
		}
		Position sourcePosSnapshot = source().getPos();
		DescribesCastLocation<?> bestLoc = snapLoc != null ? snapLoc : castLoc;
		if (bestLoc != null) {
			Position pos = bestLoc.getPos();
			if (pos != null) {
				return pos;
			}
			// 3. Snapshot angle (+ animation target)
			// 4. Cast angle
			Double heading = bestLoc.getHeadingOnly();
			Position basis = bestLoc.getAnimationTarget().getPos();
			if (heading != null && basis != null) {
				return basis.facing(heading);
			}
		}
		// 5. Caster position at time of snapshot
		if (info.type().locationType() == OmenLocationType.CASTER) {
			return sourcePosSnapshot;
		}
		XivCombatant tgt = preCast.getTarget();
		if (tgt == null || tgt.isEnvironment() || tgt.getId() == source().getId()) {
			return sourcePosSnapshot;
		}
		// Never track target - it has already snapshotted
		if (info.type().locationType() == OmenLocationType.CASTER_FACE_TARGET) {
			if (sourcePosSnapshot == null) {
				return null;
			}
			Position faceTowards = aue.getTarget().getPos();
			if (faceTowards != null) {
				return sourcePosSnapshot.facing(faceTowards);
			}
			return null;
		}
		else {
			return aue.getTarget().getPos();
		}
	}

	@Override
	public OmenEventType type() {
		return OmenEventType.CAST_FINISHED;
	}
}
