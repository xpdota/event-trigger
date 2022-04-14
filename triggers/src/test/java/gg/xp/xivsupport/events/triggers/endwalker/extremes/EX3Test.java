package gg.xp.xivsupport.events.triggers.endwalker.extremes;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class EX3Test extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/ew_ex3.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(6906, "Raidwide", "Raidwide (4.7)"),
				call(18270, "Away from SOUTHWEST", "Away from SOUTHWEST (6.7)"),
				call(25079, "Raidwide", "Raidwide (4.7)"),
				call(36446, "Knockback from NORTHWEST", "Knockback from NORTHWEST (6.7)"),
				call(55377, "Break Tether (with BRD Player)", "Break Tether (with BRD Player)"),
				call(59565, "Middle", "Middle (5.7)"),
				call(69679, "Big Raidwide", "Big Raidwide (4.7)"),
				call(79838, "Tankbuster", "Tankbuster (4.7)"),
				call(92041, "Raidwide", "Raidwide (4.7)"),
				call(103401, "Knockback from SOUTHEAST", "Knockback from SOUTHEAST (6.7)"),
				call(132634, "Knockback from NORTHWEST", "Knockback from NORTHWEST (1.7)"),
				call(139143, "Knockback from SOUTHEAST", "Knockback from SOUTHEAST (1.7)"),
				call(139543, "Middle", "Middle (5.7)"),
				call(199440, "Sides", "Sides (5.7)"),
				call(209592, "Tankbuster", "Tankbuster (4.7)"),
				call(226220, "Spread", "Spread"),
				call(239337, "Flare", "Flare"),
				call(244125, "Middle", "Middle (5.7)"),
				call(259721, "Donut", "Donut"),
				call(348191, "Middle", "Middle (5.7)"),
				call(358297, "Big Raidwide", "Big Raidwide (4.7)"),
				call(368417, "Tankbuster", "Tankbuster (4.7)"),
				call(382774, "Raidwide", "Raidwide (4.7)"),
				call(394137, "Away from NORTHEAST", "Away from NORTHEAST (6.7)"),
				call(421400, "Away from NORTHEAST", "Away from NORTHEAST (1.7)"),
				call(427914, "Away from SOUTHWEST", "Away from SOUTHWEST (1.7)"),
				call(431517, "Knockback from NORTHWEST", "Knockback from NORTHWEST (1.7)"),
				call(438063, "Knockback from SOUTHEAST", "Knockback from SOUTHEAST (1.7)"),
				call(441529, "Sides", "Sides (5.7)"),
				call(525701, "Middle", "Middle (5.7)"),
				call(535799, "Tankbuster", "Tankbuster (4.7)")
		);
	}
}
