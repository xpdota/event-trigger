package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class PlayerMarkerPlacedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasPrimaryValue, HasPlayerHeadMarker {

	@Serial
	private static final long serialVersionUID = 4273043478384517415L;
	private final MarkerSign marker;
	private final XivCombatant source;
	private final XivCombatant target;

	public PlayerMarkerPlacedEvent(MarkerSign marker, XivCombatant source, XivCombatant target) {
		this.marker = marker;
		this.source = source;
		this.target = target;
	}

	public MarkerSign getMarker() {
		return marker;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public @Nullable String extraDescription() {
		return getPrimaryValue();
	}

	@Override
	public String getPrimaryValue() {
		return "Placed " + marker.getFriendlyName();
	}
}
