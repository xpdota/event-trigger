package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.state.RefreshSpecificCombatantsRequest;
import gg.xp.xivsupport.events.state.XivState;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractACTLineParser<F extends Enum<F>> {

	private static final Logger log = LoggerFactory.getLogger(AbstractACTLineParser.class);

	private final Class<? extends Enum<F>> enumCls;
	private final int lineNumber;
	private final List<@Nullable F> groups;
	protected final XivState state;
	protected final @Nullable FakeTimeSource fakeTimeSource;

	AbstractACTLineParser(PicoContainer container, int logLineNumber, Class<F> enumCls) {
		this(container, logLineNumber, Arrays.asList(enumCls.getEnumConstants()));
	}

	@SuppressWarnings({"ConstantConditions", "unchecked"})
	AbstractACTLineParser(PicoContainer container, int logLineNumber, List<@Nullable F> groups) {
		this.state = Objects.requireNonNull(container.getComponent(XivState.class), "XivState is required");
		this.fakeTimeSource = container.getComponent(FakeACTTimeSource.class);
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
	}

	@SuppressWarnings("unchecked")
	@HandleEvents
	public void handle(EventContext context, ACTLogLineEvent event) {
		if (event.getLineNumber() != lineNumber) {
			return;
		}
		try {
			String line = event.getLogLine();
			String[] splits = event.getRawFields();
			Map<F, String> out = new EnumMap<>((Class<F>) enumCls);
			// Subtract 3 - line number, timestamp, hash
			int fieldCount = Math.min(groups.size(), splits.length - 3);
			// TODO: validate number of fields
			for (int i = 0; i < fieldCount; i++) {
				// i + 2 is because the first two are the line number and timestamp.
				out.put(groups.get(i), splits[i + 2]);
			}
			ZonedDateTime zdt = event.getTimestamp();
			FieldMapper<F> mapper = new FieldMapper<>(out, state, entityLookupMissBehavior(), splits);
			Event outgoingEvent;
			try {
				outgoingEvent = convert(mapper, lineNumber, zdt);
			}
			catch (Throwable t) {
				//noinspection ThrowCaughtLocally
				throw new IllegalArgumentException("Error parsing ACT line: " + line, t);
			}
			// TODO: check whether it's better to request all at once, individually
			mapper.getCombatantsToUpdate().forEach(id -> {
				context.accept(new RefreshSpecificCombatantsRequest(List.of(id)));
			});
			mapper.flushStateOverrides();
			if (fakeTimeSource != null) {
				// For 00-lines, the timestamp only has second-level precision, compared to millisecond-level
				// precision for everything else. This causes time to jump around, which we don't want.
				// Thus, for 00-lines, just copy
				if (zdt.get(ChronoField.MILLI_OF_SECOND) == 0) {
					event.setHappenedAt(fakeTimeSource.now());
				}
				else {
					fakeTimeSource.setNewTime(zdt.toInstant());
				}
				event.setTimeSource(fakeTimeSource);
			}
			if (outgoingEvent != null) {
				if (outgoingEvent instanceof MultipleEvent me) {
					me.events.forEach(context::accept);
				}
				else {
					context.accept(outgoingEvent);
				}
			}
		}
		catch (Throwable t) {
			throw new ActLineParseException(event.getLogLine(), t);
		}
	}

	protected abstract @Nullable Event convert(FieldMapper<F> fields, int lineNumber, ZonedDateTime time);

	protected EntityLookupMissBehavior entityLookupMissBehavior() {
		return EntityLookupMissBehavior.GET_AND_WARN;
	}

	public int getLineNumber() {
		return lineNumber;
	}
}
