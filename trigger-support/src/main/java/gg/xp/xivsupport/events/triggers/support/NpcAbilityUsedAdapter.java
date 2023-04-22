package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.FeedHandlerChildInfo;
import gg.xp.reevent.scan.FeedHelperAdapter;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;

import java.time.Duration;

@ScanMe
public class NpcAbilityUsedAdapter implements FeedHelperAdapter<NpcAbilityUsedCallout, AbilityUsedEvent, ModifiableCallout<AbilityUsedEvent>> {

	@Override
	public Class<AbilityUsedEvent> eventType() {
		return AbilityUsedEvent.class;
	}

	@Override
	public TypedEventHandler<AbilityUsedEvent> makeHandler(FeedHandlerChildInfo<NpcAbilityUsedCallout, ModifiableCallout<AbilityUsedEvent>> info) {
		NpcAbilityUsedCallout annotation = info.getAnnotation();
		long[] castIds = annotation.value();
		long suppressMs = annotation.suppressMs();
		RepeatSuppressor supp = new RepeatSuppressor(Duration.ofMillis(suppressMs));
		return new TypedEventHandler<>() {
			@Override
			public Class<? extends AbilityUsedEvent> getType() {
				return AbilityUsedEvent.class;
			}

			@Override
			public void handle(EventContext context, AbilityUsedEvent event) {
				for (int i = 0; i < castIds.length; i++) {
					if (!event.getSource().isPc() && castIds[i] == event.getAbility().getId()) {
						if (suppressMs <= 0 || supp.check(event)) {
							context.accept(info.getHandlerFieldValue().getModified(event));
							return;
						}
					}
				}
			}
		};
	}
}
