package gg.xp.xivsupport.events.state.playermarkers;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.PlayerMarkerPlacedEvent;
import gg.xp.xivsupport.events.actlines.events.PlayerMarkerRemovedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class PlayerMarkerRepository {

	private static final Logger log = LoggerFactory.getLogger(PlayerMarkerRepository.class);
	// With EnumMap, there's not much in the way of thread safety requirements
	private final EnumMap<MarkerSign, XivCombatant> markers = new EnumMap<>(MarkerSign.class);

	@HandleEvents(order = -1_000_000)
	public void markerPlaced(EventContext context, PlayerMarkerPlacedEvent event) {
		XivCombatant target = event.getTarget();
		log.info("Marker {} PLACED ON {} (by {})", event.getMarker().getFriendlyName(), target.getName(), event.getSource().getName());
		markers.put(event.getMarker(), target);
		// This situation *should* be a non-issue, since there's a remove line for when a marker is implicitly removed
		// by putting another marker on the same unit, but check just in case.
		MarkerSign otherSign = signOnCombatant(target);
		if (otherSign != null) {
			markers.remove(otherSign, target);
		}
	}

	@HandleEvents(order = -1_000_000)
	public void markerRemoved(EventContext context, PlayerMarkerRemovedEvent event) {
		log.info("Marker {} REMOVED FROM {} (by {})", event.getMarker().getFriendlyName(), event.getTarget().getName(), event.getSource().getName());
		markers.remove(event.getMarker(), event.getTarget());
	}

	@HandleEvents
	public void zoneChanged(EventContext context, ZoneChangeEvent zce) {
		markers.clear();
	}

	public Map<MarkerSign, XivCombatant> getMarkers() {
		return new EnumMap<>(markers);
	}

	@Contract("null -> fail")
	public @Nullable XivCombatant combatantWithSign(MarkerSign sign) {
		return markers.get(sign);
	}

	@Contract("null -> null")
	public @Nullable MarkerSign signOnCombatant(XivCombatant cbt) {
		if (cbt == null) {
			return null;
		}
		return markers.entrySet().stream()
				.filter(e -> cbt.equals(e.getValue()))
				.findFirst()
				.map(Map.Entry::getKey)
				.orElse(null);
	}


}
