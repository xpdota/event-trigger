package gg.xp.events.ws;

import gg.xp.context.StateStore;
import gg.xp.events.AutoEventDistributor;
import gg.xp.events.BasicEventQueue;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.state.XivState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class WsHandlerTests {

	private static final Logger log = LoggerFactory.getLogger(WsHandlerTests.class);

	@Test
	public void testZoneAndPlayerChange() {
		EventDistributor<Event> dist = new AutoEventDistributor();

		EventMaster master = new EventMaster(dist);
		master.start();

		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":275160354,\"charName\":\"Foo Bar\"}"));
		master.pushEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":129,\"zoneName\":\"Limsa Lominsa Lower Decks\"}"));
		master.pushEvent(new ActWsRawMsg("{\n" +
				"  \"type\": \"PartyChanged\",\n" +
				"  \"party\": [\n" +
				"    {\n" +
				"      \"id\": \"106891DE\",\n" +
				"      \"name\": \"Player One\",\n" +
				"      \"worldId\": 63,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"107939CF\",\n" +
				"      \"name\": \"Some Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"107E4275\",\n" +
				"      \"name\": \"Who Dis\",\n" +
				"      \"worldId\": 73,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"10801F1A\",\n" +
				"      \"name\": \"Random Person\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"106CD17F\",\n" +
				"      \"name\": \"Pf Hero\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"10669D22\",\n" +
				"      \"name\": \"Foo Bar\",\n" + // This player should be sorted first because they are the actual player
				"      \"worldId\": 65,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"1065C635\",\n" +
				"      \"name\": \"Last Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 36,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    }\n" +
				"  ]\n" +
				"}\n"));
		StateStore stateStore = dist.getStateStore();
		XivState xivState = stateStore.get(XivState.class);

		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException e) {
			// TODO: figure out unit testing solution that doesn't require a delay when we use async queued events
		}

		((BasicEventQueue) master.getQueue()).waitDrain();
		log.info("Queue size: {}", master.getQueue().pendingSize());

		Assert.assertEquals(xivState.getZone().getId(), 129);
		Assert.assertEquals(xivState.getPlayer().getId(), 275160354);

		Assert.assertEquals(xivState.getZone().getName(), "Limsa Lominsa Lower Decks");
		Assert.assertEquals(xivState.getPlayer().getName(), "Foo Bar");

		Assert.assertEquals(xivState.getPartyList().get(0).getId(), 275160354);
		Assert.assertEquals(xivState.getPartyList().get(0).getName(), "Foo Bar");

		Assert.assertEquals(xivState.getPartyList().size(), 7);


	}
}
