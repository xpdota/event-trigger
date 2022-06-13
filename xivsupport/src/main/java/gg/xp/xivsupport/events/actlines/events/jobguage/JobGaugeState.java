package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;
import org.jetbrains.annotations.Nullable;

public class JobGaugeState {

	private JobGaugeUpdate lastUpdate;

	@HandleEvents
	public void handleJobGaugeEvent(EventContext context, JobGaugeUpdate gauge) {
		lastUpdate = gauge;
	}

	public JobGaugeUpdate getLastUpdate() {
		return lastUpdate;
	}

	public <X> @Nullable X getLastUpdateOf(Class<X> clazz) {
		JobGaugeUpdate upd = lastUpdate;
		if (clazz.isInstance(upd)) {
			return (X) upd;
		}
		return null;
	}
}
