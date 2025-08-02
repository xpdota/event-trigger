package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class EasyTriggersPersistenceTest {

	// Test that we can import existing triggers
	@Test
	void legacyPersistenceTest() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(PersistenceProvider.class).save("easy-triggers.my-triggers", triggerData);
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());

		EasyTriggers et = pico.getComponent(EasyTriggers.class);
		Assert.assertEquals(et.getChildTriggers().size(), 7);

		EventDistributor dist = pico.getComponent(EventDistributor.class);

		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":22,\"charName\":\"Foo Bar\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":777,\"zoneName\":\"the Weapon's Refrain (Ultimate)\"}"));
		// This player should be sorted first because they are the actual player
		dist.acceptEvent(new ActWsRawMsg(
				"""
						{
						  "combatants": [
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 22,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 24,
						      "Level": 80,
						      "Name": "Foo Bar",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    }
						  ],
						  "rseq": 0
						}"""
		));


		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		dist.acceptEvent(new ChatLineEvent(0, "", "Battle commencing in 15 seconds"));

		MatcherAssert.assertThat(coll.getEventsOf(TtsRequest.class), Matchers.empty());

		dist.acceptEvent(new ActWsRawMsg(
				"""
						{
						  "combatants": [
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 22,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 39,
						      "Level": 80,
						      "Name": "Foo Bar",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    }
						  ],
						  "rseq": 0
						}"""
		));


		dist.acceptEvent(new ChatLineEvent(0, "", "Battle commencing in 15 seconds"));

		List<TtsRequest> ttsEventsAfter = coll.getEventsOf(TtsRequest.class);
		MatcherAssert.assertThat(ttsEventsAfter.size(), Matchers.equalTo(1));
		TtsRequest theTtsEvent = ttsEventsAfter.get(0);
		MatcherAssert.assertThat(theTtsEvent.getTtsString(), Matchers.equalTo("Use Soulsow"));


	}




	private static final String triggerData = """
			[
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 11077
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityTypeFilter",
			        "type": "NPC_REAL"
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityTypeFilter",
			        "type": "ANY_PLAYER"
			      }
			    ],
			    "name": "Spiny Plume",
			    "tts": "Spiny Plume Attacked",
			    "text": "Spiny Plume Attacked {event.getTarget()}"
			  },
			  {
			    "enabled": false,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 11077
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityTypeFilter",
			        "type": "NPC_REAL"
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityTypeFilter",
			        "type": "ANY_PLAYER"
			      }
			    ],
			    "name": "Spiny Plume",
			    "tts": "Spiny Plume Attacked",
			    "text": "Spiny Plume Attacked"
			  },
			  {
			    "enabled": false,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.ActorControlEvent",
			    "conditions": [],
			    "name": "Give me a name",
			    "tts": "Actor control {event.command}",
			    "text": "Actor control {event.command}"
			  },
			  {
			    "enabled": false,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityTypeFilter",
			        "type": "THE_PLAYER"
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityTypeFilter",
			        "type": "NPC"
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 24312
			      }
			    ],
			    "name": "Dosis III casting",
			    "tts": "{event.ability}",
			    "text": "{event.ability} ({event.estimatedRemainingDuration})"
			  },
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourceEntityNpcIdFilter",
			        "operator": "EQ",
			        "expected": 13824
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.TargetEntityTypeFilter",
			        "type": "ANY_PLAYER"
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 27179
			      }
			    ],
			    "name": "Heart Stake casting",
			    "tts": "{event.ability}",
			    "text": "{event.ability} ({event.estimatedRemainingDuration})"
			  },
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityNameFilter",
			        "operator": "EQ",
			        "localLanguage": false,
			        "caseSensitive": false,
			        "expected": null
			      }
			    ],
			    "name": "Give me a name",
			    "tts": "{event.ability}",
			    "text": "{event.ability}"
			  },
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.ChatLineEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.ChatLineRegexFilter",
			        "regex": "Battle commencing in (\\\\d+) seconds"
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyEventFilter",
			        "groovyScript": "xivState.player.job == Job.RPR ",
			        "strict": false,
			        "eventType": "gg.xp.xivsupport.events.actlines.events.ChatLineEvent"
			      }
			    ],
			    "name": "Soulsow",
			    "tts": "Use Soulsow",
			    "text": "Use Soulsow"
			  }
						]
						""";

}
