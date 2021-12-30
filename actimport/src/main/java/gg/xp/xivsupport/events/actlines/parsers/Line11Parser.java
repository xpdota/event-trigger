package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.ActImportOnly;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.RawXivPartyInfo;
import gg.xp.xivsupport.events.state.XivStateImpl;
import org.assertj.core.util.Strings;
import org.picocontainer.PicoContainer;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public class Line11Parser extends AbstractACTLineParser<Line11Parser.Fields> implements ActImportOnly {

	public Line11Parser(PicoContainer container) {
		super(container, 11, Fields.class, true);
	}

	enum Fields {
		count
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		long count = fields.getLong(Fields.count);
		List<String> raw = fields.getRawLineSplit();
		List<RawXivPartyInfo> out = IntStream.range(0, (int) count)
				.mapToObj(i -> raw.get(i + 3))
				.filter(s -> !Strings.isNullOrEmpty(s))
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
