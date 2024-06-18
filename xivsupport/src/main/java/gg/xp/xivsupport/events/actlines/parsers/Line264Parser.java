package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.AnimationLockEvent;
import gg.xp.xivsupport.events.actlines.events.SnapshotLocationDataEvent;
import gg.xp.xivsupport.events.misc.OverwritingRingBuffer;
import gg.xp.xivsupport.models.Position;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line264Parser extends AbstractACTLineParser<Line264Parser.Fields> {


	public Line264Parser(PicoContainer container) {
		super(container, 264, Fields.class);
	}

	enum Fields {
		entityId, abilityId, sequence, hasData, x, y, z, rotation, animationLock
	}

	private final OverwritingRingBuffer<AbilityUsedEvent> buffer = new OverwritingRingBuffer<>(32);

	@HandleEvents
	public void consumeAbilityUse(AbilityUsedEvent aue) {
		if (aue.isFirstTarget()) {
			buffer.write(aue);
		}
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		long entity = fields.getHex(Fields.entityId);
		long ability = fields.getHex(Fields.abilityId);
		long sequenceId = fields.getHex(Fields.sequence);
		AbilityUsedEvent last;
		while ((last = buffer.read()) != null) {
			if (last.getSource().getId() == entity
			    && last.getAbility().getId() == ability
			    && last.getSequenceId() == sequenceId) {

				MultipleEvent out = new MultipleEvent();

				// Cast location/angle
				// First, check if the line indicates that such data is actually present.
				int hasData = fields.getInt(Fields.hasData);
				SnapshotLocationDataEvent slde;
				// 0 = no data, 1 = data, 256 = error
				if (hasData == 1) {
					double x = fields.getDouble(Fields.x);
					double y = fields.getDouble(Fields.y);
					double z = fields.getDouble(Fields.z);
					double h = fields.getDouble(Fields.rotation);
					if (x == 0.0 && y == 0.0 && z == 0.0) {
						if (h == 0.0) {
							slde = null;
						}
						else {
							slde = new SnapshotLocationDataEvent(last, h);
						}
					}
					else {
						Position pos = new Position(x, y, z, h);
						slde = new SnapshotLocationDataEvent(last, pos);
					}
					if (slde != null) {
						last.setLocationInfo(slde);
						out.add(slde);
					}
				}
				if (fields.hasField(Fields.animationLock)) {
					double animLock = fields.getDouble(Fields.animationLock);
					if (animLock > 0) {
						AnimationLockEvent lock = new AnimationLockEvent(last, animLock);
						last.setAnimationLock(lock.getInitialDuration());
						out.add(lock);
					}
				}
				return out;
			}
		}
		return null;
	}

}
