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
				call(18270, "NORTHEAST safe", "NORTHEAST safe (6.7)"),
				call(25079, "Raidwide", "Raidwide (4.7)"),
				call(36446, "Knockback from NORTHWEST", "Knockback from NORTHWEST (6.7)"),
				call(55377, "Break Tether (with BRD Player)", "Break Tether (with BRD Player)"),
				call(59565, "Middle", "Middle (5.7)"),
				call(69679, "Big Raidwide", "Big Raidwide (4.7)"),
				call(79838, "Tankbuster", "Tankbuster (4.7)"),
				call(92041, "Raidwide", "Raidwide (4.7)"),
				call(103401, "Knockback from SOUTHEAST", "Knockback from SOUTHEAST (6.7)"),
				call(109194, "Healer Stacks", "Healer Stacks (4.7)"),
				call(127461, "NORTHWEST then SOUTHEAST", "NORTHWEST, then SOUTHEAST (6.5)"),
				call(139543, "Middle", "Middle (5.7)"),
				call(160876, "SOUTHEAST, SOUTHWEST, NORTHWEST"),
				call(189251, "SOUTHEAST"),
				call(199440, "Sides", "Sides (5.7)"),
				call(209592, "Tankbuster", "Tankbuster (4.7)"),
				call(226220, "Spread", "Spread (6.0)"),
				call(239337, "Flare", "Flare (6.0)"),
				call(244125, "Middle", "Middle (5.7)"),
				call(259721, "Donut", "Donut (6.0)"),
				call(269398, "Flare", "Flare (12.0)"),
				call(348191, "Middle", "Middle (5.7)"),
				call(358297, "Big Raidwide", "Big Raidwide (4.7)"),
				call(368417, "Tankbuster", "Tankbuster (4.7)"),
				call(382774, "Raidwide", "Raidwide (4.7)"),
				call(394137, "SOUTHWEST safe", "SOUTHWEST safe (6.7)"),
				call(399942, "Healer Stacks", "Healer Stacks (4.7)"),
				call(416234, "SOUTHWEST then NORTHEAST", "SOUTHWEST, then NORTHEAST (6.5)"),
				call(426358, "NORTHWEST then SOUTHEAST", "NORTHWEST, then SOUTHEAST (6.5)"),
				call(441529, "Sides", "Sides (5.7)"),
				call(487061, "NORTHWEST, NORTHEAST, SOUTHEAST"),
				call(515479, "NORTHWEST"),
				call(525701, "Middle", "Middle (5.7)"),
				call(535799, "Tankbuster", "Tankbuster (4.7)")
		);
	}
}
