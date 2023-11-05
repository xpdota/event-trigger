package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.SnapshotLocationDataEvent;
import gg.xp.xivsupport.models.Position;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line264Parser extends AbstractACTLineParser<Line264Parser.Fields> {


	public Line264Parser(PicoContainer container) {
		super(container, 264, Fields.class);
	}

	enum Fields {
		entityId, abilityId, sequence, hasData, x, y, z, rotation
	}

	private @Nullable AbilityUsedEvent last;

	@HandleEvents
	public void consumeAbilityCast(AbilityUsedEvent aue) {
		if (aue.isFirstTarget()) {
			this.last = aue;
		}
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		int hasData = fields.getInt(Fields.hasData);
		if (hasData == 0) {
			return null;
		}
		long entity = fields.getHex(Fields.entityId);
		long ability = fields.getHex(Fields.abilityId);
		long sequenceId = fields.getHex(Fields.sequence);
		if (last != null
		    && last.getSource().getId() == entity
		    && last.getAbility().getId() == ability
		    && last.getSequenceId() == sequenceId) {
			double x = fields.getDouble(Fields.x);
			double y = fields.getDouble(Fields.y);
			double z = fields.getDouble(Fields.z);
			double h = fields.getDouble(Fields.rotation);
			SnapshotLocationDataEvent out;
			if (x == 0.0 && y == 0.0 && z == 0.0) {
				if (h == 0.0) {
					return null;
				}
				else {
					out = new SnapshotLocationDataEvent(last, h);
				}
			}
			else {
				Position pos = new Position(x, y, z, h);
				out = new SnapshotLocationDataEvent(last, pos);
			}
			last.setLocationInfo(out);
			return out;
		}
		return null;
	}

}
