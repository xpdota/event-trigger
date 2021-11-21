package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CombatantType;
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

import static gg.xp.xivdata.jobs.Job.WHM;
import static gg.xp.xivdata.jobs.Job.AST;
import static gg.xp.xivdata.jobs.Job.BLM;
import static gg.xp.xivdata.jobs.Job.DNC;
import static gg.xp.xivdata.jobs.Job.DRG;
import static gg.xp.xivdata.jobs.Job.SMN;
import static gg.xp.xivdata.jobs.Job.WAR;

public class WsHandlerTests {

	private static final Logger log = LoggerFactory.getLogger(WsHandlerTests.class);

	@Test
	public void testZoneAndPlayerChange() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventMaster master = pico.getComponent(EventMaster.class);


		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":275160354,\"charName\":\"Foo Bar\"}"));
		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":129,\"zoneName\":\"Limsa Lominsa Lower Decks\"}"));
		master.pushEvent(new ActWsRawMsg("{\n" +
				"  \"type\": \"PartyChanged\",\n" +
				"  \"party\": [\n" +
				"    {\n" +
				"      \"id\": \"106891DE\",\n" +
				"      \"name\": \"Player One\",\n" +
				"      \"worldId\": 63,\n" +
				"      \"job\": 22,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"107939CF\",\n" +
				"      \"name\": \"Some Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 27,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"107E4275\",\n" +
				"      \"name\": \"Who Dis\",\n" +
				"      \"worldId\": 73,\n" +
				"      \"job\": 25,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"10801F1A\",\n" +
				"      \"name\": \"Random Person\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 21,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"AABBCCDD\",\n" +
				"      \"name\": \"Other Alliance\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 33,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": false\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"106CD17F\",\n" +
				"      \"name\": \"Pf Hero\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 33,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"10669D22\",\n" +
				"      \"name\": \"Foo Bar\",\n" + // This player should be sorted first because they are the actual player
				"      \"worldId\": 65,\n" +
				"      \"job\": 24,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"1065C635\",\n" +
				"      \"name\": \"Last Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 38,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    }\n" +
				"  ]\n" +
				"}\n"));
		master.pushEvent(new ActWsRawMsg(
				"{\n" +
						"  \"combatants\": [\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 275160354,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 24,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Foo Bar\",\n" +
						"      \"CurrentHP\": 122700,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    },\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 0,\n" +
						"      \"WorldID\": 0,\n" +
						"      \"WorldName\": \"\",\n" +
						"      \"BNpcID\": 387,\n" +
						"      \"BNpcNameID\": 387,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 123456,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 2,\n" +
						"      \"Job\": 0,\n" +
						"      \"Level\": 0,\n" +
						"      \"Name\": \"Some Boss\",\n" +
						"      \"CurrentHP\": 0,\n" +
						"      \"MaxHP\": 0,\n" +
						"      \"CurrentMP\": 0,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 116.301865,\n" +
						"      \"PosY\": -82.89748,\n" +
						"      \"PosZ\": 43.9826,\n" +
						"      \"Heading\": -1.55023289\n" +
						"    }\n" +
						"  ],\n" +
						"  \"rseq\":0" +
						"}"
		));
		StateStore stateStore = pico.getComponent(StateStore.class);
		XivState xivState = stateStore.get(XivState.class);

		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			// TODO: figure out unit testing solution that doesn't require a delay when we use async queued events
			// The purpose of doing those async is to eventually implement event merging if needed for performance.
		}

		((BasicEventQueue) master.getQueue()).waitDrain();
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


	}
}
