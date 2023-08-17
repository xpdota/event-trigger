package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

import java.time.Duration;

public class MchGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final double hyperchargeDuration; //[2], [1]
    private final double queenDuration; //[4], [3]
    private final int heatGauge; //[5]
    private final int batteryGauge; //[6]

    public MchGaugeEvent(double hyperchargeDuration, double queenDuration,int heatGauge, int batteryGauge) {
        this.hyperchargeDuration = hyperchargeDuration;
        this.queenDuration = queenDuration;
        this.heatGauge = heatGauge;
        this.batteryGauge = batteryGauge;
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%.0f HC, %.0f Q, %d HG,%d BG", hyperchargeDuration, queenDuration, heatGauge, batteryGauge);
    }

    @Override
    public Job getJob() {
        return Job.MCH;
    }

    public Duration getHyperchargeDuration() {
        return Duration.ofMillis((long)hyperchargeDuration);
    }

    public Duration getQueenDuration() {
        return Duration.ofMillis((long)queenDuration);
    }

    public int getHeatGauge() {
        return heatGauge;
    }

    public int getBatteryGauge() {
        return batteryGauge;
    }
}
