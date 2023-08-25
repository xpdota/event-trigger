package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

public class GnbGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final int powderGauge;

    public GnbGaugeEvent(int powderGauge) {
        this.powderGauge = powderGauge;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%d PG", powderGauge);
    }

    @Override
    public Job getJob() {
        return Job.GNB;
    }

    public int getPowderGauge() {
        return powderGauge;
    }
}
