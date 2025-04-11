package gg.xp.xivsupport.triggers.Arcadion;

import gg.xp.xivsupport.callouts.ModifiedCalloutHandle;
import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.lang.reflect.Field;
import java.util.List;

public class M8sTest extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/m8s_anon.log";
	}

	@Override
	protected void modifyCalloutSettings(ModifiedCalloutHandle handle) {
		Field field = handle.getField();
		switch (field == null ? "" : field.getName()) {
			case "moonlightFirstTwo",
			     "moonlightSecondTwo",
			     "moonlightMoveSecondQuadrant",
			     "moonlightExtraCollFirst" -> handle.getEnable().set(false);
			default -> handle.getEnable().set(true);
		}
	}

	@Override
	protected long minimumMsBetweenCalls() {
		// The tether callouts can be tightly spaced
		return 650;
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(9991, "Raidwide", "Raidwide (3.7)"),
				call(22920, "Buddies, In, Intercards", "Buddies, In, Intercards (5.7)"),
				call(34067, "Dodge Clones, Out of Middle", "Dodge Clones, Out of Middle (6.7)"),
				call(41026, "Close Stacks", "Close Stacks"),
				call(48826, "Raidwide", "Raidwide (3.7)"),
				call(61272, "Raidwide", "Raidwide (4.7)"),
				call(72471, "Counter-Clockwise", "Counter-Clockwise (5.7)"),
				call(78809, "Spread", "Spread"),
				call(87916, "Tether", "Tether"),
				call(95797, "Multi Stack", "Multi Stack (4.7)"),
				call(111008, "Raidwide", "Raidwide (3.7)"),
				call(118764, "Tank Buster on Gogogule Mamagule", "Tank Buster on Gogogule Mamagule (4.7)"),
				call(147889, "Northeast Safe", "Northeast Safe (5.3)"),
				call(154176, "Dodge Clones, Out of Middle", "Dodge Clones, Out of Middle (6.7)"),
				call(161136, "Far Stacks", "Far Stacks"),
				call(175899, "Adds", "Adds (2.7)"),
				call(183344, "Tethered to East", "Tethered to East"),
				call(189009, "Counter-Clockwise", "Counter-Clockwise"),
				call(190216, "Touch Green, Third", "Touch Green, Third (52.5)"),
				call(193340, "Cleaves", "Cleaves (4.7)"),
				call(207488, "Cleaves", "Cleaves (4.7)"),
				call(221637, "Cleaves", "Cleaves (4.7)"),
				call(229176, "Attack Yellow", "Attack Yellow"),
				call(251842, "Multiple Raidwides", "Multiple Raidwides (3.1)"),
				call(280266, "Stack Between Lines", "Stack Between Lines"),
				call(285397, "Move", "Move"),
				call(286956, "Spread Behind Adds", "Spread Behind Adds"),
				call(292383, "Rotate", "Rotate"),
				call(296438, "Dodge Clones, Out of Middle", "Dodge Clones, Out of Middle (6.7)"),
				call(303421, "Close Stacks, Dodge Lines", "Close Stacks, Dodge Lines"),
				call(309390, "Dodge Lines", "Dodge Lines (2.2)"),
				call(317357, "Tank Buster on Mumuwesa Mamawesa", "Tank Buster on Mumuwesa Mamawesa (4.7)"),
				call(341939, "Start North", "North"),
				call(344343, "Stack", "Stack"),
				call(347998, "North", "North, East, South, West"),
				call(350894, "East", "East, South, West"),
				call(352945, "South", "South, West"),
				call(354058, "Spread", "Spread"),
				call(354906, "West", "West"),
				call(356954, "Cardinals", "Cardinals"),
				call(361188, "Spread, Out, Cardinals", "Spread, Out, Cardinals (5.7)"),
				call(371341, "Multi Stack", "Multi Stack (4.7)"),
				call(386516, "Raidwide", "Raidwide (3.7)"),
				call(401299, "Raidwide", "Raidwide (3.7)"),
				call(458701, "Stacks", "Stacks (4.7)"),
				call(469812, "DPS Marked", "DPS Marked"),
				call(480066, "Double Tankbuster", "Double Tankbuster (6.7)"),
				call(492348, "Out, East", "Out, East (6.7)"),
				call(503386, "DPS Marked", "DPS Marked"),
				call(515631, "Stacks", "Stacks (4.7)"),
				call(528921, "Off Platform, Cleave", "Off Platform, Cleave (4.7)"),
				call(535973, "Markers on Gogogule Mamagule, Dipipi Dipi", "Markers on Gogogule Mamagule, Dipipi Dipi"),
				call(548362, "Partner Towers", "Partner Towers (2.7)"),
				call(564636, "South", "Tether on Sapipi Sapi, South"),
				call(566153, "Southeast", "Tether on Sapipi Sapi, Southeast"),
				call(567626, "", "Tether on Mumuwesa Mamawesa, Southeast"),
				call(573200, "", "Tether on YOU, Southeast"),
				call(575563, "Northeast", "Tether on YOU, Northeast"),
				call(576232, "", "Tether on Wewerolu Dodorolu, Northeast"),
				call(580150, "", "Tether on YOU, Northeast"),
				call(581755, "Northwest", "Tether on Nanaleqi Gogoleqi, Northwest"),
				call(584297, "", "Tether on Dipipi Dipi, Northwest"),
				call(587733, "", "Tether on Nanaleqi Gogoleqi, Northwest"),
				call(588444, "Southwest", "Tether on Gogogule Mamagule, Southwest"),
				call(589827, "", "Tether on Kokofuso Papafuso, Southwest"),
				call(600712, "South", "Tether on Kokofuso Papafuso, South"),
				call(609270, "Clockwise", "Clockwise (7.0)"),
				call(613819, "Out Right", "Out Right"),
				call(618238, "In Left", "In Left"),
				call(622657, "Sides Left", "Sides Left"),
				call(627068, "Center Right", "Center Right"),
				call(631484, "In Left", "In Left"),
				call(639376, "Stacks", "Stacks (4.7)"),
				call(652475, "DPS Marked", "DPS Marked"),
				call(662713, "Double Tankbuster", "Double Tankbuster (6.7)"),
				call(690809, "Short Tether with Nanaleqi Gogoleqi", "Short Tether with Nanaleqi Gogoleqi (21.0)"),
				call(701013, "2-Tower Northwest", "2-Tower Northwest (7.7)"),
				call(711453, "Out, Northeast", "Out, Northeast (6.7)"),
				call(724518, "Supports Marked", "Supports Marked"),
				call(737588, "Enrage Tower", "Enrage Tower (7.7)"),
				call(753027, "Leave Platform", "Leave Platform (3.7)"),
				call(760211, "Enrage Tower", "Enrage Tower (4.7)"),
				call(779838, "Enrage Tower", "Enrage Tower (4.7)"),
				call(799425, "Enrage Tower", "Enrage Tower (4.7)"),
				call(819046, "Enrage Tower", "Enrage Tower (4.7)"),
				call(831482, "Enrage", "Enrage (10.7)")
		);
	}
}
