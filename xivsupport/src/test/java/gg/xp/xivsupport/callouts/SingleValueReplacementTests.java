package gg.xp.xivsupport.callouts;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.callouts.conversions.GlobalArenaSectorConverter;
import gg.xp.xivsupport.callouts.conversions.PlayerNameConversion;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.ManaPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SingleValueReplacementTests {

	private static final XivPlayerCharacter thePlayer = new XivPlayerCharacter(0x1000_0001, "The Player", Job.WHM, XivWorld.of(), true, 1, new HitPoints(1000, 2000), ManaPoints.of(1000, 2000), Position.of2d(100, 100), 0, 0, 1, 100, 0, 0);
	private static final XivPlayerCharacter otherPlayer = new XivPlayerCharacter(0x1000_0002, "Other Player", Job.SGE, XivWorld.of(), false, 1, new HitPoints(1000, 2000), ManaPoints.of(1000, 2000), Position.of2d(100, 100), 0, 0, 1, 100, 0, 0);
	private static final XivCombatant npc = new XivCombatant(0x4000_0001, "An NPC");

	@Test
	public void testPlayerNameReplacements() {
		MutablePicoContainer pico = XivMain.testingMinimalInit();
		pico.addComponent(SingleValueReplacement.class);
		pico.addComponent(GlobalArenaSectorConverter.class);
		SingleValueReplacement svr = pico.getComponent(SingleValueReplacement.class);
		// Test defaults
		Assert.assertEquals(svr.singleReplacement(thePlayer), "YOU");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "Other Player");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		// Change "YOU" replacement
		svr.getReplacementForYou().set("You");
		Assert.assertEquals(svr.singleReplacement(thePlayer), "You");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "Other Player");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		// Change to job display for players
		svr.getPcNameStyle().set(PlayerNameConversion.JOB_ABBREV);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "You");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "SGE");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		// Enable "append you"
		svr.getAppendYou().set(true);
		// Should auto-disable replace you
		Assert.assertFalse(svr.getReplaceYou().get());
		Assert.assertEquals(svr.singleReplacement(thePlayer), "WHM You");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "SGE");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		// Enable "replace you" - disables "append you"
		svr.getReplaceYou().set(true);
		Assert.assertFalse(svr.getAppendYou().get());
		Assert.assertEquals(svr.singleReplacement(thePlayer), "You");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "SGE");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		// Disable both
		svr.getReplaceYou().set(false);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "WHM");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "SGE");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		// Test the rest of the styles
		svr.getPcNameStyle().set(PlayerNameConversion.FULL_NAME);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "The Player");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "Other Player");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		svr.getPcNameStyle().set(PlayerNameConversion.FIRST_NAME);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "The");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "Other");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		svr.getPcNameStyle().set(PlayerNameConversion.LAST_NAME);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "Player");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "Player");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		svr.getPcNameStyle().set(PlayerNameConversion.JOB_NAME);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "White Mage");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "Sage");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");

		svr.getPcNameStyle().set(PlayerNameConversion.INITIALS);
		Assert.assertEquals(svr.singleReplacement(thePlayer), "T.P.");
		Assert.assertEquals(svr.singleReplacement(otherPlayer), "O.P.");
		Assert.assertEquals(svr.singleReplacement(npc), "An NPC");
	}

}
