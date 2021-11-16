package gg.xp.xivsupport.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.triggers.jails.AutoMarkRequest;
import gg.xp.xivsupport.events.triggers.jails.FinalTitanJailsSolvedEvent;
import gg.xp.xivsupport.events.triggers.jails.UnsortedTitanJailsSolvedEvent;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

public class JailExampleTest {

	private static final Logger log = LoggerFactory.getLogger(JailExampleTest.class);

	/**
	 * End to end example for titan jails
	 */
	@Test
	public void jailTest() throws InterruptedException {
		// Test setup
		MutablePicoContainer container = XivMain.testingMasterInit();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

//		// Register plugins
//		dist.registerHandler(ACTLogLineEvent.class, new Line21Parser()::handle);
//		JailSolver jailSolver = new JailSolver();
//		dist.registerHandler(AbilityUsedEvent.class, jailSolver
		List<Class<? extends BaseEvent>> eventsWeCareAbout = List.of(
				ACTLogLineEvent.class,
				AbilityUsedEvent.class,
				UnsortedTitanJailsSolvedEvent.class,
				FinalTitanJailsSolvedEvent.class,
				CalloutEvent.class,
				TtsRequest.class,
				AutoMarkRequest.class
		);
		// TODO: make this a template for integration testing
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":22,\"charName\":\"Foo Bar\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":777,\"zoneName\":\"the Weapon's Refrain (Ultimate)\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\n" +
				"  \"type\": \"PartyChanged\",\n" +
				"  \"party\": [\n" +
				"    {\n" +
				"      \"id\": \"10\",\n" +
				"      \"name\": \"Player One\",\n" +
				"      \"worldId\": 63,\n" +
				"      \"job\": 22,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"13\",\n" +
				"      \"name\": \"Random Person\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 21,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"12\",\n" +
				"      \"name\": \"Who Dis\",\n" +
				"      \"worldId\": 73,\n" +
				"      \"job\": 25,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"11\",\n" +
				"      \"name\": \"Some Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 27,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"14\",\n" +
				"      \"name\": \"Other Alliance\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 33,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"16\",\n" +
				"      \"name\": \"Foo Bar\",\n" + // This player should be sorted first because they are the actual player
				"      \"worldId\": 65,\n" +
				"      \"job\": 24,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"15\",\n" +
				"      \"name\": \"Pf Hero\",\n" +
				"      \"worldId\": 79,\n" +
				"      \"job\": 33,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    },\n" +
				"    {\n" +
				"      \"id\": \"17\",\n" +
				"      \"name\": \"Last Guy\",\n" +
				"      \"worldId\": 65,\n" +
				"      \"job\": 38,\n" +
				"      \"level\": 0,\n" +
				"      \"inParty\": true\n" +
				"    }\n" +
				"  ]\n" +
				"}\n"));
		dist.acceptEvent(new ActWsRawMsg(
				"{\n" +
						"  \"combatants\": [\n" +
						"    {\n" +
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 16,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 22,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Player One\",\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 17,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 27,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Some Guy\",\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 18,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 25,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Who Dis\",\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 19,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 21,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Random Person\",\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 20,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 33,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Other Alliance\",\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 21,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 33,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Pf Hero\",\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 22,\n" +
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
						"      \"CurrentWorldID\": 65,\n" +
						"      \"WorldID\": 65,\n" +
						"      \"WorldName\": \"Midgardsormr\",\n" +
						"      \"BNpcID\": 0,\n" +
						"      \"BNpcNameID\": 0,\n" +
						"      \"PartyType\": 0,\n" +
						"      \"ID\": 23,\n" +
						"      \"OwnerID\": 0,\n" +
						"      \"type\": 1,\n" +
						"      \"Job\": 38,\n" +
						"      \"Level\": 80,\n" +
						"      \"Name\": \"Last Guy\",\n" +
						"      \"CurrentHP\": 122700,\n" +
						"      \"MaxHP\": 122700,\n" +
						"      \"CurrentMP\": 10000,\n" +
						"      \"MaxMP\": 10000,\n" +
						"      \"PosX\": 114.926422,\n" +
						"      \"PosY\": -83.86734,\n" +
						"      \"PosZ\": 44.3433,\n" +
						"      \"Heading\": -1.66136408\n" +
						"    }\n" +
						"  ],\n" +
						"  \"rseq\": 0\n" +
						"}"
		));

		XivState state = container.getComponent(XivState.class);
		// TODO: still need better support for this...
		Thread.sleep(1000);
		Assert.assertEquals(state.getPartyList().size(), 8);
		Assert.assertEquals(state.getCombatantsListCopy().size(), 8);


		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		finalEvents.forEach(e -> log.info("Seen event: {}", e));

		List<Class<?>> actualEventClasses = finalEvents.stream().map(Event::getClass).collect(Collectors.toList());
		Assert.assertEquals(actualEventClasses, List.of(
				// Raw log line + parsed into AbilityUsedEvent
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				// Same
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				// Same
				ACTLogLineEvent.class, AbilityUsedEvent.class,
				// This represents the three players that got jailed, in no particular order
				UnsortedTitanJailsSolvedEvent.class,
				// Finally, we have the three players that got jailed, but sorted in whatever order (currently just alphabetical)
				FinalTitanJailsSolvedEvent.class,
				// Automarks
				AutoMarkRequest.class, AutoMarkRequest.class, AutoMarkRequest.class,
				// Personal callout since the player was one of the three
				CalloutEvent.class,
				// TTS
				TtsRequest.class

		));
		// For debugging purposes (or maybe even production purposes, who knows), every synthetic event also has its
		// parent tagged onto it.
		List<ACTLogLineEvent> logLines = collector.getEventsOf(ACTLogLineEvent.class);
		List<AbilityUsedEvent> abilityUsedEvents = collector.getEventsOf(AbilityUsedEvent.class);
		MatcherAssert.assertThat(
				abilityUsedEvents.stream().map(Event::getParent).collect(Collectors.toList()),
				Matchers.equalTo(logLines));

		// Chain of provenance continues down the rest of the events
		UnsortedTitanJailsSolvedEvent unsortedEvent = collector.getEventsOf(UnsortedTitanJailsSolvedEvent.class).get(0);
		Assert.assertEquals(unsortedEvent.getParent(), abilityUsedEvents.get(2));
		FinalTitanJailsSolvedEvent sortedEvent = collector.getEventsOf(FinalTitanJailsSolvedEvent.class).get(0);
		Assert.assertEquals(sortedEvent.getParent(), unsortedEvent);


		// Last event should be sorted jails
		List<? extends XivEntity> jailedPlayers = sortedEvent.getJailedPlayers();
		// Assert correct sort order
		Assert.assertEquals(jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.toList()), List.of("Random Person", "Some Guy", "Foo Bar"));

		List<AutoMarkRequest> automarks = collector.getEventsOf(AutoMarkRequest.class);
		Assert.assertEquals(automarks.stream().map(am -> am.getPlayerToMark().getName()).collect(Collectors.toList()), List.of("Random Person", "Some Guy", "Foo Bar"));

		List<CalloutEvent> callouts = collector.getEventsOf(CalloutEvent.class);
		Assert.assertEquals(callouts.size(), 1);
		Assert.assertEquals(callouts.get(0).getCallText(), "Third");

		List<TtsRequest> ttsEvents = collector.getEventsOf(TtsRequest.class);
		Assert.assertEquals(ttsEvents.size(), 1);
		Assert.assertEquals(ttsEvents.get(0).getTtsString(), "Third");
	}
}
