package gg.xp.xivsupport.events.actlines.events.jobguage;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.JobGaugeUpdate;

public class AstGaugeEvent extends BaseEvent implements HasPrimaryValue, JobGaugeUpdate {

    private final int cardHeld; //[6], Balance 1, Bole 2, Arrow 3, Spear 4, Ewer 5, Spire 6, Lord + 0x70, Lady + 0x80
    private final int minorHeld;
    private final int slot1; //[7], Sun 1, Moon 2, Star 3
    private final int slot2;
    private final int slot3;

    public AstGaugeEvent(int cardHeld, int minorHeld, int slot1, int slot2, int slot3) {
        this.cardHeld = cardHeld;
        this.minorHeld = minorHeld;
        this.slot1 = slot1;
        this.slot2 = slot2;
        this.slot3 = slot3;
    }

    public static AstGaugeEvent fromRaw(byte[] data) {
        int cardHeld = data[6] & 0xf;
        int minorHeld = (data[6] >> 8) & 0xf;
        int slot1 = data[7] & 3;
        int slot2 = (data[7] >> 2) & 3;
        int slot3 = (data[7] >> 4) & 3;

        return new AstGaugeEvent(cardHeld, minorHeld, slot1, slot2, slot3);
    }

    @Override
    public String getPrimaryValue() {
        return String.format("%d CH, %d MH, %d S1, %d S2, %d S3", cardHeld, minorHeld, slot1, slot2, slot3);
    }

    @Override
    public Job getJob() {
        return Job.AST;
    }

    public int getCardHeld() {
        return cardHeld;
    }

    public int getMinorHeld() {
        return minorHeld;
    }

    public int getSlot1() {
        return slot1;
    }

    public int getSlot2() {
        return slot2;
    }

    public int getSlot3() {
        return slot3;
    }
}
