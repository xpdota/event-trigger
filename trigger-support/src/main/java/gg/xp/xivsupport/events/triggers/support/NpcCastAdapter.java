package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.FeedHandlerChildInfo;
import gg.xp.reevent.scan.FeedHelperAdapter;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.util.RepeatSuppressor;

import java.time.Duration;
import java.util.Optional;

@ScanMe
public class NpcCastAdapter implements FeedHelperAdapter<NpcCastCallout, AbilityCastStart, ModifiableCallout<AbilityCastStart>> {

	private final ActiveCastRepository casts;

	public NpcCastAdapter(ActiveCastRepository casts) {
		this.casts = casts;
	}

	@Override
	public Class<AbilityCastStart> eventType() {
		return AbilityCastStart.class;
	}

	@Override
	public TypedEventHandler<AbilityCastStart> makeHandler(FeedHandlerChildInfo<NpcCastCallout, ModifiableCallout<AbilityCastStart>> info) {
		NpcCastCallout ann = info.getAnnotation();
		long[] castIds = ann.value();
		long suppMs = ann.suppressMs();
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
							RawModifiedCallout<AbilityCastStart> modified = info.getHandlerFieldValue().getModified(event);
							if (ann.cancellable()) {
								modified.addExpiryCondition(() -> {
									Optional<CastTracker> ctOpt = casts.forCast(event);
									if (ctOpt.isEmpty()) {
										return true;
									}
									CastTracker ct = ctOpt.get();
									switch (ct.getResult()) {
										// Still in progress
										case IN_PROGRESS -> {
											return false;
										}
										// Wait for normal expiry delay. This acts as an "OR" so returning false
										// means to defer to the existing logic.
										case SUCCESS -> {
											return false;
										}
										// Remove immediately if interrupted
										case INTERRUPTED -> {
											return true;
										}
										// ?
										case UNKNOWN -> {
											return false;
										}
									}
									return false;
								});
							}
							context.accept(modified);
						}
						return;
					}
				}
			}
		};
	}
}
