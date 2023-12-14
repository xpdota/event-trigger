package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.timelines.cbevents.CbEventFmt;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class FileEventSyncController implements EventSyncController {

	private final Class<? extends Event> eventType;
	private final Predicate<Event> predicate;
	private final String originalType;
	private final Map<String, String> original;

	public FileEventSyncController(Class<? extends Event> eventType, Predicate<Event> predicate, String originalType, Map<String, String> original) {
		this.eventType = eventType;
		this.predicate = predicate;
		this.originalType = originalType;
		this.original = new HashMap<>(original);
	}

	@Override
	public boolean shouldSync(Event event) {
		return predicate.test(event);
	}

	@Override
	public Class<? extends Event> eventType() {
		return eventType;
	}

	@Override
	public EventSyncController translateWith(LanguageReplacements replacements) {
		Map<String, String> modified = new HashMap<>(original);
		modified.replaceAll((key, val) -> {
			for (var syncReplacement : replacements.replaceSync().entrySet()) {
				val = syncReplacement.getKey().matcher(val).replaceAll(syncReplacement.getValue());
			}
			return val;
		});
		if (modified.equals(original)) {
			return this;
		}
		return CbEventFmt.parse(originalType, modified);
	}

	@Override
	public String toTextFormat() {
		return CbEventFmt.format(this.originalType, this.original);
	}

	@Override
	public String toString() {
		return eventType.getSimpleName() + original;
	}
}
