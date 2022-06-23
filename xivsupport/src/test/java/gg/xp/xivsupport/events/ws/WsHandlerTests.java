package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.HitPoints;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gg.xp.xivdata.data.Job.AST;
import static gg.xp.xivdata.data.Job.BLM;
import static gg.xp.xivdata.data.Job.DNC;
import static gg.xp.xivdata.data.Job.DRG;
import static gg.xp.xivdata.data.Job.SMN;
import static gg.xp.xivdata.data.Job.WAR;
import static gg.xp.xivdata.data.Job.WHM;

public class WsHandlerTests {

	private static final Logger log = LoggerFactory.getLogger(WsHandlerTests.class);

	@Test
	public void testZoneAndPlayerChange() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventMaster master = pico.getComponent(EventMaster.class);


		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":275160354,\"charName\":\"Foo Bar\"}"));
		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":129,\"zoneName\":\"Limsa Lominsa Lower Decks\"}"));
		// This player should be sorted first because they are the actual player
		// Intentionally put the player as the wrong job to verify that combatants data takes priority over party data
		master.pushEvent(new ActWsRawMsg("""
				{
				  "type": "PartyChanged",
				  "party": [
				    {
				      "id": "106891DE",
				      "name": "Player One",
				      "worldId": 63,
				      "job": 22,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "107939CF",
				      "name": "Some Guy",
				      "worldId": 65,
				      "job": 27,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "107E4275",
				      "name": "Who Dis",
				      "worldId": 73,
				      "job": 25,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "10801F1A",
				      "name": "Random Person",
				      "worldId": 65,
				      "job": 21,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "AABBCCDD",
				      "name": "Other Alliance",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": false
				    },
				    {
				      "id": "106CD17F",
				      "name": "Pf Hero",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "10669D22",
				      "name": "Foo Bar",
				      "worldId": 65,
				      "job": 12,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "1065C635",
				      "name": "Last Guy",
				      "worldId": 65,
				      "job": 38,
				      "level": 0,
				      "inParty": true
				    }
				  ]
				}
				"""));
		String combatantsMsg = """
				{
				  "combatants": [
				    {
				      "CurrentWorldID": 65,
				      "WorldID": 65,
				      "WorldName": "Midgardsormr",
				      "BNpcID": 0,
				      "BNpcNameID": 0,
				      "PartyType": 0,
				      "ID": 275160354,
				      "OwnerID": 0,
				      "type": 1,
				      "Job": 24,
				      "Level": 80,
				      "Name": "Foo Bar",
				      "CurrentHP": 12345,
				      "MaxHP": 122700,
				      "CurrentMP": 10000,
				      "MaxMP": 10000,
				      "PosX": 114.926422,
				      "PosY": -83.86734,
				      "PosZ": 44.3433,
				      "Heading": -1.66136408
				    },
				    {
				      "CurrentWorldID": 0,
				      "WorldID": 0,
				      "WorldName": "",
				      "BNpcID": 387,
				      "BNpcNameID": 387,
				      "PartyType": 0,
				      "ID": 123456,
				      "OwnerID": 0,
				      "type": 2,
				      "Job": 0,
				      "Level": 0,
				      "Name": "Some Boss",
				      "CurrentHP": 0,
				      "MaxHP": 0,
				      "CurrentMP": 0,
				      "MaxMP": 10000,
				      "PosX": 116.301865,
				      "PosY": -82.89748,
				      "PosZ": 43.9826,
				      "Heading": -1.55023289
				    }
				  ],
				  "rseq":0}""";
		master.pushEventAndWait(new ActWsRawMsg(combatantsMsg));
		XivStateImpl xivState = pico.getComponent(XivStateImpl.class);

		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			// TODO: figure out unit testing solution that doesn't require a delay when we use async queued events
			// The purpose of doing those async is to eventually implement event merging if needed for performance.
		}

		log.info("Queue size: {}", master.getQueue().pendingSize());

		XivZone zone = xivState.getZone();
		Assert.assertEquals(zone.getId(), 129);
		Assert.assertEquals(zone.getName(), "Limsa Lominsa Lower Decks");

		XivPlayerCharacter player = xivState.getPlayer();

		Assert.assertEquals(player.getId(), 275160354);
		Assert.assertEquals(player.getName(), "Foo Bar");
		Assert.assertEquals(player.getJob(), WHM);
		Assert.assertTrue(player.isThePlayer());
		Assert.assertTrue(player.isPc());
		Assert.assertFalse(player.isEnvironment());
		Assert.assertEquals(player.getType(), CombatantType.PC);
		Assert.assertEquals(player.getHp(), new HitPoints(12345, 122700));
		Position originalPos = new Position(114.926422, -83.86734, 44.3433, -1.66136408);
		Assert.assertEquals(player.getPos(), originalPos);
		/*
								      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408

		 */

		Assert.assertEquals(xivState.getPartyList().get(0).getId(), 275160354);
		Assert.assertEquals(xivState.getPartyList().get(0).getName(), "Foo Bar");

		Map<Long, XivCombatant> combatants = xivState.getCombatants();
		XivCombatant otherCombatant = combatants.get(123456L);
		Assert.assertEquals(otherCombatant.getName(), "Some Boss");
		Assert.assertEquals(otherCombatant.getType(), CombatantType.NPC);
		Assert.assertFalse(otherCombatant.isPc());
		Assert.assertFalse(otherCombatant.isThePlayer());
		Assert.assertFalse(otherCombatant.isEnvironment());


		List<Job> jobs = xivState.getPartyList().stream().map(XivPlayerCharacter::getJob).collect(Collectors.toList());

		// Player sorted first, rest by sort order
		MatcherAssert.assertThat(jobs, Matchers.equalTo(List.of(WHM, WAR, AST, DRG, DNC, BLM, SMN)));

		Assert.assertEquals(xivState.getPartyList().size(), 7);

		Position posOver = new Position(1, 2, 3, 4);
		HitPoints hpOver = new HitPoints(123, 456);
		xivState.provideCombatantPos(player, posOver);
		xivState.provideCombatantHP(player, hpOver);
		xivState.flushProvidedValues();
		player = xivState.getPlayer();
		Assert.assertEquals(player.getHp(), hpOver);
		Assert.assertEquals(player.getPos(), posOver);

		// Some of the WS processing checks that the values are different and ignores it if not, so we need to change
		// something irrelevant.
		master.pushEventAndWait(new ActWsRawMsg(combatantsMsg.replaceAll("10000", "9999")));

		player = xivState.getPlayer();
		Assert.assertEquals(player.getHp(), hpOver);
		Assert.assertEquals(player.getPos(), originalPos);


		// TODO: combatant removal tests

	}

	@Test
	public void testZoneAndPlayerChange_FullRefresh() throws InterruptedException {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventMaster master = pico.getComponent(EventMaster.class);


		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":275160354,\"charName\":\"Foo Bar\"}"));
		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":129,\"zoneName\":\"Limsa Lominsa Lower Decks\"}"));
		// This player should be sorted first because they are the actual player
		master.pushEvent(new ActWsRawMsg("""
				{
				  "type": "PartyChanged",
				  "party": [
				    {
				      "id": "106891DE",
				      "name": "Player One",
				      "worldId": 63,
				      "job": 22,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "107939CF",
				      "name": "Some Guy",
				      "worldId": 65,
				      "job": 27,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "107E4275",
				      "name": "Who Dis",
				      "worldId": 73,
				      "job": 25,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "10801F1A",
				      "name": "Random Person",
				      "worldId": 65,
				      "job": 21,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "AABBCCDD",
				      "name": "Other Alliance",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": false
				    },
				    {
				      "id": "106CD17F",
				      "name": "Pf Hero",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "10669D22",
				      "name": "Foo Bar",
				      "worldId": 65,
				      "job": 24,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "1065C635",
				      "name": "Last Guy",
				      "worldId": 65,
				      "job": 38,
				      "level": 0,
				      "inParty": true
				    }
				  ]
				}
				"""));
		String combatantsMsg = """
				{
				  "combatants": [
				    {
				      "CurrentWorldID": 65,
				      "WorldID": 65,
				      "WorldName": "Midgardsormr",
				      "BNpcID": 0,
				      "BNpcNameID": 0,
				      "PartyType": 0,
				      "ID": 275160354,
				      "OwnerID": 0,
				      "type": 1,
				      "Job": 24,
				      "Level": 80,
				      "Name": "Foo Bar",
				      "CurrentHP": 12345,
				      "MaxHP": 122700,
				      "CurrentMP": 10000,
				      "MaxMP": 10000,
				      "PosX": 114.926422,
				      "PosY": -83.86734,
				      "PosZ": 44.3433,
				      "Heading": -1.66136408
				    },
				    {
				      "CurrentWorldID": 0,
				      "WorldID": 0,
				      "WorldName": "",
				      "BNpcID": 387,
				      "BNpcNameID": 387,
				      "PartyType": 0,
				      "ID": 123456,
				      "OwnerID": 0,
				      "type": 2,
				      "Job": 0,
				      "Level": 0,
				      "Name": "Some Boss",
				      "CurrentHP": 0,
				      "MaxHP": 0,
				      "CurrentMP": 0,
				      "MaxMP": 10000,
				      "PosX": 116.301865,
				      "PosY": -82.89748,
				      "PosZ": 43.9826,
				      "Heading": -1.55023289
				    }
				  ],
				  "rseq": "allCombatants"}""";
		master.pushEventAndWait(new ActWsRawMsg(combatantsMsg));
		XivStateImpl xivState = pico.getComponent(XivStateImpl.class);

		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			// TODO: figure out unit testing solution that doesn't require a delay when we use async queued events
			// The purpose of doing those async is to eventually implement event merging if needed for performance.
		}

		log.info("Queue size: {}", master.getQueue().pendingSize());

		XivZone zone = xivState.getZone();
		Assert.assertEquals(zone.getId(), 129);
		Assert.assertEquals(zone.getName(), "Limsa Lominsa Lower Decks");

		XivPlayerCharacter player = xivState.getPlayer();

		Assert.assertEquals(player.getId(), 275160354);
		Assert.assertEquals(player.getName(), "Foo Bar");
		Assert.assertEquals(player.getJob(), WHM);
		Assert.assertTrue(player.isThePlayer());
		Assert.assertTrue(player.isPc());
		Assert.assertFalse(player.isEnvironment());
		Assert.assertEquals(player.getType(), CombatantType.PC);
		Assert.assertEquals(player.getHp(), new HitPoints(12345, 122700));
		Position originalPos = new Position(114.926422, -83.86734, 44.3433, -1.66136408);
		Assert.assertEquals(player.getPos(), originalPos);
		/*
								      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408

		 */

		Assert.assertEquals(xivState.getPartyList().get(0).getId(), 275160354);
		Assert.assertEquals(xivState.getPartyList().get(0).getName(), "Foo Bar");

		Map<Long, XivCombatant> combatants = xivState.getCombatants();
		XivCombatant otherCombatant = combatants.get(123456L);
		Assert.assertEquals(otherCombatant.getName(), "Some Boss");
		Assert.assertEquals(otherCombatant.getType(), CombatantType.NPC);
		Assert.assertFalse(otherCombatant.isPc());
		Assert.assertFalse(otherCombatant.isThePlayer());
		Assert.assertFalse(otherCombatant.isEnvironment());


		List<Job> jobs = xivState.getPartyList().stream().map(XivPlayerCharacter::getJob).collect(Collectors.toList());

		// Player sorted first, rest by sort order
		MatcherAssert.assertThat(jobs, Matchers.equalTo(List.of(WHM, WAR, AST, DRG, DNC, BLM, SMN)));

		Assert.assertEquals(xivState.getPartyList().size(), 7);

		Position posOver = new Position(1, 2, 3, 4);
		HitPoints hpOver = new HitPoints(123, 456);
		xivState.provideCombatantPos(player, posOver);
		xivState.provideCombatantHP(player, hpOver);
		xivState.flushProvidedValues();
		player = xivState.getPlayer();
		Assert.assertEquals(player.getHp(), hpOver);
		Assert.assertEquals(player.getPos(), posOver);

		// Some of the WS processing checks that the values are different and ignores it if not, so we need to change
		// something irrelevant.
		master.pushEventAndWait(new ActWsRawMsg(combatantsMsg.replaceAll("10000", "9999")));


		player = xivState.getPlayer();
		Assert.assertEquals(player.getHp(), hpOver);
		Assert.assertEquals(player.getPos(), originalPos);

	}
}
