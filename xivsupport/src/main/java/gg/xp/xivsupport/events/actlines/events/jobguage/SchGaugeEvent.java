package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

import java.time.Duration;

public class SchGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final int aetherflow; //[1]
    private final int faerieGauge; //[2]
    private final double seraphDuration; //[4], [3]
    private final int unknown5; //[5], 0 normally, 6 when seraph is active

    public SchGaugeEvent(int aetherflow, int faerieGauge, double seraphDuration, int unknown5) {
        this.aetherflow = aetherflow;
        this.faerieGauge = faerieGauge;
        this.seraphDuration = seraphDuration;
        this.unknown5 = unknown5;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%d AF, %d FG, %.0f SD, %d u5", aetherflow, faerieGauge, seraphDuration, unknown5);
    }

    @Override
    public Job getJob() {
        return Job.SCH;
    }

    public int getAetherflow() {
        return aetherflow;
    }

    public int getFaerieGauge() {
        return faerieGauge;
    }

    public Duration getSeraphDuration() {
        return Duration.ofMillis((long)seraphDuration);
    }

    public int getUnknown5() {
        return unknown5;
    }
}
