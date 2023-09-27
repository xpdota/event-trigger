package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

import java.time.Duration;

public class RprGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final int soulGauge; //[1]
    private final int shroudGauge; //[2]
    private final double enshroudDuration; //[4], [3]
    private final int blueShroudOrbs; //[5], The ones that let you use the GCDs
    private final int pinkShroudOrbs; //[6], The ones that let you use the oGCDs

    public RprGaugeEvent(int soulGauge, int shroudGauge, double enshroudDuration, int blueShroudOrbs, int pinkShroudOrbs) {
        this.soulGauge = soulGauge;
        this.shroudGauge = shroudGauge;
        this.enshroudDuration = enshroudDuration;
        this.blueShroudOrbs = blueShroudOrbs;
        this.pinkShroudOrbs = pinkShroudOrbs;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%d SG, %d ShG, %.0f ED, %d BO, %d PO", soulGauge, shroudGauge, enshroudDuration, blueShroudOrbs, pinkShroudOrbs);
    }

    @Override
    public Job getJob() {
        return Job.RPR;
    }

    public int getSoulGauge() {
        return soulGauge;
    }

    public int getShroudGauge() {
        return shroudGauge;
    }

    public Duration getEnshroudDuration() {
        return Duration.ofMillis((long)enshroudDuration);
    }

    public int getBlueShroudOrbs() {
        return blueShroudOrbs;
    }

    public int getPinkShroudOrbs() {
        return pinkShroudOrbs;
    }
}
