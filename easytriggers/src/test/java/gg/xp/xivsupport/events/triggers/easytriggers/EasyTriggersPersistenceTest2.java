package gg.xp.xivsupport.events.triggers.easytriggers;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.util.ReflectHelpers;
import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.FailedDeserializationTrigger;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.intellij.lang.annotations.Language;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.awt.*;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EasyTriggersPersistenceTest2 {

	// Test that we can import existing triggers
	@Test
	void legacyPersistenceTestOld() throws JacksonException {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		pers.save("easy-triggers.my-triggers", triggerDataOldLegacy);

		MutablePicoContainer pico = ExampleSetup.setup(pers);

		EasyTriggers ez = pico.getComponent(EasyTriggers.class);
		MatcherAssert.assertThat(ez.getChildTriggers(), Matchers.hasSize(2));

		XivState state = pico.getComponent(XivState.class);
		XivPlayerCharacter someRandomPartyMember = state.getPartyList().get(1);
		XivCombatant someOtherCombatant = new XivCombatant(0x1000_4444, "Other Player");

		EventDistributor dist = pico.getComponent(EventDistributor.class);

		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		dist.acceptEvent(new AbilityCastStart(new XivAbility(172), someRandomPartyMember, someOtherCombatant, 5.0));

		{
			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(0));
		}

		{
			AtomicInteger elapsedTime = new AtomicInteger(0);

			dist.acceptEvent(new AbilityCastStart(new XivAbility(173), someRandomPartyMember, someOtherCombatant, 5.0) {
				@Override
				public Duration getEffectiveTimeSince() {
					return Duration.ofMillis(elapsedTime.get());
				}
			});

			// TODO: check UUIDs

			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(1));
			CalloutEvent theTtsEvent = callouts.get(0);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (5.0)"));
			MatcherAssert.assertThat(theTtsEvent.getColorOverride(), Matchers.equalTo(new Color(204, 0, 102, 196)));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3400);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (1.6)"));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(6233);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (NOW)"));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(6235);
			Assert.assertTrue(theTtsEvent.isExpired());
		}

		coll.clear();

		{
			AtomicInteger elapsedTime = new AtomicInteger(0);
			dist.acceptEvent(new AbilityUsedEvent(new XivAbility(173), someRandomPartyMember, someOtherCombatant, Collections.emptyList(), 1234, 0, 1) {
				@Override
				public Duration getEffectiveTimeSince() {
					return Duration.ofMillis(elapsedTime.get());
				}
			});
			// TODO: check UUIDs

			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(1));
			CalloutEvent theTtsEvent = callouts.get(0);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getColorOverride(), Matchers.equalTo(new Color(153, 255, 0, 255)));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3330);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3334);
			Assert.assertTrue(theTtsEvent.isExpired());

		}
		Assert.assertNotNull(ReflectHelpers.reflectionGetField(((EasyTrigger) ez.getChildTriggers().get(0)).getActions().get(0), "uuid"));
		Assert.assertNotNull(ReflectHelpers.reflectionGetField(((EasyTrigger) ez.getChildTriggers().get(1)).getActions().get(0), "uuid"));
		// Now, fake these so we can do a comparison. Not dumb if it works.
		ReflectHelpers.reflectionSetField(((EasyTrigger<?>) ez.getChildTriggers().get(0)).getActions().get(0), "uuid", UUID.fromString("883ecb35-8324-411b-9d0f-cd131de42a57"));
		ReflectHelpers.reflectionSetField(((EasyTrigger<?>) ez.getChildTriggers().get(1)).getActions().get(0), "uuid", UUID.fromString("4fcdcfb5-4f74-4a49-b425-2faf7460caae"));

		ObjectMapper mapper = new ObjectMapper();
		MatcherAssert.assertThat(mapper.readTree(ez.exportToString(ez.getChildTriggers())), Matchers.equalTo(mapper.readTree(triggerDataNew)));
		// TODO: check migration flags
	}

	@Test
	void legacyPersistenceTest() throws JacksonException {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		pers.save("easy-triggers.my-triggers", triggerDataLegacy);

		MutablePicoContainer pico = ExampleSetup.setup(pers);

		EasyTriggers ez = pico.getComponent(EasyTriggers.class);
		MatcherAssert.assertThat(ez.getChildTriggers(), Matchers.hasSize(2));

		XivState state = pico.getComponent(XivState.class);
		XivPlayerCharacter someRandomPartyMember = state.getPartyList().get(1);
		XivCombatant someOtherCombatant = new XivCombatant(0x1000_4444, "Other Player");

		EventDistributor dist = pico.getComponent(EventDistributor.class);

		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		dist.acceptEvent(new AbilityCastStart(new XivAbility(172), someRandomPartyMember, someOtherCombatant, 5.0));

		{
			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(0));
		}

		{
			AtomicInteger elapsedTime = new AtomicInteger(0);

			dist.acceptEvent(new AbilityCastStart(new XivAbility(173), someRandomPartyMember, someOtherCombatant, 5.0) {
				@Override
				public Duration getEffectiveTimeSince() {
					return Duration.ofMillis(elapsedTime.get());
				}
			});

			// TODO: check UUIDs

			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(1));
			CalloutEvent theTtsEvent = callouts.get(0);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (5.0)"));
			MatcherAssert.assertThat(theTtsEvent.getColorOverride(), Matchers.equalTo(new Color(204, 0, 102, 196)));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3400);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (1.6)"));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(6233);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (NOW)"));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(6235);
			Assert.assertTrue(theTtsEvent.isExpired());
		}

		coll.clear();

		{
			AtomicInteger elapsedTime = new AtomicInteger(0);
			dist.acceptEvent(new AbilityUsedEvent(new XivAbility(173), someRandomPartyMember, someOtherCombatant, Collections.emptyList(), 1234, 0, 1) {
				@Override
				public Duration getEffectiveTimeSince() {
					return Duration.ofMillis(elapsedTime.get());
				}
			});
			// TODO: check UUIDs

			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(1));
			CalloutEvent theTtsEvent = callouts.get(0);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getColorOverride(), Matchers.equalTo(new Color(153, 255, 0, 255)));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3330);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3334);
			Assert.assertTrue(theTtsEvent.isExpired());

		}
		Assert.assertEquals(ReflectHelpers.reflectionGetField(((EasyTrigger) ez.getChildTriggers().get(0)).getActions().get(0), "uuid"), UUID.fromString("883ecb35-8324-411b-9d0f-cd131de42a57"));
		Assert.assertEquals(ReflectHelpers.reflectionGetField(((EasyTrigger) ez.getChildTriggers().get(1)).getActions().get(0), "uuid"), UUID.fromString("4fcdcfb5-4f74-4a49-b425-2faf7460caae"));
		// Now, fake these so we can do a comparison. Not dumb if it works.

		ObjectMapper mapper = new ObjectMapper();
		MatcherAssert.assertThat(mapper.readTree(ez.exportToString(ez.getChildTriggers())), Matchers.equalTo(mapper.readTree(triggerDataNew)));
		// TODO: check migration flags
	}

	@Test
	void newPersistenceTest() throws JacksonException {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		pers.save("easy-triggers.my-triggers-2", triggerDataNew);

		MutablePicoContainer pico = ExampleSetup.setup(pers);

		EasyTriggers ez = pico.getComponent(EasyTriggers.class);
		MatcherAssert.assertThat(ez.getChildTriggers(), Matchers.hasSize(2));

		XivState state = pico.getComponent(XivState.class);
		XivPlayerCharacter someRandomPartyMember = state.getPartyList().get(1);
		XivCombatant someOtherCombatant = new XivCombatant(0x1000_4444, "Other Player");

		EventDistributor dist = pico.getComponent(EventDistributor.class);

		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		dist.acceptEvent(new AbilityCastStart(new XivAbility(172), someRandomPartyMember, someOtherCombatant, 5.0));

		{
			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(0));
		}

		{
			AtomicInteger elapsedTime = new AtomicInteger(0);

			dist.acceptEvent(new AbilityCastStart(new XivAbility(173), someRandomPartyMember, someOtherCombatant, 5.0) {
				@Override
				public Duration getEffectiveTimeSince() {
					return Duration.ofMillis(elapsedTime.get());
				}
			});

			// TODO: check UUIDs

			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(1));
			CalloutEvent theTtsEvent = callouts.get(0);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (5.0)"));
			MatcherAssert.assertThat(theTtsEvent.getColorOverride(), Matchers.equalTo(new Color(204, 0, 102, 196)));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3400);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (1.6)"));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(6233);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " is raising " + someOtherCombatant.getName() + " (NOW)"));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(6235);
			Assert.assertTrue(theTtsEvent.isExpired());
		}

		coll.clear();

		{
			AtomicInteger elapsedTime = new AtomicInteger(0);
			dist.acceptEvent(new AbilityUsedEvent(new XivAbility(173), someRandomPartyMember, someOtherCombatant, Collections.emptyList(), 1234, 0, 1) {
				@Override
				public Duration getEffectiveTimeSince() {
					return Duration.ofMillis(elapsedTime.get());
				}
			});
			// TODO: check UUIDs

			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			MatcherAssert.assertThat(callouts.size(), Matchers.equalTo(1));
			CalloutEvent theTtsEvent = callouts.get(0);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getColorOverride(), Matchers.equalTo(new Color(153, 255, 0, 255)));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3330);
			MatcherAssert.assertThat(theTtsEvent.getCallText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			MatcherAssert.assertThat(theTtsEvent.getVisualText(), Matchers.equalTo(someRandomPartyMember.getName() + " just raised " + someOtherCombatant.getName()));
			Assert.assertFalse(theTtsEvent.isExpired());
			elapsedTime.set(3334);
			Assert.assertTrue(theTtsEvent.isExpired());

		}
		Assert.assertEquals(ReflectHelpers.reflectionGetField(((EasyTrigger) ez.getChildTriggers().get(0)).getActions().get(0), "uuid"), UUID.fromString("883ecb35-8324-411b-9d0f-cd131de42a57"));
		Assert.assertEquals(ReflectHelpers.reflectionGetField(((EasyTrigger) ez.getChildTriggers().get(1)).getActions().get(0), "uuid"), UUID.fromString("4fcdcfb5-4f74-4a49-b425-2faf7460caae"));
		// Now, fake these so we can do a comparison. Not dumb if it works.

		ObjectMapper mapper = new ObjectMapper();
		MatcherAssert.assertThat(mapper.readTree(ez.exportToString(ez.getChildTriggers())), Matchers.equalTo(mapper.readTree(triggerDataNew)));
	}

	@Test
	void newPersistenceTestWithFail() throws JsonProcessingException {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		pers.save("easy-triggers.my-triggers-2", triggerDataNewWithNonexistentCondition);

		MutablePicoContainer pico = ExampleSetup.setup(pers);

		EasyTriggers ez = pico.getComponent(EasyTriggers.class);
		MatcherAssert.assertThat(ez.getChildTriggers(), Matchers.hasSize(2));
		MatcherAssert.assertThat(ez.getChildTriggers().get(0), Matchers.instanceOf(EasyTrigger.class));
		MatcherAssert.assertThat(ez.getChildTriggers().get(0).getName(), Matchers.equalTo("Rez Start"));
		MatcherAssert.assertThat(ez.getChildTriggers().get(1), Matchers.instanceOf(FailedDeserializationTrigger.class));
	}

	@Language("JSON")
	private static final String triggerDataOldLegacy = """
			[
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "name": "Rez Start",
			    "tts": "{event.source} is raising {event.target}",
			    "text": "{event.source} is raising {event.target} ({event.estimatedRemainingDuration})",
			    "colorRaw": -993263514,
			    "hangTime": 1234,
			    "useDuration": true,
			    "useIcon": true
			  },
			  {
			    "enabled": true,
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "name": "Give me a name",
			    "tts": "{event.source} just raised {event.target}",
			    "text": "{event.source} just raised {event.target}",
			    "colorRaw": -6684928,
			    "hangTime": 3333,
			    "useDuration": true,
			    "useIcon": true
			  }
			]
			""";

	private static final String triggerDataLegacy = """
			[
			  {
			    "enabled": true,
			    "concurrency": "BLOCK_NEW",
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.DurationBasedCalloutAction",
			        "tts": "{event.source} is raising {event.target}",
			        "text": "{event.source} is raising {event.target} ({event.estimatedRemainingDuration})",
			        "colorRaw": -993263514,
			        "plusDuration": true,
			        "hangTime": 1234,
			        "useIcon": true,
			        "uuid": "883ecb35-8324-411b-9d0f-cd131de42a57"
			      }
			    ],
			    "name": "Rez Start"
			  },
			  {
			    "enabled": true,
			    "concurrency": "BLOCK_NEW",
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction",
			        "tts": "{event.source} just raised {event.target}",
			        "text": "{event.source} just raised {event.target}",
			        "colorRaw": -6684928,
			        "hangTime": 3333,
			        "useIcon": true,
			        "uuid": "4fcdcfb5-4f74-4a49-b425-2faf7460caae"
			      }
			    ],
			    "name": "Give me a name"
			  }
						]
			""";

	private static final String triggerDataNew = """
			[
			  {
			    "type": "trigger",
			    "enabled": true,
			    "name": "Rez Start",
			    "concurrency": "BLOCK_NEW",
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.DurationBasedCalloutAction",
			        "tts": "{event.source} is raising {event.target}",
			        "text": "{event.source} is raising {event.target} ({event.estimatedRemainingDuration})",
			        "colorRaw": -993263514,
			        "plusDuration": true,
			        "hangTime": 1234,
			        "useIcon": true,
			        "uuid": "883ecb35-8324-411b-9d0f-cd131de42a57"
			      }
			    ]
			  },
			  {
			    "type": "trigger",
			    "enabled": true,
			    "name": "Give me a name",
			    "concurrency": "BLOCK_NEW",
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction",
			        "tts": "{event.source} just raised {event.target}",
			        "text": "{event.source} just raised {event.target}",
			        "colorRaw": -6684928,
			        "hangTime": 3333,
			        "useIcon": true,
			        "uuid": "4fcdcfb5-4f74-4a49-b425-2faf7460caae"
			      }
			    ]
			  }
			]""";

	private static final String triggerDataNewWithNonexistentCondition = """
			[
			  {
			    "type": "trigger",
			    "enabled": true,
			    "name": "Rez Start",
			    "concurrency": "BLOCK_NEW",
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityCastStart",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.SourcePartyMemberFilter",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.DurationBasedCalloutAction",
			        "tts": "{event.source} is raising {event.target}",
			        "text": "{event.source} is raising {event.target} ({event.estimatedRemainingDuration})",
			        "colorRaw": -993263514,
			        "plusDuration": true,
			        "hangTime": 1234,
			        "useIcon": true,
			        "uuid": "883ecb35-8324-411b-9d0f-cd131de42a57"
			      }
			    ]
			  },
			  {
			    "type": "trigger",
			    "enabled": false,
			    "name": "Trigger which fails",
			    "concurrency": "BLOCK_NEW",
			    "eventType": "gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent",
			    "conditions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter",
			        "operator": "EQ",
			        "expected": 173
			      },
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.conditions.NonExistent",
			        "invert": false
			      }
			    ],
			    "actions": [
			      {
			        "@class": "gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction",
			        "tts": "{event.source} just raised {event.target}",
			        "text": "{event.source} just raised {event.target}",
			        "colorRaw": -6684928,
			        "hangTime": 3333,
			        "useIcon": true,
			        "uuid": "4fcdcfb5-4f74-4a49-b425-2faf7460caae"
			      }
			    ]
			  }
			]""";
}
