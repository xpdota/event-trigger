package gg.xp.xivsupport.events.triggers.easytriggers.actions;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.callouts.CalloutTrackingKey;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.speech.ProcessedCalloutEvent;
import org.apache.commons.lang3.StringUtils;

public class SoundAction implements Action<Event> {

	public String sound;

	@Override
	public void accept(EasyTriggerContext context, Event event) {
		if (StringUtils.isNotBlank(sound)) {
			context.accept(new ProcessedCalloutEvent(new CalloutTrackingKey(), null, null, () -> true, () -> null, null, sound));
		}
	}

	@Override
	public String fixedLabel() {
		return "Play Sound";
	}

	@Override
	public String dynamicLabel() {
		return "Play Sound " + (sound == null ? "(nothing)" : sound);
	}

	@Override
	public String toString() {
		return "SoundAction{" +
		       "sound='" + sound + '\'' +
		       '}';
	}
}
