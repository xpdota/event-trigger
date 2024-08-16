package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M2sTest
		extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m2s_anon.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(5082, "Raidwide", "Raidwide (4.7)"),
				call(12212, "Spread Later", "Spread Later"),
				call(22774, "In", "In (5.2)"),
				call(28254, "Spread", "Spread"),
				call(34447, "Buddy Later", "Buddy Later"),
				call(41611, "Out of Middle", "Out of Middle (5.2)"),
				call(47088, "Buddy", "Buddy"),
				call(55245, "Tank Cleaves", "Tank Cleaves (3.7)"),
				call(70621, "Raidwide", "Raidwide (9.9)"),
				call(81718, "Out+Cardinals", "Out+Cardinals"),
				call(87877, "Out+Intercards", "Out+Intercards"),
				call(90861, "In+Intercards", "In+Intercards"),
				call(98300, "Take 1 Towers", "Take 1 Towers"),
				call(132537, "Out", "Out (3.7)"),
				call(145687, "Don't Stack", "Don't Stack"),
				call(163099, "In+Intercards", "In+Intercards"),
				call(169291, "Out+Intercards", "Out+Intercards"),
				call(172271, "Out+Cardinals", "Out+Cardinals"),
				call(181717, "Raidwide", "Raidwide (4.7)"),
				call(196957, "Tank Stack", "Tank Stack (3.7)"),
				call(206090, "Bait Lines", "Bait Lines (2.7)"),
				call(245540, "Raidwide", "Raidwide (9.9)"),
				call(256642, "Buddy Later", "Buddy Later"),
				call(272861, "Avoid Stacks", "Avoid Stacks"),
				call(278831, "Spread", "Spread"),
				call(290551, "In", "In (5.2)"),
				call(296033, "Buddy", "Buddy"),
				call(305202, "Raidwide", "Raidwide (4.7)"),
				call(323769, "Buddy Later", "Buddy Later"),
				call(330892, "Bait Lines and Puddles", "Bait Lines and Puddles (2.7)"),
				call(352034, "Drop Puddle", "Drop Puddle"),
				call(363034, "Light Parties", "Light Parties (4.7)"),
				call(371149, "Tank Stack", "Tank Stack (3.7)"),
				call(382902, "Out of Middle", "Out of Middle (5.2)"),
				call(388381, "Buddy", "Buddy"),
				call(404800, "Raidwide", "Raidwide (9.9)"),
				call(413029, "Long Defamation", "Long Defamation (46.0)"),
				call(414940, "Spread Later", "Spread Later"),
				call(422066, "In+Intercards", "In+Intercards"),
				call(428255, "Out+Intercards", "Out+Intercards"),
				call(431235, "Out+Cardinals", "Out+Cardinals"),
				call(434039, "Avoid Defamation", "Avoid Defamation"),
				call(439024, "Soak Tower", "Soak Tower"),
				call(441698, "Out+Cardinals", "Out+Cardinals"),
				call(447886, "Out+Intercards", "Out+Intercards"),
				call(450868, "In+Intercards", "In+Intercards"),
				call(454117, "Out", "Out (4.9)"),
				call(459012, "Avoid Towers", "Avoid Towers"),
				call(466311, "Out of Middle", "Out of Middle (5.2)"),
				call(471786, "Spread", "Spread"),
				call(479933, "Raidwide", "Raidwide (4.7)"),
				call(494144, "Tank Stack", "Tank Stack (3.7)"),
				call(510525, "Raidwide", "Raidwide (4.3)"),
				call(515117, "Group 4 with Kunana Kuna", "Group 4 with Kunana Kuna (62.0)"),
				call(517704, "Group 1: Titijada Nanajada and Lalale Lale", "Group 1: Titijada Nanajada and Lalale Lale (9.4)"),
				call(526720, "Raidwide", "Raidwide (4.7)"),
				call(531899, "Group 2: Lelecho Lecho and Dedefa Defa", "Group 2: Lelecho Lecho and Dedefa Defa (11.2)"),
				call(543832, "Raidwide", "Raidwide (4.7)"),
				call(549099, "Group 3: Fufute Fute and Sichiketi Teketi", "Group 3: Fufute Fute and Sichiketi Teketi (10.0)"),
				call(560976, "Raidwide", "Raidwide (4.7)"),
				call(566197, "Pop with Kunana Kuna", "Pop with Kunana Kuna (10.9)")
		);
	}
}
