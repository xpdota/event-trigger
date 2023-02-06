package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class MultiSlotAutoMarkHandler<X extends Enum<X>> {

	private static final Logger log = LoggerFactory.getLogger(MultiSlotAutoMarkHandler.class);
	private final Consumer<SpecificAutoMarkRequest> eventConsumer;
	private final MultiSlotAutomarkSetting<X> setting;
	private final Class<X> clazz;

	public MultiSlotAutoMarkHandler(Consumer<SpecificAutoMarkRequest> eventConsumer, MultiSlotAutomarkSetting<X> setting) {
		this.eventConsumer = eventConsumer;
		this.setting = setting;
		clazz = setting.getEnumCls();
	}

	public void process(X slot, XivPlayerCharacter player) {
		MarkerSign markerFor = setting.getMarkerFor(slot);
		if (markerFor != null) {
			eventConsumer.accept(new SpecificAutoMarkRequest(player, markerFor));
		}
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
