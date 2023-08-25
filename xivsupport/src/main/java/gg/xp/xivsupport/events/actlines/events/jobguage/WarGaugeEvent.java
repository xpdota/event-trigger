package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

public class WarGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final int beastGauge;

    public WarGaugeEvent(int beastGauge) {
        this.beastGauge = beastGauge;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%d BG", beastGauge);
    }

    @Override
    public Job getJob() {
        return Job.WAR;
    }

    public int getBeastGauge() {
        return beastGauge;
    }
}
