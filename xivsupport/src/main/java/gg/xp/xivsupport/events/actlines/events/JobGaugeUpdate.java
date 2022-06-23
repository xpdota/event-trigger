package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.Event;
import gg.xp.xivdata.data.Job;

public interface JobGaugeUpdate extends Event {
	Job getJob();
}
