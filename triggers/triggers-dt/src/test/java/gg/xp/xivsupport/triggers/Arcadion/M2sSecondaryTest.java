package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M2sSecondaryTest
		extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m2s_anon_2.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(6364, "Raidwide", "Raidwide (4.7)"),
				call(18468, "Buddy Later", "Buddy Later"),
				call(23814, "In", "In (5.2)"),
				call(29286, "Buddy", "Buddy"),
				call(40456, "Spread Later", "Spread Later"),
				call(42591, "Out of Middle", "Out of Middle (5.2)"),
				call(48070, "Spread", "Spread"),
				call(56223, "Tank Stack", "Tank Stack (3.7)"),
				call(71642, "Raidwide", "Raidwide (9.9)"),
				call(82748, "Out+Cardinals", "Out+Cardinals"),
				call(88933, "Out+Intercards", "Out+Intercards"),
				call(91908, "In+Intercards", "In+Intercards"),
				call(99369, "Avoid Towers", "Avoid Towers"),
				call(133656, "Out", "Out (3.7)"),
				call(146818, "Take Stack", "Take Stack"),
				call(164053, "In+Intercards", "In+Intercards"),
				call(170223, "Out+Intercards", "Out+Intercards"),
				call(173220, "Out+Cardinals", "Out+Cardinals"),
				call(182686, "Raidwide", "Raidwide (4.7)"),
				call(197888, "Tank Cleaves", "Tank Cleaves (3.7)"),
				call(207043, "Bait Lines", "Bait Lines (2.7)"),
				call(246465, "Raidwide", "Raidwide (9.9)"),
				call(262597, "Buddy Later", "Buddy Later"),
				call(273850, "Avoid Stacks", "Avoid Stacks"),
				call(279830, "Spread", "Spread"),
				call(291534, "Out of Middle", "Out of Middle (5.2)"),
				call(297027, "Buddy", "Buddy"),
				call(306221, "Raidwide", "Raidwide (4.7)"),
				call(329537, "Buddy Later", "Buddy Later"),
				call(331681, "Bait Lines and Puddles", "Bait Lines and Puddles (2.7)"),
				call(337848, "Drop Puddle", "Drop Puddle"),
				call(363830, "Light Parties", "Light Parties (4.7)"),
				call(371949, "Tank Cleaves", "Tank Cleaves (3.7)"),
				call(383688, "Out of Middle", "Out of Middle (5.2)"),
				call(389171, "Buddy", "Buddy"),
				call(405586, "Raidwide", "Raidwide (9.9)"),
				call(413813, "Long Defamation", "Long Defamation (46.0)"),
				call(420697, "Spread Later", "Spread Later"),
				call(422840, "In+Intercards", "In+Intercards"),
				call(429003, "Out+Intercards", "Out+Intercards"),
				call(431998, "Out+Cardinals", "Out+Cardinals"),
				call(434861, "Avoid Defamation", "Avoid Defamation"),
				call(439782, "Soak Tower", "Soak Tower"),
				call(442463, "Out+Cardinals", "Out+Cardinals"),
				call(448667, "Out+Intercards", "Out+Intercards"),
				call(451659, "In+Intercards", "In+Intercards"),
				call(454911, "Out", "Out (4.9)"),
				call(459778, "Avoid Towers", "Avoid Towers"),
				call(467104, "Out of Middle", "Out of Middle (5.2)"),
				call(472561, "Spread", "Spread"),
				call(480727, "Raidwide", "Raidwide (4.7)"),
				call(494938, "Tank Cleaves", "Tank Cleaves (3.7)"),
				call(511552, "Raidwide", "Raidwide (4.3)"),
				call(516151, "Group 3 with Bime Dumame", "Group 3 with Bime Dumame (44.0)"),
				call(518653, "Group 1: Fofopu Fopu and Wunono Wuno", "Group 1: Fofopu Fopu and Wunono Wuno (9.5)"),
				call(527683, "Raidwide", "Raidwide (4.7)"),
				call(532916, "Group 2: Dadaboqu Sasaboqu and Gaganeki Bubuneki", "Group 2: Dadaboqu Sasaboqu and Gaganeki Bubuneki (11.2)"),
				call(544801, "Raidwide", "Raidwide (4.7)"),
				call(549988, "Pop with Bime Dumame", "Pop with Bime Dumame (10.2)"),
				call(561910, "Raidwide", "Raidwide (4.7)"),
				call(567095, "Group 4: Zozolalu Titilalu and Wuwuhuga Zizihuga", "Group 4: Zozolalu Titilalu and Wuwuhuga Zizihuga (11.1)"),
				call(579030, "Raidwide", "Raidwide (4.7)"),
				call(596342, "Enrage", "Enrage (9.7)")
		);
	}
}
