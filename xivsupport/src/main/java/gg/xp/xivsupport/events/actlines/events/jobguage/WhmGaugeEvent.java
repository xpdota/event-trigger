package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

import java.time.Duration;

public class WhmGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final double lilyDuration; //[4], [3]
    private final int lilyCount; //[5]
    private final int bloodLily; //[6]

    public WhmGaugeEvent(double lilyDuration, int lilyCount, int bloodLily) {
        this.lilyDuration = lilyDuration;
        this.lilyCount = lilyCount;
        this.bloodLily = bloodLily;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%.0f LD, %d L, %d BL", lilyDuration, lilyCount, bloodLily);
    }

    @Override
    public Job getJob() {
        return Job.WHM;
    }

    public Duration getLilyDuration() {
        return Duration.ofMillis((long)lilyDuration);
    }

    public int getLilyCount() {
        return lilyCount;
    }

    public int getBloodLily() {
        return bloodLily;
    }
}
