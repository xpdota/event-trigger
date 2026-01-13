package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

import java.time.Duration;

public class DrkGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

	//TODO: serial id
	private final int bloodGauge; //[1]
	private final double darkSide; //[4], [3]
	private final double esteemDuration; //[8], [7]

	public DrkGaugeEvent(int bloodGauge, double darkSide, double esteemDuration) {
		this.bloodGauge = bloodGauge;
		this.darkSide = darkSide;
		this.esteemDuration = esteemDuration;
	}

	public static DrkGaugeEvent fromRaw(byte[] data) {
		int bloodGauge = data[1];
		long darkSideDuration = JobGaugeHandlers.bytesToLong(data[4], data[3]);
		long esteemDuration = JobGaugeHandlers.bytesToLong(data[8], data[7]);

		return new DrkGaugeEvent(bloodGauge, darkSideDuration, esteemDuration);
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%d BG, %.0f DS, %.0f est", bloodGauge, darkSide,esteemDuration);
	}

	@Override
	public Job getJob() {
		return Job.DRK;
	}

	public int getBloodGauge() {
		return bloodGauge;
	}

	public Duration getDarkSideDuration() {
		return Duration.ofMillis((long)darkSide);
	}

	public Duration getEsteemDuration() {
		return Duration.ofMillis((long)esteemDuration);
	}
}
