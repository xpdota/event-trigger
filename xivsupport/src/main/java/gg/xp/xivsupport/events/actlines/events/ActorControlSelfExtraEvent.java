package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

/**
 * Represents various types of actor control events, e.g. wipes, barrier up/down, etc
 */
@SystemEvent
public class ActorControlSelfExtraEvent extends BaseEvent implements HasPrimaryValue, HasTargetEntity {

	@Serial
	private static final long serialVersionUID = 8669401183656172010L;
	private final XivCombatant target;
	private final int category;
	private final long data0;
	private final long data1;
	private final long data2;
	private final long data3;
	private final long data4;
	private final long data5;

	public ActorControlSelfExtraEvent(XivCombatant target, int category, long data0, long data1, long data2, long data3, long data4, long data5) {
		this.target = target;
		this.category = category;
		this.data0 = data0;
		this.data1 = data1;
		this.data2 = data2;
		this.data3 = data3;
		this.data4 = data4;
		this.data5 = data5;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%X %X:%X:%X:%X:%X:%X", category, data0, data1, data2, data3, data4, data5);
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	public int getCategory() {
		return category;
	}

	public long getData0() {
		return data0;
	}

	public long getData1() {
		return data1;
	}

	public long getData2() {
		return data2;
	}

	public long getData3() {
		return data3;
	}

	public long getData4() {
		return data4;
	}

	public long getData5() {
		return data5;
	}
}
