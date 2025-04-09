package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M6sTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m6s_anon.log";
	}

//	@Override
//	protected long minimumMsBetweenCalls() {
//		// it's the nisi call between Let's Dance cleaves
//		return 750;
//	}


	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(5583, "Raidwide", "Raidwide (4.7)"),
				call(16758, "Double Tankbuster", "Double Tankbuster (4.7)"),
				call(37453, "Light Parties Stocked", "Light Parties Stocked"),
				call(47660, "KB from Northwest", "KB from Northwest (8.8)"),
				call(56282, "Light Parties", "Light Parties"),
				call(63864, "Spread", "Spread (4.7)"),
				call(69638, "Mousse on YOU, Chojaja Choja", "Mousse on YOU, Chojaja Choja"),
				call(75996, "Double Tankbuster", "Double Tankbuster (4.7)"),
				call(97555, "Later: Stack on Chojaja Choja", "Later: Stack on Chojaja Choja (43.0)"),
				call(132559, "Stack on Chojaja Choja", "Stack on Chojaja Choja (8.0)"),
				call(143363, "Spread", "Spread (4.7)"),
				call(149144, "Mousse on YOU, Mumupu Mupu", "Mousse on YOU, Mumupu Mupu"),
				call(163684, "Avoid Fire on Mamahu Mahu, Mumupu Mupu", "Avoid Fire on Mamahu Mahu, Mumupu Mupu (7.9)"),
				call(183289, "Place Bomb in Bad", "Place Bomb in Bad"),
				call(193505, "Raidwide", "Raidwide (4.7)"),
				call(201633, "Double Tankbuster", "Double Tankbuster (4.7)"),
				call(213856, "Adds", "Adds"),
				call(225025, "Wave 1", "Wave 1"),
				call(254189, "Wave 2", "Wave 2"),
				call(276359, "Wave 3", "Wave 3"),
				call(298446, "Raidwide", "Raidwide (6.7)"),
				call(315619, "Wave 4", "Wave 4"),
				call(381725, "Raidwide", "Raidwide (6.7)"),
				call(391897, "Avoid Lines", "Avoid Lines"),
				call(406033, "Double Tankbuster", "Double Tankbuster (4.7)"),
				call(414176, "Raidwide", "Raidwide (4.7)"),
				call(442748, "Spread on Land, Avoid Lines", "Spread on Land, Avoid Lines"),
				call(464206, "Move", "Move (2.7)"),
				call(491579, "Spread", "Spread"),
				call(512251, "Multi Stack", "Multi Stack (3.7)"),
				call(554835, "Wait Outside Tower", "Wait Outside Tower"),
				call(561063, "Move into Tower", "Move into Tower (2.7)"),
				call(569109, "Fly to Platform", "Fly to Platform"),
				call(583444, "Wait Outside Tower", "Wait Outside Tower"),
				call(587356, "Move into Tower", "Move into Tower (2.7)"),
				call(592331, "Raidwide", "Raidwide (4.7)"),
				call(602474, "Spread", "Spread (4.7)"),
				call(608255, "Mousse on Chojaja Choja, Mamahu Mahu", "Mousse on Chojaja Choja, Mamahu Mahu"),
				call(614613, "Double Tankbuster", "Double Tankbuster (4.7)"),
				call(635332, "Partners Stocked", "Partners Stocked"),
				call(645956, "KB from Northeast", "KB from Northeast (8.5)"),
				call(654182, "Partners", "Partners"),
				call(668824, "Enrage", "Enrage (7.7)")
		);
	}
}
