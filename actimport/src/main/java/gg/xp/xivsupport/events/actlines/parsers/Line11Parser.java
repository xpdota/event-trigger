package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.ActImportOnly;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.RawXivPartyInfo;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public class Line11Parser extends AbstractACTLineParser<Line11Parser.Fields> implements ActImportOnly {

	public Line11Parser(PicoContainer container) {
		super(container, 11, Fields.class);
	}

	enum Fields {
		count
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		long count = fields.getLong(Fields.count);
		List<String> raw = fields.getRawLineSplit();
		// Subtract four - line number, timestamp, count, hash
		// This is due to an ACT bug where it can report a party size of 8, but not actually list all 8, e.g.
		// 11|2022-09-02T18:46:48.5120000+08:00|8|1029C88D|1033290F|101FE2E6|102B4E61|10308B6B|10296516|10327D18|7bbaf2d12623bd7b
		long realCount = Math.min(count, raw.size() - 4);
		List<RawXivPartyInfo> out = IntStream.range(0, (int) realCount)
				.mapToObj(i -> raw.get(i + 3))
				.filter(s -> !(s == null || s.isEmpty()))
				// We can use bad info for most of this because it gets replaced by the fake real combatant info anyway
				// (from 03-lines)
				.map(id -> new RawXivPartyInfo(
						id,
						"",
						0,
						0,
						0,
						true
				))
				.collect(Collectors.toList());
		return new PartyChangeEvent(out);
	}
}
