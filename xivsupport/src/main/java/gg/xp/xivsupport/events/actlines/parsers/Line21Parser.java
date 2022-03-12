package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line21Parser extends AbstractACTLineParser<NetworkAbilityFields> {

	public Line21Parser(PicoContainer container) {
		super(container, 21, NetworkAbilityFields.class, true);
	}

	@Override
	protected Event convert(FieldMapper<NetworkAbilityFields> fields, int lineNumber, ZonedDateTime time) {
		return NetworkAbilityFields.convert(fields);
	}
}
