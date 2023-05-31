package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.xivsupport.events.triggers.support.MechAssignmentEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.groupmodels.PsMarkerGroup;

import java.io.Serial;
import java.util.Map;

public class PsMarkerAssignments extends MechAssignmentEvent<PsMarkerGroup> {

	@Serial
	private static final long serialVersionUID = 8477519462575557435L;
	private final boolean mid;

	public PsMarkerAssignments(Map<PsMarkerGroup, XivPlayerCharacter> assignments, boolean mid) {
		super(assignments);
		this.mid = mid;
	}

	public boolean isMid() {
		return mid;
	}
}
