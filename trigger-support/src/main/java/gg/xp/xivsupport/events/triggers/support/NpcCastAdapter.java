package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.FeedHandlerChildInfo;
import gg.xp.reevent.scan.FeedHelperAdapter;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;

import java.time.Duration;

@ScanMe
public class NpcCastAdapter implements FeedHelperAdapter<NpcCastCallout, AbilityCastStart, ModifiableCallout<AbilityCastStart>> {

	@Override
	public Class<AbilityCastStart> eventType() {
		return AbilityCastStart.class;
	}

	@Override
	public TypedEventHandler<AbilityCastStart> makeHandler(FeedHandlerChildInfo<NpcCastCallout, ModifiableCallout<AbilityCastStart>> info) {
		long[] castIds = info.getAnnotation().value();
		long suppMs = info.getAnnotation().suppressMs();
		RepeatSuppressor supp;
		if (suppMs >= 0) {
			supp = new RepeatSuppressor(Duration.ofMillis(suppMs));
		}
		else {
			supp = RepeatSuppressor.noOp();
		}
		return new TypedEventHandler<>() {
			@Override
			public Class<? extends AbilityCastStart> getType() {
				return AbilityCastStart.class;
			}

			@Override
			public void handle(EventContext context, AbilityCastStart event) {
				for (int i = 0; i < castIds.length; i++) {
					if (!event.getSource().isPc() && castIds[i] == event.getAbility().getId()) {
						if (supp.check(event)) {
							context.accept(info.getHandlerFieldValue().getModified(event));
						}
						return;
					}
				}
			}
		};
	}
}
