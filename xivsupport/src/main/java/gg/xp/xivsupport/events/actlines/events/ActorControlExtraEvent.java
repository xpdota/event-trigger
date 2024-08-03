package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

/**
 * Represents entity-specific actor control extra data
 */
//@SystemEvent
public class ActorControlExtraEvent extends BaseEvent implements HasPrimaryValue, HasTargetEntity {


	@Serial
	private static final long serialVersionUID = -877415425842079649L;
	private final XivCombatant target;
	private final int category;
	private final long data0;
	private final long data1;
	private final long data2;
	private final long data3;

	public ActorControlExtraEvent(XivCombatant target, int category, long data0, long data1, long data2, long data3) {
		this.target = target;
		this.category = category;
		this.data0 = data0;
		this.data1 = data1;
		this.data2 = data2;
		this.data3 = data3;
	}

	@Override
	public String getPrimaryValue() {
		return String.format("%X %X:%X:%X:%X", category, data0, data1, data2, data3);
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
}
