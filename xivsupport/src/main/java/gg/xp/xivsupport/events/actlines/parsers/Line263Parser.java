package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.CastLocationDataEvent;
import gg.xp.xivsupport.events.misc.OverwritingRingBuffer;
import gg.xp.xivsupport.models.Position;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line263Parser extends AbstractACTLineParser<Line263Parser.Fields> {


	public Line263Parser(PicoContainer container) {
		super(container, 263, Fields.class);
	}

	enum Fields {
		entityId, abilityId, x, y, z, rotation
	}

	private final OverwritingRingBuffer<AbilityCastStart> buffer = new OverwritingRingBuffer<>(32);

	@HandleEvents
	public void consumeAbilityCast(AbilityCastStart acs) {
		buffer.write(acs);
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		long entity = fields.getHex(Fields.entityId);
		long ability = fields.getHex(Fields.abilityId);
		AbilityCastStart last;
		while ((last = buffer.read()) != null) {
			if ((last.getTarget().isEnvironment()
			     || last.getTarget().equals(last.getSource()))
			    && last.getSource().getId() == entity
			    && last.getAbility().getId() == ability) {
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
						out = new CastLocationDataEvent(last, h);
					}
				}
				else {
					Position pos = new Position(x, y, z, h);
					out = new CastLocationDataEvent(last, pos);
				}
				last.setLocationInfo(out);
				return out;
			}
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
