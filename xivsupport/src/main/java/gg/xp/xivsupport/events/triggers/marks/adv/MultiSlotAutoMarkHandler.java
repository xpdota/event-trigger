package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MultiSlotAutoMarkHandler<X extends Enum<X>> {

	private static final Logger log = LoggerFactory.getLogger(MultiSlotAutoMarkHandler.class);
	private final Consumer<Event> eventConsumer;
	private final MultiSlotAutomarkSetting<X> setting;
	private final Class<X> clazz;
	private final List<XivPlayerCharacter> toClear = new ArrayList<>();

	public MultiSlotAutoMarkHandler(Consumer<Event> eventConsumer, MultiSlotAutomarkSetting<X> setting) {
		this.eventConsumer = eventConsumer;
		this.setting = setting;
		clazz = setting.getEnumCls();
	}

	public void process(X slot, XivPlayerCharacter player) {
		if (player == null) {
			return;
		}
		MarkerSign markerFor = setting.getMarkerFor(slot);
		if (markerFor != null) {
			eventConsumer.accept(new SpecificAutoMarkRequest(player, markerFor));
			toClear.add(player);
		}
	}

	public void processMulti(Map<X, ? extends XivPlayerCharacter> assignments) {
		// Sort to ensure consistent ordering even if the map implementation doesn't care about order
		assignments.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().ordinal()))
				.forEach(entry -> process(entry.getKey(), entry.getValue()));
	}

	public void clearAll() {
		if (toClear.size() >= 8) {
			eventConsumer.accept(new ClearAutoMarkRequest());
		}
		else {
			for (XivPlayerCharacter xpc : toClear) {
				eventConsumer.accept(new SpecificAutoMarkRequest(xpc, MarkerSign.CLEAR));
			}
		}
		toClear.clear();
	}

	public void clearAllFast() {
		eventConsumer.accept(new ClearAutoMarkRequest());
		toClear.clear();
	}

	public void processRange(List<XivPlayerCharacter> players, X startInclusive, X endInclusive) {
		int startOrdinal = startInclusive.ordinal();
		int endOrdinal = endInclusive.ordinal();
		if (startOrdinal > endOrdinal) {
			throw new IllegalArgumentException("Start was a higher ordinal than the end: %s vs %s".formatted(startInclusive, endInclusive));
		}
		for (int i = 0; i < players.size(); i++) {
			int ordinal = startOrdinal + i;
			if (ordinal > endOrdinal) {
				log.warn("Too many entries ({}) for range [{}, {}]", players.size(), startInclusive, endInclusive);
				return;
			}
			X item = clazz.getEnumConstants()[ordinal];
			process(item, players.get(i));
		}
	}
}
