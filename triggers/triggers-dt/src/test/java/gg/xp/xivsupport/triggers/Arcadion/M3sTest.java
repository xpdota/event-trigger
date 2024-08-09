package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M3sTest
		extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m3s_anon.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(6003, "Raidwide - Multi Hit", "Raidwide - Multi Hit (4.7)"),
				call(15360, "Tank Buster - Multi Hit", "Tank Buster - Multi Hit (4.7)"),
				call(30416, "Out and Spread", "Out and Spread (6.7)"),
				call(40302, "Out into Role Pairs", "Out into Role Pairs (7.7)"),
				call(54781, "Raidwide - Multi Hit", "Raidwide - Multi Hit (4.7)"),
				call(72211, "Knockback Towers", "Knockback Towers (3.7)"),
				call(76189, "Side Towers to Corners", "Side Towers to Corners"),
				call(87318, "Corner Towers to Center", "Corner Towers to Center"),
				call(90568, "Center Tower to Northeast", "Center Tower to Northeast"),
				call(136451, "In and Partners", "In and Partners (6.7)"),
				call(146341, "Knockback into Spreads", "Knockback into Spreads (7.7)"),
				call(160777, "Raidwide - Multi Hit", "Raidwide - Multi Hit (4.7)"),
				call(172276, "Tank Buster - Multi Hit", "Tank Buster - Multi Hit (4.7)"),
				call(207832, "Northwest safe", "Northwest safe"),
				call(212690, "Southwest safe", "Southwest safe"),
				call(219465, "In and Spread", "In and Spread (6.7)"),
				call(228818, "Raidwide - Multi Hit", "Raidwide - Multi Hit (4.7)"),
				call(254748, "Northeast Safe, Long Fuse", "Northeast Safe, Long Fuse"),
				call(265614, "Spread", "Spread"),
				call(270653, "Knockback into Role Pairs", "Knockback into Role Pairs (7.7)"),
				call(293729, "Pop Fuses Sequentially", "Pop Fuses Sequentially (3.7)"),
				call(298634, "Short Fuse", "Short Fuse (26.0)"),
				call(349244, "Tank Buster - Multi Hit", "Tank Buster - Multi Hit (4.7)"),
				call(384756, "Multiple Raidwides", "Multiple Raidwides (5.7)"),
				call(397274, "Out", "Out (3.0)"),
				call(400524, "In", "In (2.2)"),
				call(403027, "Knockback into Buddies", "Knockback into Buddies (7.9)"),
				call(411212, "Buddies", "Buddies"),
				call(428675, "Northeast safe", "Northeast safe"),
				call(445693, "Move", "Move"),
				// TODO Also, this should call out boss direction
				call(449657, "Northwest safe, Get Hit By Boss", "Northwest safe, Get Hit By Boss"),
				call(456520, "Southwest safe", "Southwest safe"),
				call(463333, "Out into Spreads", "Out into Spreads (7.7)"),
				call(478886, "Raidwide - Multi Hit", "Raidwide - Multi Hit (4.7)"),
				call(492520, "Tank Buster - Multi Hit", "Tank Buster - Multi Hit (4.7)"),
				call(512842, "Spinning", "Spinning (3.7)"),
				call(516808, "Short Fuse", "Short Fuse"),
				call(520371, "Counter-Clockwise", "Counter-Clockwise (5.2)"),
				call(534228, "Spread", "Spread"),
				call(539304, "Spread", "Spread"),
				call(541039, "Out and Partners", "Out and Partners (6.7)"),
				call(550397, "Raidwide - Multi Hit", "Raidwide - Multi Hit (4.7)"),
				call(571114, "Knockback Towers", "Knockback Towers (3.7)"),
				call(575085, "Side Towers to Corners", "Side Towers to Corners"),
				call(586220, "Corner Towers to Center", "Corner Towers to Center"),
				call(589604, "Center Tower to West", "Center Tower to West"),
				call(594641, "East Safe", "East Safe"),
				call(610060, "Tank Buster - Multi Hit", "Tank Buster - Multi Hit (4.7)"),
				call(651098, "Multiple Raidwides", "Multiple Raidwides (5.7)")
		);
	}
}
