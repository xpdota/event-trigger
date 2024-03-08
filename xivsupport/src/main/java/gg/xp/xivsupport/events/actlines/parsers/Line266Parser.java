package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.misc.NpcYellEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line266Parser extends AbstractACTLineParser<Line266Parser.Fields> {


	public Line266Parser(PicoContainer container) {
		super(container, 266, Fields.class);
	}

	enum Fields {
		entityId, nameId, yellId
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new NpcYellEvent(fields.getEntity(Fields.entityId), NpcYellLibrary.INSTANCE.forId(fields.getHex(Fields.yellId)));
	}

}
