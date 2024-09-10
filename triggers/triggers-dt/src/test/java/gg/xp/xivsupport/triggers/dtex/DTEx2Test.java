package gg.xp.xivsupport.triggers.dtex;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class DTEx2Test extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/ex2anon.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(10450, "Raidwide", "Raidwide (4.7)"),
				call(20850, "Cross", "Cross (4.7)"),
				call(29151, "Back Left/Southwest", "Back Left/Southwest (8.7)"),
				call(42579, "Cross", "Cross (4.7)"),
				call(50746, "Tank Tethers", "Tank Tethers (7.7)"),
				call(67146, "Raidwide", "Raidwide (6.7)"),
				call(84321, "Watch Swords", "Watch Swords (3.7)"),
				call(93697, "Swords Mirroring", "Swords Mirroring (4.7)"),
				call(101994, "Right/East", "Right/East (6.8)"),
				call(113012, "Watch Tracks", "Watch Tracks (3.7)"),
				call(129326, "Watch Swords and Tracks", "Watch Swords and Tracks (10.6)"),
				call(140378, "Spread on Tiles", "Spread on Tiles"),
				call(146486, "Raidwide", "Raidwide (4.7)"),
				call(158666, "Avoid Balls, Follow Donuts", "Avoid Balls, Follow Donuts (4.7)"),
				call(183346, "Front Left/Northwest", "Front Left/Northwest (8.8)"),
				call(197386, "Raidwide", "Raidwide (4.7)"),
				call(209600, "Take Stacks Sequentially", "Take Stacks Sequentially (4.7)"),
				call(259834, "Buster on Sojusa Chiqosa", "Buster on Sojusa Chiqosa (4.7)"),
				call(279472, "Raidwide", "Raidwide (6.7)"),
				call(296614, "Knock Buddy Back", "Knock Buddy Back (7.7)"),
				call(310687, "Watch Swords", "Watch Swords (4.7)"),
				call(319879, "Swords Mirroring", "Swords Mirroring (4.7)"),
				call(327810, "Spread on Tiles", "Spread on Tiles"),
				call(340436, "Back Left/North", "Back Left/North (9.8)"),
				call(352433, "Stack - Multiple Hits", "Stack - Multiple Hits (4.6)"),
				call(366198, "Stack", "Stack"),
				call(371183, "Break Chains (with Gamama Gama)", "Break Chains (with Gamama Gama) (15.0)"),
				call(386373, "Raidwide", "Raidwide (4.7)"),
				call(399689, "Avoid Balls, Follow Donuts", "Avoid Balls, Follow Donuts (4.7)"),
				call(419754, "Left/West and Out", "Left/West and Out (6.7)"),
				call(434911, "Tank Tethers", "Tank Tethers (7.7)"),
				call(450126, "Take Stacks Sequentially", "Take Stacks Sequentially (4.7)"),
				call(471892, "Left/West", "Left/West (6.8)"),
				call(481028, "Left/West", "Left/West (6.8)"),
				call(491361, "Left/East", "Left/East (6.8)"),
				call(501439, "Buster on Sojusa Chiqosa", "Buster on Sojusa Chiqosa (4.7)")
		);
	}
}
