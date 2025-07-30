package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.ZoneIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class EasyTriggersNestedTest {

	private static final XivCombatant caster = new XivCombatant(10, "Caster");
	private static final XivAbility matchingAbility = new XivAbility(123, "Foo Ability");
	private static final XivAbility otherAbility = new XivAbility(456, "Bar Ability");
	private static final XivCombatant target = new XivCombatant(11, "Target");
	private static final XivStatusEffect status = new XivStatusEffect(123, "Foo Status");
	private static final AbilityUsedEvent abilityUsed1 = new AbilityUsedEvent(matchingAbility, caster, target, Collections.emptyList(), 123, 0, 1);
	private static final AbilityUsedEvent abilityUsed2 = new AbilityUsedEvent(otherAbility, caster, target, Collections.emptyList(), 123, 0, 1);
	private static final ZoneChangeEvent zoneChange = new ZoneChangeEvent(new XivZone(987, "Some Zone"));
	private static final AbilityCastStart castStart = new AbilityCastStart(matchingAbility, caster, target, 5.0) {
		// Override this to keep it a fixed time for the test
		@Override
		public Duration getEstimatedRemainingDuration() {
			return Duration.ofMillis(4500);
		}
	};
	private static final AbilityCastCancel castCancel = new AbilityCastCancel(caster, matchingAbility, "Stuff");
	private static final EntityKilledEvent killed = new EntityKilledEvent(caster, target);
	private static final BuffApplied buffApply = new BuffApplied(status, 5.0, caster, target, 5);
	private static final BuffRemoved buffRemove = new BuffRemoved(status, 5.0, caster, target, 5);
	private static final AbilityResolvedEvent resolved = new AbilityResolvedEvent(abilityUsed1);
	private static final ActorControlEvent ace = new ActorControlEvent(1, 2, 3, 4, 5, 6);
	private static final ACTLogLineEvent logLine = new ACTLogLineEvent("22|2022-01-20T18:11:59.9720000-08:00|40031036|Sparkfledged|66E6|Ashen Eye|10679943|Player Name|3|967F4098|1B|66E68000|0|0|0|0|0|0|0|0|0|0|0|0|61186|61186|10000|10000|||100.14|91.29|-0.02|3.08|69200|69200|10000|10000|||100.11|106.76|0.00|3.14|000133E7|2|4|64f5cd5254f9f411");

	@Test
	void simpleNestedTest() {
		PersistenceProvider pers;
		{
			MutablePicoContainer pico = ExampleSetup.setup();
			pers = pico.getComponent(PersistenceProvider.class);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);

			EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);

			TriggerFolder folder1 = new TriggerFolder();
			TriggerFolder folder2 = new TriggerFolder();
			ez1.addTrigger(folder1, folder2);
			// TODO: test adding in different orders
			ez1.addTrigger(null, folder1);

			EasyTrigger<AbilityUsedEvent> trig1 = new EasyTrigger<>();

			// Add a condition
			AbilityIdFilter cond = new AbilityIdFilter();
			cond.operator = NumericOperator.EQ;
			cond.expected = 123;
			trig1.setEventType(AbilityUsedEvent.class);
			trig1.addCondition(cond);

			// Add an action
			CalloutAction call = new CalloutAction();
			call.setText("{event.getAbility().getId()}");
			call.setTts("{event.getAbility().getId()}");
			trig1.addAction(call);

			ez1.addTrigger(folder2, trig1);

			MatcherAssert.assertThat(ez1.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder firstFolder = (TriggerFolder) ez1.getChildTriggers().get(0);
			MatcherAssert.assertThat(firstFolder.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder secondFolder = (TriggerFolder) firstFolder.getChildTriggers().get(0);
			MatcherAssert.assertThat(secondFolder.getChildTriggers(), Matchers.hasSize(1));
			Assert.assertEquals(secondFolder.getChildTriggers().get(0), trig1);

			dist.acceptEvent(abilityUsed2);
			dist.acceptEvent(zoneChange);
			dist.acceptEvent(abilityUsed1);

			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 1);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), "123");
				Assert.assertEquals(theCall.getCallText(), "123");
			}
		}
		// Now load the serialized version and make sure it all still works

		{
			MutablePicoContainer pico = ExampleSetup.setup(pers);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);

			EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
			// Check deserialization
			MatcherAssert.assertThat(ez1.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder firstFolder = (TriggerFolder) ez1.getChildTriggers().get(0);
			MatcherAssert.assertThat(firstFolder.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder secondFolder = (TriggerFolder) firstFolder.getChildTriggers().get(0);
			MatcherAssert.assertThat(secondFolder.getChildTriggers(), Matchers.hasSize(1));
//			Assert.assertEquals(secondFolder.getChildTriggers().get(0), trig1);

			dist.acceptEvent(abilityUsed2);
			dist.acceptEvent(zoneChange);
			dist.acceptEvent(abilityUsed1);

			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 1);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), "123");
				Assert.assertEquals(theCall.getCallText(), "123");
			}
		}
	}

	@Test
	void parentConditionNestedTest() {
		PersistenceProvider pers;
		{
			MutablePicoContainer pico = ExampleSetup.setup();
			pers = pico.getComponent(PersistenceProvider.class);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);

			EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);

			TriggerFolder folder1 = new TriggerFolder();
			TriggerFolder folder2 = new TriggerFolder();
			ez1.addTrigger(folder1, folder2);
			// TODO: test adding in different orders
			ez1.addTrigger(null, folder1);

			EasyTrigger<AbilityUsedEvent> trig1 = new EasyTrigger<>();

			// Add a condition
			AbilityIdFilter cond = new AbilityIdFilter();
			cond.operator = NumericOperator.EQ;
			cond.expected = 123;
			trig1.setEventType(AbilityUsedEvent.class);
			trig1.addCondition(cond);

			// Add an action
			CalloutAction call = new CalloutAction();
			call.setText("{event.getAbility().getId()}");
			call.setTts("{event.getAbility().getId()}");
			trig1.addAction(call);

			ZoneIdFilter condition = new ZoneIdFilter(pico.getComponent(XivState.class));
			condition.expected = 987;
			condition.operator = NumericOperator.EQ;
			folder1.addCondition(condition);

			ez1.addTrigger(folder2, trig1);


			MatcherAssert.assertThat(ez1.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder firstFolder = (TriggerFolder) ez1.getChildTriggers().get(0);
			MatcherAssert.assertThat(firstFolder.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder secondFolder = (TriggerFolder) firstFolder.getChildTriggers().get(0);
			MatcherAssert.assertThat(secondFolder.getChildTriggers(), Matchers.hasSize(1));
			Assert.assertEquals(secondFolder.getChildTriggers().get(0), trig1);

			dist.acceptEvent(abilityUsed1);
			dist.acceptEvent(abilityUsed1);
			dist.acceptEvent(zoneChange);
			dist.acceptEvent(abilityUsed1);
			dist.acceptEvent(abilityUsed1);

			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 2);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), "123");
				Assert.assertEquals(theCall.getCallText(), "123");
				CalloutEvent theCall2 = calls.get(0);
				Assert.assertEquals(theCall2.getVisualText(), "123");
				Assert.assertEquals(theCall2.getCallText(), "123");
			}
		}
		// Now load the serialized version and make sure it all still works

		{
			MutablePicoContainer pico = ExampleSetup.setup(pers);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);

			EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
			// Check deserialization
			MatcherAssert.assertThat(ez1.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder firstFolder = (TriggerFolder) ez1.getChildTriggers().get(0);
			MatcherAssert.assertThat(firstFolder.getChildTriggers(), Matchers.hasSize(1));
			TriggerFolder secondFolder = (TriggerFolder) firstFolder.getChildTriggers().get(0);
			MatcherAssert.assertThat(secondFolder.getChildTriggers(), Matchers.hasSize(1));
//			Assert.assertEquals(secondFolder.getChildTriggers().get(0), trig1);

			dist.acceptEvent(abilityUsed1);
			dist.acceptEvent(abilityUsed1);
			dist.acceptEvent(zoneChange);
			dist.acceptEvent(abilityUsed1);
			dist.acceptEvent(abilityUsed1);

			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 2);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), "123");
				Assert.assertEquals(theCall.getCallText(), "123");
				CalloutEvent theCall2 = calls.get(0);
				Assert.assertEquals(theCall2.getVisualText(), "123");
				Assert.assertEquals(theCall2.getCallText(), "123");
			}
		}
	}
}
