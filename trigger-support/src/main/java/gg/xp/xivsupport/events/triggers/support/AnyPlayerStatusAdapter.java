package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.FeedHandlerChildInfo;
import gg.xp.reevent.scan.FeedHelperAdapter;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;

import java.time.Duration;

@ScanMe
public class AnyPlayerStatusAdapter implements FeedHelperAdapter<AnyPlayerStatusCallout, BuffApplied, ModifiableCallout<BuffApplied>> {

	@Override
	public Class<BuffApplied> eventType() {
		return BuffApplied.class;
	}

	@Override
	public TypedEventHandler<BuffApplied> makeHandler(FeedHandlerChildInfo<AnyPlayerStatusCallout, ModifiableCallout<BuffApplied>> info) {
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
			public Class<? extends BuffApplied> getType() {
				return BuffApplied.class;
			}

			@Override
			public void handle(EventContext context, BuffApplied event) {
				if (event.buffIdMatches(castIds)) {
					if (supp.check(event)) {
						context.accept(info.getHandlerFieldValue().getModified(event));
					}
				}
			}
		};
	}
}
