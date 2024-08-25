package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.DescribesCastLocation;
import gg.xp.xivsupport.events.actlines.events.SnapshotLocationDataEvent;
import gg.xp.xivsupport.models.Position;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Line264Parser extends AbstractACTLineParser<Line264Parser.Fields> {


	public Line264Parser(PicoContainer container) {
		super(container, 264, Fields.class);
	}

	enum Fields {
		entityId, abilityId, sequence, hasData, x, y, z, rotation
	}

	private final Map<Long, List<AbilityUsedEvent>> lastAbilityByEntity = new HashMap<>();

	@HandleEvents
	public void consumeAbilityUse(AbilityUsedEvent aue) {
		// Unlike casts where ther eis only one event, we want to associate this data to ALL hits
		lastAbilityByEntity.compute(aue.getSource().getId(), (id, value) -> {
			List<AbilityUsedEvent> out;
			if (value == null || aue.isFirstTarget()) {
				out = new ArrayList<>();
			}
			else {
				AbilityUsedEvent firstStored = value.get(0);
				if (firstStored.getAbility().getId() == aue.getAbility().getId()
				    && firstStored.getSequenceId() == aue.getSequenceId()) {
					out = value;
				}
				else {
					out = new ArrayList<>();
				}
			}
			out.add(aue);
			return out;
		});
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		long entity = fields.getHex(Fields.entityId);
		long ability = fields.getHex(Fields.abilityId);
		long sequenceId = fields.getHex(Fields.sequence);
		List<AbilityUsedEvent> allMatches = lastAbilityByEntity.remove(entity);
		if (allMatches == null) {
			return null;
		}
		AbilityUsedEvent firstMatch = allMatches.get(0);
		if (firstMatch.getSource().getId() == entity
		    && firstMatch.getAbility().getId() == ability
		    && firstMatch.getSequenceId() == sequenceId) {

			// Cast location/angle
			// First, check if the line indicates that such data is actually present.
			int hasData = fields.getInt(Fields.hasData);
			SnapshotLocationDataEvent slde;
			// 0 = no data, 1 = data, 256 = error
			if (hasData == 1) {
				double x = fields.getDouble(Fields.x);
				double y = fields.getDouble(Fields.y);
				double z = fields.getDouble(Fields.z);
				double h = fields.getDouble(Fields.rotation);
				if (x == 0.0 && y == 0.0 && z == 0.0) {
					AbilityCastStart precursor = firstMatch.getPrecursor();
					DescribesCastLocation<AbilityCastStart> castLocation;
					if (precursor != null && (castLocation = precursor.getLocationInfo()) != null && castLocation.getPos() != null) {
						slde = new SnapshotLocationDataEvent(firstMatch, castLocation);
					}
					else if (h == 0.0) {
						slde = null;
					}
					else {
						slde = new SnapshotLocationDataEvent(firstMatch, h);
					}
				}
				else {
					Position pos = new Position(x, y, z, h);
					slde = new SnapshotLocationDataEvent(firstMatch, pos);
				}
				if (slde != null) {
					// Associate back to all events
					allMatches.forEach(match -> match.setLocationInfo(slde));
					return slde;
				}
			}
		}
		return null;
	}

}
