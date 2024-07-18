package gg.xp.xivsupport.triggers.dtex;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class DTEx1SecondTest
		extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/ex1anon2.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(9031, "Start West", "Start West"),
				call(35804, "Raidwide with Bleed", "Raidwide with Bleed (11.0)"),
				call(52870, "Raidwide", "Raidwide (2.7)"),
				call(56794, "East Safe", "East Safe"),
				call(70025, "Front Corners and Partners", "Front Corners and Partners (7.0)"),
				call(80845, "Move Soon", "Move Soon (4.9)"),
				call(83870, "Move!", "Move! (1.9)"),
				call(89130, "Tank Tower and Cleaves", "Tank Tower and Cleaves (3.7)"),
				call(93093, "Center", "Center"),
				call(100221, "West", "West"),
				call(105256, "Center", "Center"),
				call(110288, "West", "West"),
				call(115319, "Center", "Center"),
				call(120357, "West", "West"),
				call(124813, "Light Party Stacks then Eruptions", "Light Party Stacks then Eruptions (8.0)"),
				call(132878, "Move", "Move"),
				call(135954, "Move", "Move"),
				call(139026, "Move", "Move"),
				call(149547, "East Safe", "East Safe"),
				call(160775, "Middle and Partners", "Middle and Partners (7.0)"),
				call(170847, "Spread Soon", "Spread Soon (7.9)"),
				call(183947, "Raidwide", "Raidwide (3.5)"),
				call(191031, "Raidwide", "Raidwide (4.7)"),
				call(199177, "Line Stacks, Behind Tank", "Line Stacks, Behind Tank"),
				call(230584, "Multiple Raidwides", "Multiple Raidwides (9.7)"),
				call(260794, "Raidwide with Bleed", "Raidwide with Bleed (11.0)"),
				call(277900, "Raidwide", "Raidwide (2.7)"),
				call(284849, "Light Party Stacks", "Light Party Stacks (8.0)"),
				call(300142, "Start West, Rotate Clockwise", "Start West, Rotate Clockwise (5.7)"),
				call(312172, "Kill Southwest feather", "Kill Southwest feather"),
				call(332851, "Spread, Up", "Spread, Up (8.0)"),
				call(348488, "Out and Bait Twister", "Out and Bait Twister (6.2)"),
				call(355926, "Move", "Move (2.7)"),
				call(362835, "Row 3 Safe - Go Up", "Row 3 Safe - Go Up (6.4)"),
				call(370542, "Spread", "Spread"),
				call(372859, "Spread, Down", "Spread, Down (8.0)"),
				call(388804, "Middle and Bait Twister", "Middle and Bait Twister (6.2)"),
				call(396241, "Move", "Move (2.7)"),
				call(401856, "Raidwide", "Raidwide (3.5)"),
				call(411970, "Avoid Tower, Knockback", "Avoid Tower, Knockback (5.3)"),
				call(422034, "Raidwide with Bleed", "Raidwide with Bleed (11.0)"),
				call(439085, "Raidwide", "Raidwide (2.7)"),
				call(447013, "Healer Stacks - Avoid Tanks", "Healer Stacks - Avoid Tanks"),
				call(464385, "Cone, Northwest", "Cone, Northwest (7.0)"),
				call(472184, "Northwest safe", "Northwest safe"),
				call(491519, "Start West", "Start West")
		);
	}
}
