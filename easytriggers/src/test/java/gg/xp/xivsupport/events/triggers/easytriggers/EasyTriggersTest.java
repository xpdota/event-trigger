package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.Event;
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
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyEventFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class EasyTriggersTest {

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


	// TODO: why doesn't parallel work here? Everything should have its own instances, so thread safety should be
	// completely irrelevant.
	@DataProvider(parallel = false)
	private Object[] testCases() {
		return new TestCase[]{
				new TestCase<>(castStart, AbilityCastStart.class, "Foo Ability (4.5)", "Foo Ability"),
				new TestCase<>(abilityUsed1, AbilityUsedEvent.class, "Foo Ability", "Foo Ability"),
				new TestCase<>(resolved, AbilityResolvedEvent.class, "Foo Ability resolved", "Foo Ability resolved"),
				new TestCase<>(castCancel, AbilityCastCancel.class, "Foo Ability interrupted", "Foo Ability interrupted"),
				new TestCase<>(killed, EntityKilledEvent.class, "Target died", "Target died"),
				new TestCase<>(buffApply, BuffApplied.class, "Foo Status on Target", "Foo Status on Target"),
				new TestCase<>(buffRemove, BuffRemoved.class, "Foo Status lost from Target", "Foo Status lost from Target"),
				new TestCase<>(ace, ActorControlEvent.class, "Actor control 2", "Actor control 2"),
				new TestCase<>(logLine, ACTLogLineEvent.class, "Log Line 22", "Log Line 22"),
		};
	}

	private record TestCase<X extends Event>(
			X event,
			Class<X> type,
			String expectedText,
			String expectedTts
	) {
	}

	@Test(dataProvider = "testCases")
	@Parameters
	<X extends Event> void defaultCallouts(TestCase<X> testCase) {
		PersistenceProvider pers;
		{
			MutablePicoContainer pico = ExampleSetup.setup();
			pers = pico.getComponent(PersistenceProvider.class);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
			EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
			EasyTrigger<X> easy = ez1.getEventDescription(testCase.type()).newEmptyInst();
			ez1.addTrigger(easy);

			dist.acceptEvent(testCase.event());
			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 1);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), testCase.expectedText());
				Assert.assertEquals(theCall.getCallText(), testCase.expectedTts());
			}
		}
		// Now load the serialized version and make sure it all still works

		{
			MutablePicoContainer pico = ExampleSetup.setup(pers);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);

			dist.acceptEvent(testCase.event());
			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 1);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), testCase.expectedText());
				Assert.assertEquals(theCall.getCallText(), testCase.expectedTts());
			}
		}


	}

	@Test
	void simpleTest() {
		PersistenceProvider pers;
		{
			MutablePicoContainer pico = ExampleSetup.setup();
			pers = pico.getComponent(PersistenceProvider.class);
			TestEventCollector coll = new TestEventCollector();
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
			EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
			EasyTrigger<AbilityUsedEvent> trig1 = new EasyTrigger<>();
			AbilityIdFilter cond = new AbilityIdFilter();
			cond.operator = NumericOperator.EQ;
			cond.expected = 123;
			trig1.setEventType(AbilityUsedEvent.class);
			trig1.addCondition(cond);
			trig1.setText("{event.getAbility().getId()}");
			trig1.setTts("{event.getAbility().getId()}");
			ez1.addTrigger(trig1);

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
	void testGroovyTrigger() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("event.line.startsWith('foo')");
		Assert.assertTrue(gef.test(new EchoEvent("foobar")));
		Assert.assertFalse(gef.test(new EchoEvent("notbar")));
	}

	@Test
	void testGroovyTriggerBadSyntax() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("asdfasdfasdfasdf");
		Assert.assertFalse(gef.test(new EchoEvent("notbar")));
		MatcherAssert.assertThat(gef.getLastError(), Matchers.instanceOf(MissingPropertyException.class));
	}

	// TODO: broken
	@Test
	@Ignore
	void testGroovyTriggerBadSyntaxStrict() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setStrict(true);
		// Due to the way this is set up, we have to set the script to something valid, *then* something invalid
		gef.setGroovyScript("true");
		Assert.assertThrows(MultipleCompilationErrorsException.class, () -> gef.setGroovyScript("asdfasdfasdfasdf"));
	}

	@Test
	void testGroovyTriggerBadSyntaxStrictBuggyWrongBehavior() {
		// TODO: this is testing wrong behavior to make sure that it's not TOO wrong
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setStrict(true);
		// Due to the way this is set up, we have to set the script to something valid, *then* something invalid
		gef.setGroovyScript("true");
		gef.setGroovyScript("asdfasdfasdfasdf");
		Assert.assertFalse(gef.test(new EchoEvent("notbar")));
		MatcherAssert.assertThat(gef.getLastError(), Matchers.instanceOf(MissingPropertyException.class));
	}

	@Test
	void testGroovyTriggerSandboxViolation() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("new java.io.File(\"Foo\") != null");
		Assert.assertFalse(gef.test(new EchoEvent("notbar")));
		MatcherAssert.assertThat(gef.getLastError(), Matchers.instanceOf(RejectedAccessException.class));
	}

	private static MutablePicoContainer getSbxDisabledSetup() {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		pers.save("groovy.enable-sandbox", false);
		return ExampleSetup.setup(pers);
	}

	@Test
	void testGroovyTriggerNoSbx() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("event.line.startsWith('foo')");
		Assert.assertTrue(gef.test(new EchoEvent("foobar")));
		Assert.assertFalse(gef.test(new EchoEvent("notbar")));
	}

	@Test
	void testGroovyTriggerBadSyntaxNoSbx() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("asdfasdfasdfasdf");
		Assert.assertFalse(gef.test(new EchoEvent("notbar")));
		MatcherAssert.assertThat(gef.getLastError(), Matchers.instanceOf(MissingPropertyException.class));
	}

	@Test
	void testGroovyTriggerBadSyntaxStrictNoSbx() {
		MutablePicoContainer pico = getSbxDisabledSetup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setStrict(true);
		// Due to the way this is set up, we have to set the script to something valid, *then* something invalid
		gef.setGroovyScript("true");
		Assert.assertThrows(IllegalArgumentException.class, () -> gef.setGroovyScript("asdfasdfasdfasdf"));
	}

	@Test
	void testGroovyTriggerSandboxViolationNoSbx() {
		MutablePicoContainer pico = getSbxDisabledSetup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("new java.io.File(\"Foo\") != null");
		Assert.assertTrue(gef.test(new EchoEvent("foobar")));
	}
}