package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M1sTest
		extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m1s_anon.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(8270, "Quadruple Crossing", "Quadruple Crossing (4.7)"),
				call(14229, "Don't Bait - Out", "Don't Bait - Out"),
				call(16988, "Dodge 1", "Dodge 1"),
				call(19833, "Dodge 2", "Dodge 2"),
				call(27300, "Tankbuster on Liliwure Ririwure", "Tankbuster on Liliwure Ririwure (4.7)"),
				call(51444, "Left then Right", "Left then Right (5.7)"),
				call(57449, "Right", "Right"),
				call(61808, "Start Northwest-North", "Start Northwest-North (19.7)"),
				call(81869, "Move", "Move"),
				call(82894, "Partners", "Partners (3.7)"),
				call(94990, "Quadruple Crossing - Leaping Left", "Quadruple Crossing - Leaping Left (4.7)"),
				call(101970, "Don't Bait - Out", "Don't Bait - Out"),
				call(104682, "Dodge 1", "Dodge 1"),
				call(106370, "Partners", "Partners (3.7)"),
				call(107527, "Dodge 2", "Dodge 2"),
				call(116951, "Raidwide", "Raidwide (4.7)"),
				// TODO: watch positions for this and call out safe spot?
				call(170497, "Kick on Pupuqeto Fufuqeto - Knock", "Kick on Pupuqeto Fufuqeto - Knock (5.3)"),
				call(181677, "Punch on Liliwure Ririwure", "Punch on Liliwure Ririwure (5.3)"),
				call(192824, "Kick on YOU - Knock", "Kick on YOU - Knock (5.3)"),
				call(203971, "Punch on Boboze Boze", "Punch on Boboze Boze (5.3)"),
				call(218273, "Tankbuster on Boboze Boze", "Tankbuster on Boboze Boze (4.7)"),
				call(229305, "Knockback into Spread", "Knockback into Spread (6.7)"),
				call(236293, "Spread", "Spread (3.5)"),
				call(257790, "Right then Left (West)", "Right then Left (West) (6.4)"),
				call(264523, "Left", "Left"),
				call(279083, "Quadruple Crossing - Leaping Right", "Quadruple Crossing - Leaping Right (4.7)"),
				call(286073, "Don't Bait - Out", "Don't Bait - Out"),
				call(288791, "Dodge 1", "Dodge 1"),
				call(291642, "Dodge 2", "Dodge 2"),
				call(318257, "Party Stacks - West, In First", "Party Stacks - West, In First (4.7)"),
				// TODO: this is a tiny fraction of a second too early
				call(324990, "Out", "Out"),
				call(342432, "Bait Protean", "Bait Protean (7.7)"),
				call(351045, "Out", "Out (7.7)"),
				call(353191, "Dodge 1", "Dodge 1"),
				call(356042, "Dodge 2", "Dodge 2"),
				call(365501, "Raidwide", "Raidwide (4.7)"),
				call(419134, "Kick on Jujuqaju Guguqaju - Knock", "Kick on Jujuqaju Guguqaju - Knock (5.3)"),
				call(423195, "Role Groups", "Role Groups (4.7)"),
				call(431367, "Kick on Fifilu Filu - Knock", "Kick on Fifilu Filu - Knock (5.3)"),
				call(435604, "Stack", "Stack (4.7)"),
				call(443987, "Punch on Niqo Moqo", "Punch on Niqo Moqo (5.3)"),
				call(448129, "Stack", "Stack (4.7)"),
				call(456504, "Punch on Rorasu Qorasu", "Punch on Rorasu Qorasu (5.3)"),
				call(460560, "Role Groups", "Role Groups (4.7)"),
				call(470678, "Tankbuster on Boboze Boze", "Tankbuster on Boboze Boze (4.7)"),
				call(482756, "Knockback into Spread", "Knockback into Spread (6.7)"),
				call(489754, "Spread", "Spread (3.5)"),
				call(507274, "Tethers and Stacks", "Tethers and Stacks (5.7)"),
				call(514270, "Tethers and Stacks", "Tethers and Stacks (4.7)"),
				call(520289, "Tethers and Stacks", "Tethers and Stacks (4.7)"),
				call(548174, "Watch Pounces", "Watch Pounces (12.7)"),
				call(561392, "Left then Right", "Left then Right (5.7)"),
				call(567869, "Right", "Right")
		);
	}
}
