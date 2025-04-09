package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M5sTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m5s_anon.log";
	}

	@Override
	protected long minimumMsBetweenCalls() {
		// it's the nisi call between Let's Dance cleaves
		return 750;
	}


	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(8985, "Tankbuster with Bleed", "Tankbuster with Bleed (4.7)"),
				call(22549, "A Side Stocked", "A Side Stocked"),
				call(28642, "West then East Role Stacks", "West then East Role Stacks (4.7)"),
				call(35556, "East Role Stacks", "East Role Stacks"),
				call(41832, "B Side Stocked", "B Side Stocked"),
				call(47938, "West then East Light Parties", "West then East Light Parties (4.7)"),
				call(54486, "East Light Parties", "East Light Parties"),
				call(59162, "Raidwide", "Raidwide (4.7)"),
				call(72523, "Raidwide", "Raidwide (3.7)"),
				call(77071, "Short Timer", "Short Timer (23.5)"),
				call(84810, "In then Out", "In then Out (4.7)"),
				call(89877, "Out", "Out"),
				call(93610, "Soak", "Soak (7.0)"),
				call(94852, "A Side Stocked", "A Side Stocked"),
				call(111442, "West then East Role Stacks", "West then East Role Stacks (4.7)"),
				call(117972, "East Role Stacks", "East Role Stacks"),
				call(122729, "Raidwide", "Raidwide (4.7)"),
				call(129840, "Tankbuster with Bleed", "Tankbuster with Bleed (4.7)"),
				call(149664, "Out", "Out (4.9)"),
				call(155172, "In", "In"),
				call(157621, "Out", "Out"),
				call(160065, "In", "In"),
				call(162511, "Out, Dodge", "Out, Dodge"),
				call(164958, "In", "In"),
				call(167442, "Out", "Out"),
				call(169930, "In", "In"),
				call(174380, "Second Beta", "Second Beta (16.1)"),
				call(176514, "Start East", "East, East, West, East, West, West, East, West"),
				call(183309, "Stay East", "Stay East"),
				call(185800, "Move West", "Move West"),
				call(186554, "Touch Somomo Somo", "Touch Somomo Somo (4.0)"),
				call(188287, "Move East", "Move East"),
				call(190774, "Move West", "Move West"),
				call(193262, "Stay West", "Stay West"),
				call(195711, "Move East", "Move East"),
				call(198159, "Move West", "Move West"),
				call(203806, "Raidwide", "Raidwide (4.7)"),
				call(223035, "A Side Stocked", "A Side Stocked"),
				call(237792, "Spread", "Spread (4.7)"),
				call(245932, "Partners", "Partners (4.7)"),
				call(256426, "Out then In", "Out then In (4.7)"),
				call(261492, "In", "In"),
				call(266427, "West then East Role Stacks", "West then East Role Stacks (4.7)"),
				call(273360, "East Role Stacks", "East Role Stacks"),
				call(278657, "Tankbuster with Bleed", "Tankbuster with Bleed (4.7)"),
				call(285816, "Raidwide", "Raidwide (4.7)"),
				call(306421, "North/South Safe", "North/South Safe (10.2)"),
				call(311313, "Partners", "Partners (4.7)"),
				call(320475, "Raidwide", "Raidwide (3.7)"),
				call(325016, "Soak", "Soak (9.5)"),
				call(335605, "Bait", "Bait"),
				call(341711, "B Side Stocked", "B Side Stocked"),
				call(347818, "East then West Light Parties", "East then West Light Parties (4.7)"),
				call(354749, "West Light Parties", "West Light Parties"),
				call(359059, "Raidwide", "Raidwide (4.7)"),
				call(378836, "Out", "Out (4.9)"),
				call(384345, "In", "In"),
				call(386831, "Out", "Out"),
				call(389318, "In, Dodge", "In, Dodge"),
				call(391807, "Out", "Out"),
				call(394292, "In", "In"),
				call(396780, "Out", "Out"),
				call(399228, "In", "In"),
				call(405931, "Start South", "South, North, West, East, North, South, East, West"),
				call(412728, "Move North", "Move North"),
				call(414192, "Move West", "Move West"),
				call(415661, "Move East", "Move East"),
				call(417126, "Move North", "Move North"),
				call(418589, "Move South", "Move South"),
				call(420050, "Move East", "Move East"),
				call(421519, "Move West", "Move West"),
				call(425205, "Raidwide", "Raidwide (4.7)"),
				call(452251, "North Safe", "North Safe (4.3)"),
				call(456341, "West Safe", "West Safe (4.3)"),
				call(459897, "West Safe", "West Safe (4.7)"),
				call(471243, "East/West Out", "East/West Out (10.2)"),
				call(487226, "East/West In", "East/West In (10.2)"),
				call(502378, "Between Northwest and North", "Between Northwest and North (4.6)"),
				call(509355, "Tankbuster with Bleed", "Tankbuster with Bleed (4.7)"),
				call(528396, "Spread", "Spread (4.7)"),
				call(536577, "In then Out", "In then Out (4.7)"),
				call(541647, "Out", "Out"),
				call(548624, "Partners", "Partners (4.7)"),
				call(555750, "Raidwide", "Raidwide (4.7)"),
				call(565904, "Raidwide", "Raidwide (4.7)"),
				call(593520, "Enrage", "Enrage (11.7)")

		);
	}
}
