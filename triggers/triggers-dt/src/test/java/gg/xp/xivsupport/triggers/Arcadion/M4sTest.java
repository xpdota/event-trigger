package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class M4sTest
		extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m4s_anon.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(9892, "Raidwide", "Raidwide (4.7)"),
				call(31592, "Avoid Lines", "Avoid Lines (5.7)"),
				call(44070, "Inside, Avoid Lines", "Inside, Avoid Lines (4.7)"),
				call(52230, "Outside, Bait Out", "Outside, Bait Out (4.7)"),
				call(60384, "Inside, Baiters In", "Inside, Baiters In (13.7)"),
				call(75441, "Outside, Baiters Out", "Outside, Baiters Out"),
				call(79007, "Inside, Baiters In", "Inside, Baiters In"),
				call(82566, "Outside, Baiters Out", "Outside, Baiters Out"),
				call(93844, "Raidwide", "Raidwide (4.7)"),
				call(114426, "Stand on Cardinals, Multiple Hits", "Stand on Cardinals, Multiple Hits (2.7)"),
				call(128331, "Spread Southwest", "Spread Southwest (6.7)"),
				call(138483, "Tank Buster on Wichichi Wichi", "Tank Buster on Wichichi Wichi (4.7)"),
				call(155365, "Clock Positions", "Clock Positions (2.7)"),
				call(170825, "2 Short", "2 Short"),
				call(174920, "Spark 2 - East, West", "Spark 2 - East, West"),
				call(184014, "Buddies West with Hahaja Haja, Dedeke Deke", "Buddies West with Hahaja Haja, Dedeke Deke (6.7)"),
				call(195328, "East Safe", "East Safe"),
				call(204240, "Stack, Multiple Hits", "Stack, Multiple Hits (3.7)"),
				call(234671, "West", "West (6.5)"),
				call(245941, "Go North", "Go North (5.4)"),
				call(251643, "Get Hit by Protean", "Get Hit by Protean (7.0)"),
				call(259081, "Go South", "Go South (5.4)"),
				call(264787, "Roundhouse - Spread", "Roundhouse - Spread (5.0)"),
				call(272229, "Go North", "Go North (5.4)"),
				call(277934, "Proximate Current", "Proximate Current (5.0)"),
				call(286216, "Tank Buster on Dedeke Deke", "Tank Buster on Dedeke Deke (4.7)"),
				call(320628, "Dodge Proteans", "Dodge Proteans (2.7)"),
				call(323608, "Move", "Move"),
				call(326683, "Move", "Move"),
				call(329713, "Move", "Move"),
				call(332745, "Move", "Move"),
				call(335997, "Cover", "Cover"),
				call(338805, "Move", "Move"),
				call(346874, "Dodge Proteans", "Dodge Proteans (2.7)"),
				call(349860, "Move", "Move"),
				call(352941, "Move", "Move"),
				call(355975, "Move", "Move"),
				call(359005, "Move", "Move"),
				call(362253, "Behind", "Behind"),
				call(365058, "Move", "Move"),
				call(367148, "Multiple Raidwides, Get Knocked South", "Multiple Raidwides, Get Knocked South"),
				call(406314, "Multiple Raidwides", "Multiple Raidwides (4.7)"),
				call(422168, "Northwest Corner and Northeast Side", "Northwest Corner and Northeast Side (7.2)"),
				call(434650, "Sides", "Sides (4.7)"),
				call(447826, "Tethers to Tanks then Spread", "Tethers to Tanks then Spread"),
				call(456869, "Grab Bombs from Tanks", "Grab Bombs from Tanks"),
				call(466984, "Later: Knockback East then West", "Later: Knockback East then West (6.7)"),
				call(477142, "Knockback East then West", "Knockback East then West (4.7)"),
				call(499461, "Raidwide", "Raidwide (4.7)"),
				call(514785, "Bait Middle", "Bait Middle (3.7)"),
				call(517859, "Northeast safe", "Northeast safe"),
				call(523920, "Southwest safe, In", "Southwest safe, In (4.7)"),
				call(544283, "Spread in Cardinals", "Spread in Cardinals"),
				call(551277, "Buddy in Intercards", "Buddy in Intercards"),
				call(555337, "Middle", "Middle (3.8)"),
				call(562598, "Raidwide", "Raidwide (4.7)"),
				call(571821, "Later: Knockback West then East", "Later: Knockback West then East (6.7)"),
				call(581982, "Out of Middle, Arena Splitting", "Out of Middle, Arena Splitting (5.7)"),
				call(591118, "Soak Tower", "Soak Tower (2.7)"),
				call(600201, "South-Middle", "South-Middle"),
				call(612496, "South-Middle, South, North, North-Middle", "South-Middle, South, North, North-Middle"),
				call(617798, "South", "South"),
				call(623192, "North", "North"),
				call(628628, "North-Middle", "North-Middle"),
				call(635407, "Knockback West then East", "Knockback West then East (4.7)"),
				call(648596, "Tethers to Tanks then Spread", "Tethers to Tanks then Spread"),
				call(657695, "Avoid Tethers", "Avoid Tethers"),
				call(667810, "Later: Knockback West then East", "Later: Knockback West then East (6.7)"),
				call(677966, "Raidwide", "Raidwide (4.7)"),
				call(689905, "Long Negatron", "Long Negatron (38.0)"),
				call(701753, "Soak West, East Towers", "Soak West, East Towers"),
				call(710441, "Middle", "Middle (4.7)"),
				call(718147, "Bait Southwest, Northwest", "Bait Southwest, Northwest (9.8)"),
				call(726613, "Knockback West then East", "Knockback West then East (4.7)"),
				call(742871, "Raidwides", "Raidwides (4.7)"),
				call(747946, "Front/Middle", "Front/Middle (8.8)"),
				call(760016, "Raidwides", "Raidwides (4.7)")
		);
	}
}
