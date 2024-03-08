package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.state.combatstate.CountdownCanceledEvent;
import gg.xp.xivsupport.events.state.combatstate.CountdownStartedEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;

import java.time.Duration;
import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line269Parser extends AbstractACTLineParser<Line269Parser.Fields> {

	public Line269Parser(PicoContainer container) {
		super(container, 269, Fields.class);
	}

	enum Fields {
		entityId, senderWorldId, entityName
	}

	private CountdownStartedEvent lastCountdown;

	@HandleEvents
	public void trackLastCountdown(CountdownStartedEvent event) {
		this.lastCountdown = event;
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		CountdownStartedEvent last = lastCountdown;
		CountdownCanceledEvent event = new CountdownCanceledEvent(
				fields.getEntity(Fields.entityId, Fields.entityName),
				last
		);
		if (last != null) {
			last.markAsCanceled();
		}
		return event;
	}

}
