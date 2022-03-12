package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.TickEvent;
import gg.xp.xivsupport.events.actlines.events.TickType;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line24Parser extends AbstractACTLineParser<Line24Parser.Fields> {

	private static final Logger log = LoggerFactory.getLogger(Line24Parser.class);

	public Line24Parser(PicoContainer container) {
		super(container, 24, Fields.class);
	}

	enum Fields {
		targetId, targetName,
		dotOrHot,
		effectId,
		damage,
		targetCurHp, targetMaxHp, targetCurMp, targetMaxMp, targetUnknown1, targetUnknown2, targetX, targetY, targetZ, targetHeading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		XivCombatant target = fields.getEntity(Fields.targetId, Fields.targetName, Fields.targetCurHp, Fields.targetMaxHp, Fields.targetCurMp, Fields.targetMaxMp, Fields.targetX, Fields.targetY, Fields.targetZ, Fields.targetHeading);
		// Not sure what to do yet, atm only care about the combatant update, but this *could* be useful for server tickers
		String which = fields.getString(Fields.dotOrHot);
		final TickType type;
		switch (which) {
			case "DoT" -> type = TickType.DOT;
			case "HoT" -> type = TickType.HOT;
			default -> {
				log.error("Invalid tick type '{}' for 24-line", which);
				return null;
			}
		}
		return new TickEvent(target, type, fields.getHex(Fields.damage), fields.getHex(Fields.effectId));
		// TODO: is HP coming from network for this? Or memory? If the former, could be trusted.
	}
}
