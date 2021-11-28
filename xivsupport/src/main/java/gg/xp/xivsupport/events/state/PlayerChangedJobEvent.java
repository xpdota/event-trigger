package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.jobs.Job;

public class PlayerChangedJobEvent extends BaseEvent {

	private final Job oldJob;
	private final Job newJob;

	public PlayerChangedJobEvent(Job oldJob, Job newJob) {
		this.oldJob = oldJob;
		this.newJob = newJob;
	}

	public Job getOldJob() {
		return oldJob;
	}

	public Job getNewJob() {
		return newJob;
	}
}
