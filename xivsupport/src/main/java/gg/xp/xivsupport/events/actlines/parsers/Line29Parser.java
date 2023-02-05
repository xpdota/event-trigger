package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.PlayerMarkerPlacedEvent;
import gg.xp.xivsupport.events.actlines.events.PlayerMarkerRemovedEvent;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line29Parser extends AbstractACTLineParser<Line29Parser.Fields> {

	public Line29Parser(PicoContainer container) {
		super(container, 29, Fields.class);
	}

	enum Fields {
		addRemove, markerId, placerId, placerName, targetId, targetName
	}

	@Override
	protected @Nullable Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		// TODO: is this hex or decimal?
		MarkerSign sign = MarkerSign.fromId(fields.getInt(Fields.markerId));
		XivCombatant source = fields.getEntity(Fields.placerId, Fields.placerName);
		XivCombatant target = fields.getEntity(Fields.targetId, Fields.targetName);
		if (source.getId() == 0) {
			// this case happens when a marker is implicitly removed by overwriting it with another marker
			source = XivCombatant.ENVIRONMENT;
		}
		String addRemove = fields.getString(Fields.addRemove);
		if ("Add".equals(addRemove)) {
			return new PlayerMarkerPlacedEvent(sign, source, target);
		}
		else {
			return new PlayerMarkerRemovedEvent(sign, source, target);
		}
	}
}
