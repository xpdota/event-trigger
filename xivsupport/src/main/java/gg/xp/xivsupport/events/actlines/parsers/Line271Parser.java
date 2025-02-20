package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.Position;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line271Parser extends AbstractACTLineParser<Line271Parser.Fields> {

	private static final Logger log = LoggerFactory.getLogger(Line271Parser.class);
	private final XivState state;

	public Line271Parser(PicoContainer container, XivState state) {
		super(container, 271, Line271Parser.Fields.class);
		this.state = state;
	}

	enum Fields {
		entityId, heading, unknown0, unknown1, x, y, z
	}

	@Override
	protected @Nullable Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		state.provideCombatantPos(fields.getEntity(Fields.entityId),
				new Position(
						fields.getDouble(Fields.x),
						fields.getDouble(Fields.y),
						fields.getDouble(Fields.z),
						fields.getDouble(Fields.heading)), true);
		return null;
	}
}
