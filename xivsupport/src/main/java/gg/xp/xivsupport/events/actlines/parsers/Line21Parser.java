package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.XivStateImpl;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line21Parser extends AbstractACTLineParser<NetworkAbilityFields> {

	public Line21Parser(org.picocontainer.PicoContainer container) {
		super(container, 21, NetworkAbilityFields.class, true);
	}

	@Override
	protected Event convert(FieldMapper<NetworkAbilityFields> fields, int lineNumber, ZonedDateTime time) {
		return NetworkAbilityFields.convert(fields);
	}
}
