package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.triggers.support.MechAssignmentEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.groupmodels.TwoGroupsOfFour;

import java.io.Serial;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ProgramLoopAssignments extends MechAssignmentEvent<TwoGroupsOfFour> {
	@Serial
	private static final long serialVersionUID = 4309367966218905867L;

	public ProgramLoopAssignments(Map<TwoGroupsOfFour, XivPlayerCharacter> groups) {
		super(new EnumMap<>(groups));
	}
}
