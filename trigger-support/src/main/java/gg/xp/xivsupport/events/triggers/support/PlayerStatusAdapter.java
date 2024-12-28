package gg.xp.xivsupport.events.triggers.support;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TypedEventHandler;
import gg.xp.reevent.scan.FeedHandlerChildInfo;
import gg.xp.reevent.scan.FeedHelperAdapter;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectCurrentStatus;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;

import java.util.Optional;

@ScanMe
public class PlayerStatusAdapter implements FeedHelperAdapter<PlayerStatusCallout, BuffApplied, ModifiableCallout<BuffApplied>> {

	private final StatusEffectRepository buffs;

	public PlayerStatusAdapter(StatusEffectRepository buffs) {
		this.buffs = buffs;
	}

	@Override
	public Class<BuffApplied> eventType() {
		return BuffApplied.class;
	}

	@Override
	public TypedEventHandler<BuffApplied> makeHandler(FeedHandlerChildInfo<PlayerStatusCallout, ModifiableCallout<BuffApplied>> info) {
		long[] castIds = info.getAnnotation().value();
		PlayerStatusCallout ann = info.getAnnotation();
		return new TypedEventHandler<>() {
			@Override
			public Class<? extends BuffApplied> getType() {
				return BuffApplied.class;
			}

			@Override
			public void handle(EventContext context, BuffApplied event) {
				if (event.getTarget().isThePlayer() && event.buffIdMatches(castIds)) {
					RawModifiedCallout<BuffApplied> modified = info.getHandlerFieldValue().getModified(event);
					if (ann.cancellable()) {
						modified.addExpiryCondition(() -> {
							StatusEffectCurrentStatus cs = buffs.statusOf(event);
							return cs != StatusEffectCurrentStatus.ACTIVE;
						});
					}
					context.accept(modified);
				}
			}
		};
	}
}
