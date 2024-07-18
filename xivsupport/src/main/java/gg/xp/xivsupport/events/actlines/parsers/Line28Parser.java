package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.MarkerPlacedEvent;
import gg.xp.xivsupport.events.actlines.events.MarkerRemovedEvent;
import gg.xp.xivsupport.events.state.floormarkers.FloorMarker;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line28Parser extends AbstractACTLineParser<Line28Parser.Fields> {

	public Line28Parser(PicoContainer container) {
		super(container, 28, Fields.class);
	}

	enum Fields {
		addRemove, markerId, placerId, placerName, x, y, z
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		FloorMarker marker = FloorMarker.values()[(int) fields.getLong(Fields.markerId)];
		Position pos = new Position(fields.getDouble(Fields.x), fields.getDouble(Fields.y), fields.getDouble(Fields.z), 0);
		XivCombatant placer = fields.getEntity(Fields.placerId, Fields.placerName);
		String addRemove = fields.getString(Fields.addRemove);
		if ("Add".equals(addRemove)) {
			return new MarkerPlacedEvent(marker, pos, placer);
		}
		else {
			return new MarkerRemovedEvent(marker, placer);
		}
	}
}
