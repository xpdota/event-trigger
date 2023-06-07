package gg.xp.xivsupport.gui.map.omen;

import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.function.Function;

public class InstantOmen implements OmenInstance {

	private final AbilityUsedEvent aue;
	private final ActionOmenInfo info;

	public InstantOmen(AbilityUsedEvent aue, ActionOmenInfo info) {
		this.aue = aue;
		this.info = info;
	}

	@Override
	public ActionOmenInfo info() {
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
		return aue.getSource().getPos();
	}

	@Override
	public OmenEventType type() {
		return OmenEventType.INSTANT;
	}
}
