package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;

@SystemEvent
public class SgeGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {
	@Serial
	private static final long serialVersionUID = -4246891633874856301L;
	private final double addersGallOverall;
	private final int adderSting;
	private final boolean eukrasiaActive;

	public SgeGaugeEvent(double addersGallOverall, int adderSting, boolean eukrasiaActive) {
		this.addersGallOverall = addersGallOverall;
		this.adderSting = adderSting;
		this.eukrasiaActive = eukrasiaActive;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%.2f AG, %s AS, Euk %s", addersGallOverall, adderSting, eukrasiaActive ? "On" : "Off");
	}

	public double getAddersGallNow() {
		double result = addersGallOverall + (double) getEffectiveTimeSince().toMillis() / JobGaugeConstants.SGE_GAUGE_RECHARGE_TIME;
		return Math.min(3, Math.max(result, 0));
	}

	@Override
	public Job getJob() {
		return Job.SGE;
	}

	public Instant replenishedAt() {
		Instant repAt = effectiveTimeNow().plusMillis((long) (JobGaugeConstants.SGE_GAUGE_RECHARGE_TIME * (3 - getAddersGallNow())));
		return repAt;
	}
}
