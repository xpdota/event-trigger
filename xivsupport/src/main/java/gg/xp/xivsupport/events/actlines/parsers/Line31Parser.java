package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.RawJobGaugeEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;

@SuppressWarnings("unused")
public class Line31Parser extends AbstractACTLineParser<Line31Parser.Fields> {

	public Line31Parser(PicoContainer container) {
		super(container,  31, Fields.class);
	}

	enum Fields {
		targetId, data0, data1, data2, data3
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		XivCombatant target = fields.getEntity(Fields.targetId);
		long data0 = fields.getHex(Fields.data0);
		long data1 = fields.getHex(Fields.data1);
		long data2 = fields.getHex(Fields.data2);
		long data3 = fields.getHex(Fields.data3);
		long[] in = {data0, data1, data2, data3};
		byte[] out = new byte[16];
		for (int i = 0; i < 4; i++) {
			long part = in[i];
			for (int j = 0; j < 4; j++) {
				out[4 * i + j] = (byte) (part & 0xff);
				part >>= 8;
			}
		}
		Job job = Job.getById(data0 & 0xff);
		return new RawJobGaugeEvent(target, job, out);
//		return new BuffRemoved(
//				fields.getStatus(Fields.buffId, Fields.buffName),
//				fields.getDouble(Fields.unknownFieldMaybeDuration),
//				fields.getEntity(Fields.sourceId, Fields.sourceName),
//				fields.getEntity(Fields.targetId, Fields.targetName),
//				fields.getHex(Fields.buffStacks)
//		);
	}
}
