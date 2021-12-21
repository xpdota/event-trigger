package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import gg.xp.xivsupport.events.state.XivStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line03Parser extends AbstractACTLineParser<Line03Parser.Fields> {

	private static final Logger log = LoggerFactory.getLogger(Line03Parser.class);

	public Line03Parser(XivStateImpl state) {
		super(state,  3, Fields.class);
	}

	enum Fields {
		id, name, job, level,
		ownerId,
		worldId, world,
		bNpcNameId, bNpcId,
		currentHp, maxHp,
		currentMp, maxMp,
		unknown1, unknown2,
		xPos, yPos, zPos, heading
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		try {
			long rawId = fields.getHex(Fields.id);
			int type;
			if (rawId >= 0x1000_0000 && rawId < 0x1FFF_FFFF) {
				type = 1;
			}
			else {
				type = 2;
			}
			return new RawAddCombatantEvent(fields.getEntity(Fields.id, Fields.name),
					// Type not in the line - have to guess
					new RawXivCombatantInfo(
							fields.getHex(Fields.id),
							fields.getString(Fields.name),
							fields.getHex(Fields.job),
							type,
							fields.getLong(Fields.currentHp),
							fields.getLong(Fields.maxHp),
							fields.getLong(Fields.currentMp),
							fields.getLong(Fields.maxMp),
							fields.getHex(Fields.level),
							fields.getDouble(Fields.xPos),
							fields.getDouble(Fields.yPos),
							fields.getDouble(Fields.zPos),
							fields.getDouble(Fields.heading),
							fields.getHex(Fields.worldId),
							fields.getString(Fields.world),
							fields.getLong(Fields.bNpcId),
							fields.getLong(Fields.bNpcNameId),
							0,
							fields.getHex(Fields.ownerId))
					);
		} catch (Throwable t) {
			log.warn("Error parsing full data from 03-line, falling back to barebones", t);
			return new RawAddCombatantEvent(fields.getEntity(Fields.id, Fields.name));
		}
	}

	@Override
	protected EntityLookupMissBehavior entityLookupMissBehavior() {
		return EntityLookupMissBehavior.GET;
	}
}
