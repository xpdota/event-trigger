package gg.xp.xivsupport.triggers.ultimate;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class FRUTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/fru_anon.log";
	}

	@Override
	protected long minimumMsBetweenCalls() {
		return 250;
	}

	// TODO: turn this into "configure a call" method and specialized "disable call" etc methods
//	@Override
//	protected void configure(MutablePicoContainer pico) {
//		// Disable this specific callout since it will fail the timing check (and is unnecessary anyway)
//		ModifiedCalloutRepository mcr = pico.getComponent(ModifiedCalloutRepository.class);
//		CalloutGroup group = mcr.getAllCallouts().stream().filter(grp -> grp.getCallClass().equals(FRU.class))
//				.findFirst().orElseThrow(() -> new RuntimeException("Did not find FRU callouts"));
//		ModifiedCalloutHandle ft4 = group.getCallouts().stream().filter(call -> call.getField().getName().equals("fourTetherColl4"))
//				.findFirst().orElseThrow(() -> new RuntimeException("Did not find fourTetherColl4 callout"));
//		ft4.getEnable().set(false);
//	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(6927, "Proteans and Spread", "Proteans and Spread (6.2)"),
				call(14021, "Move, Spread", "Move, Spread"),
				call(16104, "Move", "Move"),
				call(18158, "Move", "Move"),
				call(18913, "Buster on Chachajire Titijire", "Buster on Chachajire Titijire (4.7)"),
				call(23906, "Powder Mark on Chachajire Titijire", "Powder Mark on Chachajire Titijire (16.0)"),
				call(30567, "Stack Later", "Stack Later"),
				call(37314, "Powder Mark on Chachajire Titijire", "Powder Mark on Chachajire Titijire (2.6)"),
				call(39728, "Stack Northeast, Southwest", "Stack Northeast, Southwest (9.7)"),
				call(49778, "Proteans and Buddies", "Proteans and Buddies (5.3)"),
				call(55953, "Move, Buddies", "Move, Buddies"),
				call(56355, "Red Safe", "Red Safe"),
				call(58004, "Move", "Move"),
				call(60056, "Move, North/South Out", "Move, North/South Out (4.0)"),
				call(64394, "Get Knocked West", "Get Knocked West (5.7)"),
				call(70339, "Move In", "Move In"),
				call(80976, "Raidwide with Bleed", "Raidwide with Bleed (4.7)"),
				call(92502, "Fire on Sechobetu Jibetu", "Fire on Sechobetu Jibetu"),
				call(95627, "", "Fire on Sechobetu Jibetu, Fire on YOU"),
				call(98172, "", "Fire on Sechobetu Jibetu, Fire on YOU, Fire on Zezerobi Sisirobi"),
				call(100672, "", "Fire on Sechobetu Jibetu, Fire on YOU, Fire on Zezerobi Sisirobi, Lightning on Harone Sudune"),
				call(101697, "Fire on Sechobetu Jibetu", "Fire on Sechobetu Jibetu, Fire on YOU, Fire on Zezerobi Sisirobi, Lightning on Harone Sudune"),
				call(103219, "Fire on YOU", "Fire on YOU, Fire on Zezerobi Sisirobi, Lightning on Harone Sudune"),
				call(106255, "Fire on Zezerobi Sisirobi", "Fire on Zezerobi Sisirobi, Lightning on Harone Sudune"),
				call(108805, "Lightning on Harone Sudune", "Lightning on Harone Sudune"),
				call(116315, "Raidwide with Bleed", "Raidwide with Bleed (4.7)"),
				call(124804, "Buster on Zezerobi Sisirobi", "Buster on Zezerobi Sisirobi (4.7)"),
				call(129799, "Powder Mark on Zezerobi Sisirobi", "Powder Mark on Zezerobi Sisirobi (16.0)"),
				call(136086, "Soak Tower", "Soak Tower (9.2)"),
				call(142840, "Powder Mark on Zezerobi Sisirobi", "Powder Mark on Zezerobi Sisirobi (3.0)"),
				call(150704, "Enrage", "Enrage (9.7)"),
				call(166065, "Buster on Zezerobi Sisirobi", "Buster on Zezerobi Sisirobi (4.7)"),
				call(186836, "Raidwide", "Raidwide (4.7)"),
				call(195289, "Out with Marker, West, East Safe", "Out with Marker, West, East Safe (5.2)"),
				call(200717, "Stay, Drop Puddle", "Stay, Drop Puddle"),
				call(203033, "In, Knockback to West, East", "In, Knockback to West, East"),
				call(210109, "Multiple Stacks, Keep Moving", "Multiple Stacks, Keep Moving"),
				call(214285, "Look Away from West", "Look Away from West"),
				call(222863, "Back to Front", "Back to Front (3.2)"),
				call(226280, "Front", "Front"),
				call(234225, "Line Stack", "Line Stack (4.7)"),
				call(256982, "Blue Mirror and Boss, In+Proteans", "Blue Mirror and Boss, In+Proteans (5.7)"),
				call(263029, "Red Mirrors, In+Proteans", "Red Mirrors, In+Proteans (9.7)"),
				call(274140, "Spread", "Spread (4.7)"),
				call(283518, "Raidwide, Light Rampant Positions", "Raidwide, Light Rampant Positions (4.7)"),
				call(289475, "Chain and Stack", "Chain and Stack"),
				call(299695, "Stacks", "Stacks"),
				call(307824, "Avoid Tower", "Avoid Tower"),
				call(312577, "Buddies", "Buddies (4.7)"),
				call(320715, "Proteans", "Proteans (4.7)"),
				call(334989, "Enrage, Knockback", "Enrage, Knockback (9.7)"),
				call(371856, "Kill Crystals, Bait AoEs", "Kill Crystals, Bait AoEs (39.7)"),
				call(437660, "1 HP", "1 HP (3.7)"),
				call(445922, "Raidwide", "Raidwide (9.7)"),
				call(457085, "Long Fire", "Long Fire (30.6)"),
				call(460200, "East is North", "East is North"),
				call(462111, "Stack", "Stack (5.6)"),
				call(468120, "Bait Spinny", "Bait Spinny"),
				call(473149, "Stack", "Stack (4.6)"),
				call(478175, "Stand Middle", "Stand Middle"),
				call(483195, "Move Out", "Move Out (4.5)"),
				call(488217, "Look Outside", "Look Outside"),
				call(500670, "Stack", "Stack (2.7)"),
				call(507080, "Raidwide", "Raidwide (4.7)"),
				call(515215, "Tankbuster on Chachajire Titijire", "Tankbuster on Chachajire Titijire (4.7)"),
				call(538265, "Check Timers", "Check Timers"),
				call(543296, "Stacks", "Stacks (5.0)"),
				call(548338, "Spread", "Spread"),
				call(550253, "Spread Northeast, Southwest", "Spread Northeast, Southwest"),
				call(561508, "Move in for Stacks", "Move in for Stacks (5.8)"),
				call(563689, "Stacks then Tank Bait East, West", "Stacks then Tank Bait East, West (3.6)"),
				call(567335, "Tank Bait East, West", "Tank Bait East, West"),
				call(569116, "Knockback into Stacks", "Knockback into Stacks (7.1)"),
				call(576597, "Raidwide", "Raidwide (4.7)"),
				call(585287, "Enrage", "Enrage (9.7)"),
				call(617494, "Stack", "Stack"),
				call(623645, "Dodge", "Dodge (2.2)"),
				call(626142, "Raidwide", "Raidwide (2.2)"),
				call(633307, "Raidwide", "Raidwide (4.7)"),
				call(639140, "Nothing", "Nothing"),
				call(641988, "Bait Cleave", "Bait Cleave (7.2)"),
				call(650314, "Spread", "Spread"),
				call(652541, "Stacks", "Stacks (4.7)"),
				call(657518, "Tank Baits", "Tank Baits"),
				call(662628, "Raidwide", "Raidwide (4.7)"),
				call(670006, "Stacks", "Stacks (3.7)"),
				call(678188, "Stack", "Stack (5.7)"),
				call(689739, "Raidwide", "Raidwide (9.7)"),
				call(700540, "Ice and Blue", "Ice and Blue (14.0)"),
				call(703827, "", ""),
				call(710179, "Move, Get Pushed Northeast", "Move, Get Pushed Northeast (4.4)"),
				call(714623, "Stack", "Stack (2.9)"),
				call(717600, "Dodge and Cleanse", "Dodge and Cleanse"),
				call(724445, "Drop Rewind Northwest", "Drop Rewind Northwest (9.1)"),
				call(728084, "Raidwide", "Raidwide (3.5)"),
				call(733912, "Cleanse and Spread", "Cleanse and Spread (2.7)"),
				call(752512, "Stacks", "Stacks (3.7)"),
				call(760694, "Stack", "Stack (5.7)"),
				call(769856, "Enrage", "Enrage (9.7)"),
				call(855638, "Raidwide", "Raidwide (5.7)"),
				call(865692, "Fulgent Blade: CCW", "Fulgent Blade: CCW (6.7)"),
				call(872685, "Move", "Move"),
				call(874781, "Move", "Move"),
				call(876826, "Move", "Move"),
				call(878785, "Move", "Move"),
				call(880563, "Stacks", "Stacks (7.7)"),
				call(880831, "Move", "Move"),
				call(882790, "Move", "Move"),
				call(884879, "Move", "Move"),
				call(899968, "Tanks Left, Party Out", "Tanks Left, Party Out (6.6)"),
				call(906860, "Party In", "Party In"),
				call(910858, "Third Tower", "Third Tower"),
				call(921762, "Line Stacks", "Line Stacks (6.2)"),
				call(928829, "Move", "Move"),
				call(930919, "Stacks", "Stacks"),
				call(933456, "Move", "Move"),
				call(935546, "Stacks", "Stacks"),
				call(938078, "Swap", "Swap"),
				call(940208, "Stacks", "Stacks"),
				call(942744, "Move", "Move"),
				call(950613, "Tank LB", "Tank LB (11.7)"),
				call(968752, "Raidwide", "Raidwide (5.7)"),
				call(978808, "Fulgent Blade: CCW", "Fulgent Blade: CCW (6.7)"),
				call(985799, "Move", "Move"),
				call(987891, "Move", "Move"),
				call(989938, "Move", "Move"),
				call(991894, "Move", "Move"),
				call(993670, "Stacks", "Stacks (7.7)"),
				call(993981, "Move", "Move"),
				call(995894, "Move", "Move"),
				call(997938, "Move", "Move"),
				call(1017222, "Tanks Left, Party Out", "Tanks Left, Party Out (6.6)"),
				call(1024112, "Party In", "Party In"),
				call(1028343, "Third Tower", "Third Tower"),
				call(1033948, "Line Stacks", "Line Stacks (6.2)"),
				call(1040991, "Move", "Move"),
				call(1043086, "Stacks", "Stacks"),
				call(1045627, "Move", "Move"),
				call(1047718, "Stacks", "Stacks"),
				call(1050258, "Swap", "Swap"),
				call(1052391, "Stacks", "Stacks"),
				call(1054878, "Move", "Move"),
				call(1059590, "Raidwide", "Raidwide (5.7)"),
				call(1069679, "Fulgent Blade: CCW", "Fulgent Blade: CCW (6.7)"),
				call(1076664, "Move", "Move"),
				call(1078754, "Move", "Move"),
				call(1080842, "Move", "Move"),
				call(1082754, "Move", "Move"),
				call(1084485, "Stacks", "Stacks (7.7)"),
				call(1084798, "Move", "Move"),
				call(1086756, "Move", "Move"),
				call(1088843, "Move", "Move"),
				call(1100948, "Enrage", "Enrage (21.2)")
		);
	}
}
