package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.misc.BattleTalkEvent;
import gg.xp.xivsupport.events.misc.NpcYellEvent;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line267Parser extends AbstractACTLineParser<Line267Parser.Fields> {


	public Line267Parser(PicoContainer container) {
		super(container, 267, Fields.class);
	}

	enum Fields {
		entityId, nameId, instanceContentTextId
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		return new BattleTalkEvent(fields.getEntity(Fields.entityId), fields.getHex(Fields.instanceContentTextId));
	}

}
