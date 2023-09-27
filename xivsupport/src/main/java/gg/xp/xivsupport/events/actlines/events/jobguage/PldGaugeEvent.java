package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

public class PldGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final int oathGauge;

    public PldGaugeEvent(int oathGauge) {
        this.oathGauge = oathGauge;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%d OG", oathGauge);
    }

    @Override
    public Job getJob() {
        return Job.PLD;
    }

    public int getOathGauge() {
        return oathGauge;
    }
}
