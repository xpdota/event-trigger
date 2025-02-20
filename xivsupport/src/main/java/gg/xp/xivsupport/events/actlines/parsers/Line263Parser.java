package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.models.Position;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Line263Parser extends AbstractACTLineParser<Line263Parser.Fields> {


	public Line263Parser(PicoContainer container) {
		super(container, 263, Fields.class);
	}

	enum Fields {
		entityId, abilityId, x, y, z, rotation
	}

	private final Map<Long, AbilityCastStart> lastCastByEntity = new HashMap<>();

	@HandleEvents
	public void consumeAbilityCast(AbilityCastStart acs) {
		lastCastByEntity.put(acs.getSource().getId(), acs);
	}

	@HandleEvents
	public void clear(PullStartedEvent pse) {
		lastCastByEntity.clear();
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		long entity = fields.getHex(Fields.entityId);
		long ability = fields.getHex(Fields.abilityId);
		AbilityCastStart match = lastCastByEntity.remove(entity);
		if (match == null) {
			return null;
		}
		// Logic: Ignore the position info if there is a real target for the cast, since we know that it is unit targeted in that case.
		if ((match.getTarget().isEnvironment()
		     || match.getTarget().equals(match.getSource()))
		    && match.getSource().getId() == entity
		    && match.getAbility().getId() == ability) {
			double x = fields.getDouble(Fields.x);
			double y = fields.getDouble(Fields.y);
			double z = fields.getDouble(Fields.z);
			double h = fields.getDouble(Fields.rotation);
			CastLocationDataEvent out;
			if (x == 0.0 && y == 0.0 && z == 0.0) {
				if (h == 0.0) {
					return null;
				}
				else {
					out = new CastLocationDataEvent(match, h);
				}
			}
			else {
				Position pos = new Position(x, y, z, h);
				out = new CastLocationDataEvent(match, pos);
			}
			match.setLocationInfo(out);
			return out;
		}
		return null;
	}
	/*
		Notes:
		Non-targeted skills will have the source's location+heading, and then
			a no-data 264.
		Entity-targeted skills like Glare will have the target location but the
			source's heading in the 263, and then a no-data 264.
		Holy's 263 has source's location and heading, while the 264 contains only heading.
		Aqua Breath (line/cone) has location + heading for cast, and then
			264 is only heading.
		True ground target (like bomb toss) has full info for both.


	 */

}
