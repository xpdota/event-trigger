package gg.xp.xivsupport.events.triggers.endwalker.ultimate;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class DSR extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/dsr_cropped.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				// Doorboss
				call(5974, "Raidwide", "Raidwide (4.0)"),
				call(18107, "In", "In (5.0)"),
				call(23144, "Stack on DRG Player", "Stack on DRG Player (5.0)"),
				call(38465, "Second Set", "Second Set"),
				call(56776, "Raidwide", "Raidwide (4.0)"),
				call(71807, "Interrupt Ser Adelphel", "Interrupt Ser Adelphel (4.0)"),
				call(80724, "Purple Square with DRG Player", "Purple Square with DRG Player"),
				call(90082, "Interrupt Ser Adelphel", "Interrupt Ser Adelphel (4.0)"),
				call(98009, "Raidwide", "Raidwide (4.0)"),
				call(99035, "In", "In (5.0)"),
				call(109052, "Interrupt Ser Adelphel", "Interrupt Ser Adelphel (4.0)"),
				call(148479, "Puddle on you", "Puddle on you (5.0)"),
				call(153468, "Move", "Move"),
				call(167236, "Cleave Bait", "Cleave Bait (3.0)"),
				// P2
				call(9230, "Cleave Bait", "Cleave Bait (3.0)"),
				call(36654, "East/West Safe", "East West Safe"),
				call(44460, "Cleave Bait", "Cleave Bait (3.0)"),
				call(49409, "Take Tether", "Take Tether"),
				call(49409, "Thordan EAST", "Thordan EAST"),
				call(63976, "Raidwide", "Raidwide (6.0)"),
				call(103079, "SCH Player and WHM Player", "SCH Player and WHM Player"),
				call(105605, "Counterclockwise", "Counterclockwise"),
				call(106270, "Look away", "Look away (4.0)"),
				call(121568, "Meteor on you", "Meteor on you"),
				call(129366, "Soak First Tower", "Soak First Tower"),
				call(133592, "Drop Meteors", "Drop Meteor #1"),
				call(140000, "Knockback Immune in Tower", "Knockback Immune in Tower"),
				call(174441, "Back then Left", "Back then Left (3.0)"),
				call(182895, "Back then Left", "Back then Left (3.0)"),
				// P3
				call(216689, "Three", "Three"),
				call(222428, "East and Face Out", "East and Face Out (30.0)"),
				call(223808, "Stack, Out, In, then soak tower", "Stack, Out, In, then soak tower"),
				call(231371, "Out", "Out"),
				call(235061, "In", "In"),
				call(238080, "Bait Geirskogul", "Bait Geirskogul"),
				call(245286, "Place Tower Behind You, then In then Out", "Place Tower Behind You, then In then Out"),
				call(252851, "In", "In"),
				call(256542, "Out", "Out"),
				call(268168, "Out of front", "Out of front (2.9)"),
				call(278175, "Bait Geirskogul", "Bait Geirskogul"),
				call(306278, "Out of front", "Out of front (2.9)"),
				// Eyes
				call(350014, "Red", "Red"),
				call(352374, "Blue", "Blue"),
				call(359919, "Red", "Red"),
				call(378385, "Blue", "Blue"),
				call(388498, "Red", "Red"),
				// Intermission
				call(434009, "", "HP"),
				call(454659, "Puddle on you", "Puddle on you (5.0)"),
				call(459648, "Move", "Move"),
				// P5
				call(520722, "Nothing", "Nothing"),
				call(525519, "Lightning", "Lightning (15.0)"),
				call(526806, "Twister", "Twister"),
				call(529071, "Spread", "Spread (3.3)"),
				call(535247, "In with Lightning", "In with Lightning (5.0)"),
				call(541312, "Raidwide", "Raidwide (6.0)"),
				call(583327, "Puddle", "Puddle"),
				call(592204, "Twister", "Twister"),
				call(600279, "Blue Cross", "Blue Cross"),
				call(600368, "Look away", "Look away (4.0)"),
				call(623248, "Raidwide", "Raidwide (6.0)"),
				// P6
				call(683744, "Nidd Buster, Hraes Cleave", "Nidd Buster, Hraes Cleave (6.3)"),
				call(701165, "", "HP Check: Even"),
				call(701165, "Light Party Stacks", "Light Party Stacks (8.0)"),
				call(714987, "Right, near Hraesvelgr", "Right, near Hraesvelgr (7.5)"),
				call(739102, "Nothing", "Nothing (23.0)"),
				call(754794, "In", "In (5.5)"),
				call(770414, "", "HP Check: Attack Hraesvelgr"),
				call(770414, "Light Party Stacks", "Light Party Stacks (8.0)"),
				call(783084, "Left, away from Hraesvelgr", "Left, away from Hraesvelgr (7.5)"),
				call(785092, "Out", "Out (5.5)"),
				call(804102, "Nidd Buster, Hraes Cleave", "Nidd Buster, Hraes Cleave (6.3)"),
				// P7
				call(907745, "Exaflare", "Exaflare (6.0)"),
				call(908681, "Fire - Out", "Fire - Out"),
				call(914613, "Move", "Exaflare"),
				call(916532, "Move", "Exaflare"),
				call(928967, "Stacks", "Stacks (6.0)"),
				call(929902, "Ice - In", "Ice - In"),
				call(953412, "Gigaflare", "Gigaflare (8.0)"),
				call(954344, "Fire - Out", "Fire - Out"),
				call(962386, "Move", "Gigaflare"),
				call(966387, "Move", "Gigaflare"),
				call(986637, "Exaflare", "Exaflare (6.0)"),
				call(987572, "Fire - Out", "Fire - Out"),
				call(993497, "Move", "Exaflare"),
				call(995416, "Move", "Exaflare"),
				call(1007821, "Stacks", "Stacks (6.0)"),
				call(1008757, "Ice - In", "Ice - In"),
				call(1033373, "Gigaflare", "Gigaflare (8.0)"),
				call(1034309, "Ice - In", "Ice - In"),
				call(1042339, "Move", "Gigaflare"),
				call(1046356, "Move", "Gigaflare"),
				call(1066583, "Exaflare", "Exaflare (6.0)"),
				call(1067513, "Ice - In", "Ice - In"),
				call(1073481, "Move", "Exaflare"),
				call(1075400, "Move", "Exaflare"),
				call(1087776, "Stacks", "Stacks (6.0)"),
				call(1088710, "Ice - In", "Ice - In"),
				call(1114345, "Enrage", "Enrage (10.0)")
		);
	}
}
