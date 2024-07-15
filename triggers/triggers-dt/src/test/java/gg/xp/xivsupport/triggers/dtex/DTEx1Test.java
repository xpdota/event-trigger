package gg.xp.xivsupport.triggers.dtex;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class DTEx1Test extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/ex1anon.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(9051, "Start West", "Start West"),
				call(35846, "Raidwide with Bleed", "Raidwide with Bleed (11.0)"),
				call(52912, "Raidwide", "Raidwide (2.7)"),
				call(56842, "West Safe", "West Safe"),
				callAppend(70077, "Middle and Partners", "(7.0)"),
				call(80849, "Move Soon", "Move Soon (5.0)"),
				call(83925, "Move!", "Move! (1.9)"),
				call(89268, "Tank Tower and Cleaves", "Tank Tower and Cleaves (3.7)"),
				call(93240, "Center", "Center"),
				call(100364, "West", "West"),
				call(105396, "East", "East"),
				call(110437, "West", "West"),
				call(115470, "Center", "Center"),
				call(120501, "West", "West"),
				call(124865, "Light Party Stacks then Eruptions", "Light Party Stacks then Eruptions (8.0)"),
				call(132925, "Move", "Move"),
				call(135994, "Move", "Move"),
				call(139067, "Move", "Move"),
				call(149665, "East Safe", "East Safe"),
				callAppend(160892, "Front Corners and Partners", "(7.0)"),
				call(170872, "Spread Soon", "Spread Soon (8.0)"),
				call(184068, "Raidwide", "Raidwide (3.5)"),
				call(191154, "Raidwide", "Raidwide (4.7)"),
				call(199308, "Line Stacks, Behind Tank", "Line Stacks, Behind Tank"),
				call(242831, "Multiple Raidwides", "Multiple Raidwides (9.7)"),
				call(272994, "Raidwide with Bleed", "Raidwide with Bleed (11.0)"),
				call(290054, "Raidwide", "Raidwide (2.7)"),
				call(297002, "Light Party Stacks", "Light Party Stacks (8.0)"),
				call(312418, "Start East, Rotate Counter-clockwise", "Start East, Rotate Counter-clockwise (5.6)"),
				call(324447, "Kill Southeast feather", "Kill Southeast feather"),
				call(344975, "Spread, Up", "Spread, Up (8.0)"),
				call(360647, "Out and Bait Twister", "Out and Bait Twister (6.2)"),
				call(368085, "Move", "Move (2.7)"),
				call(375036, "Row 1 Safe - Go Up", "Row 1 Safe - Go Up (6.4)"),
				call(382697, "Spread", "Spread"),
				call(385009, "Spread, Down", "Spread, Down (8.0)"),
				call(400995, "Middle and Bait Twister", "Middle and Bait Twister (6.2)"),
				call(408433, "Move", "Move (2.7)"),
				call(414043, "Raidwide", "Raidwide (3.5)"),
				call(424158, "Avoid Tower, Knockback", "Avoid Tower, Knockback (5.3)"),
				call(434225, "Raidwide with Bleed", "Raidwide with Bleed (11.0)"),
				call(451277, "Raidwide", "Raidwide (2.7)"),
				call(459205, "Healer Stacks - Avoid Tanks", "Healer Stacks - Avoid Tanks"),
				call(476574, "Cone, Northwest", "Cone, Northwest (7.0)"),
				call(484464, "Northwest safe", "Northwest safe"),
				call(490260, "Spread Soon", "Spread Soon (7.9)"),
				call(503704, "Start West", "Start West"),
				call(542545, "Out", "Out (6.2)"),
				call(550431, "Northeast safe", "Northeast safe"),
				call(559650, "Move", "Move (4.7)"),
				call(578812, "Raidwide", "Raidwide (3.5)")
		);
	}
}
