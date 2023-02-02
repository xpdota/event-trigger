package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.settings.MultiSlotAutomarkSetting;

import java.util.function.Consumer;

public class MultiSlotAutoMarkHandler<X extends Enum<X>> {

	private final Consumer<SpecificAutoMarkRequest> eventConsumer;
	private final MultiSlotAutomarkSetting<X> setting;

	public MultiSlotAutoMarkHandler(Consumer<SpecificAutoMarkRequest> eventConsumer, MultiSlotAutomarkSetting<X> setting) {
		this.eventConsumer = eventConsumer;
		this.setting = setting;
	}

	public void process(X slot, XivPlayerCharacter player) {
		MarkerSign markerFor = setting.getMarkerFor(slot);
		if (markerFor != null) {
			eventConsumer.accept(new SpecificAutoMarkRequest(player, markerFor));
		}
	}
}
