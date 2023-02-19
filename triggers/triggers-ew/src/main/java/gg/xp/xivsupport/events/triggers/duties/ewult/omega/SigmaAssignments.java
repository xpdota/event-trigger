package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.xivsupport.events.triggers.support.MechAssignmentEvent;
import gg.xp.xivsupport.models.XivPlayerCharacter;

import java.io.Serial;
import java.util.Map;

public class SigmaAssignments extends MechAssignmentEvent<DynamisSigmaAssignment> {
	@Serial
	private static final long serialVersionUID = -8483399990587837244L;

	public SigmaAssignments(Map<DynamisSigmaAssignment, XivPlayerCharacter> assignments) {
		super(assignments);
	}
}
