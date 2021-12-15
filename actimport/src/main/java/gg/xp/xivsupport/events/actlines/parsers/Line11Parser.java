package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.RawXivPartyInfo;
import org.assertj.core.util.Strings;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Line11Parser extends AbstractACTLineParser<Line11Parser.Fields> {

	public Line11Parser() {
		super(11, Fields.class);
	}

	enum Fields {
		count, id1, id2, id3, id4, id5, id6, id7, id8
	}

	@Override
	protected Event convert(FieldMapper<Fields> fields, int lineNumber, ZonedDateTime time) {
		List<RawXivPartyInfo> raw = Stream.of(
						Fields.id1,
						Fields.id2,
						Fields.id3,
						Fields.id4,
						Fields.id5,
						Fields.id6,
						Fields.id7,
						Fields.id8
				).map(fields::getString)
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
		return new PartyChangeEvent(raw);
	}
}
