package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.reevent.scan.HandleEvents;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractACTLineParser<F extends Enum<F>> {

	private final Class<? extends Enum<F>> enumCls;
	private final int lineNumber;
	private final String lineStart;
	private final List<@Nullable F> groups;

	AbstractACTLineParser(int logLineNumber, Class<F> enumCls) {
		this(logLineNumber, Arrays.asList(enumCls.getEnumConstants()));
	}

	@SuppressWarnings({"ConstantConditions", "unchecked"})
	AbstractACTLineParser(int logLineNumber, List<@Nullable F> groups) {
		if (groups.isEmpty()) {
			// TODO: could some of them make sense as empty?
			throw new IllegalArgumentException("Capture groups cannot be empty");
		}
		this.groups = new ArrayList<>(groups);
		F anyCap = groups.stream()
				.filter(Objects::nonNull)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Must have a non-null capture group"));
		enumCls = (Class<? extends Enum<F>>) anyCap.getClass();
		lineNumber = logLineNumber;
		lineStart = String.format("%02d", logLineNumber) + '|';
	}

	@SuppressWarnings("unchecked")
	@HandleEvents
	public void handle(EventContext<Event> context, ACTLogLineEvent event) {
		String line = event.getLogLine();
		if (line.startsWith(lineStart)) {
			int numSplits = groups.size() + 3;

			String[] splits = line.split("\\|", numSplits);
			Map<F, String> out = new EnumMap<>((Class<F>) enumCls);
			// TODO: validate number of fields
			for (int i = 0; i < groups.size(); i++) {
				// i + 2 is because the first two are the line number and timestamp.
				out.put(groups.get(i), splits[i + 2]);
			}
			ZonedDateTime zdt = ZonedDateTime.parse(splits[1]);
			FieldMapper<F> mapper = new FieldMapper<>(out, context, shouldIgnoreEntityLookupMisses());
			Event outgoingEvent;
			try {
				outgoingEvent = convert(mapper, lineNumber, zdt);
			}
			catch (Throwable t) {
				throw new IllegalArgumentException("Error parsing ACT line: " + line, t);
			}
			if (mapper.isFlaggedForCombatantUpdate()) {
				context.accept(new RefreshCombatantsRequest());
			}
			if (outgoingEvent != null) {
				outgoingEvent.setHappenedAt(zdt.toInstant());
				context.accept(outgoingEvent);
			}
		}
	}

	// TODO: consider other ways of handling line number + timestamp
	protected abstract @Nullable Event convert(FieldMapper<F> fields, int lineNumber, ZonedDateTime time);

	protected boolean shouldIgnoreEntityLookupMisses() {
		return false;
	}
}
