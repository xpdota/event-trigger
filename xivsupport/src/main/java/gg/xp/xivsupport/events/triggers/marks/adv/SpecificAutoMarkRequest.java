package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPlayerHeadMarker;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class SpecificAutoMarkRequest extends BaseEvent implements HasPrimaryValue, HasTargetEntity, HasPlayerHeadMarker {

	@Serial
	private static final long serialVersionUID = -1398496564300407615L;
	private final XivPlayerCharacter playerToMark;
	private final MarkerSign marker;

	public SpecificAutoMarkRequest(XivPlayerCharacter playerToMark, MarkerSign marker) {
		this.playerToMark = playerToMark;
		this.marker = marker;
	}

	public XivPlayerCharacter getPlayerToMark() {
		return playerToMark;
	}

	@Override
	public XivCombatant getTarget() {
		return playerToMark;
	}

	@Override
	public MarkerSign getMarker() {
		return marker;
	}

	@Override
	public @Nullable String extraDescription() {
		return getPrimaryValue();
	}

	@Override
	public String getPrimaryValue() {
		return String.format("'%s' on %s", marker.getCommand(), playerToMark.getName());
	}
}
