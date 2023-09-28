package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeTimeSource;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.GroovyAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.WaitBuffDurationAction;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.WaitCastDurationAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.ChatLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyEventFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class EasyTriggersTest {

	private static final Logger log = LoggerFactory.getLogger(EasyTriggersTest.class);

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
	// Probably because they share instances of the example events...
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

	@BeforeMethod
	void prio() {
		log.info("Lowering thread priority");
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
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
			EasyTrigger<X> easy = ez1.getEventDescription(testCase.type()).newEmptyInst(null);
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
			CalloutAction call = new CalloutAction();
			call.setText("{event.getAbility().getId()}");
			call.setTts("{event.getAbility().getId()}");
			trig1.addAction(call);
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
	void extraVarsTest() {
		MutablePicoContainer pico = ExampleSetup.setup();
		TestEventCollector coll = new TestEventCollector();
		EventMaster master = pico.getComponent(EventMaster.class);
		{
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
		}
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<AbilityUsedEvent> trig1 = new EasyTrigger<>();
		AbilityIdFilter cond = new AbilityIdFilter();
		cond.operator = NumericOperator.EQ;
		cond.expected = 123;
		trig1.setEventType(AbilityUsedEvent.class);
		trig1.addCondition(cond);
		GroovyManager groovy = pico.getComponent(GroovyManager.class);
		GroovyAction action = new GroovyAction(groovy);
		action.setGroovyScript("s.waitMs(400); s.accept(new TtsRequest(String.valueOf(context instanceof gg.xp.reevent.events.EventContext))); globals.foo = 'bar'");
		trig1.addAction(action);
		ez1.addTrigger(trig1);

		master.pushEventAndWait(abilityUsed1);
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		try {
			Thread.sleep(500);
			master.pushEventAndWait(new AbilityUsedEvent(otherAbility, caster, target, Collections.emptyList(), 123, 0, 1));
			Thread.sleep(100);
			// Workaround due to slowness...
			for (int i = 0; i < 10; i++) {
				Thread.sleep(2000);
				if (!coll.getEventsOf(TtsRequest.class).isEmpty()) {
					break;
				}
				master.pushEventAndWait(new BaseEvent() {
				});
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		{
			log.info("Done");
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 1);
			TtsRequest theCall = calls.get(0);
			Assert.assertEquals(theCall.getTtsString(), "true");
			Assert.assertEquals(groovy.makeBinding().getVariable("foo"), "bar");
		}
		// Now load the serialized version and make sure it all still works

	}

	@Test
	void extraVarsTestFakeTimeSource() {
		MutablePicoContainer pico = ExampleSetup.setup();
		pico.getComponent(FakeTimeSource.class);
		TestEventCollector coll = new TestEventCollector();
		EventMaster master = pico.getComponent(EventMaster.class);
		{
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
		}
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<AbilityUsedEvent> trig1 = new EasyTrigger<>();
		AbilityIdFilter cond = new AbilityIdFilter();
		cond.operator = NumericOperator.EQ;
		cond.expected = 123;
		trig1.setEventType(AbilityUsedEvent.class);
		trig1.addCondition(cond);
		GroovyManager groovy = pico.getComponent(GroovyManager.class);
		GroovyAction action = new GroovyAction(groovy);
		action.setGroovyScript("s.waitMs(400); s.accept(new TtsRequest(String.valueOf(context instanceof gg.xp.reevent.events.EventContext))); globals.foo = 'bar'");
		trig1.addAction(action);
		ez1.addTrigger(trig1);

		FakeTimeSource fts = new FakeTimeSource();
		Instant now = Instant.now();
		fts.setNewTime(now);
		AbilityUsedEvent abilityUsed1 = new AbilityUsedEvent(matchingAbility, caster, target, Collections.emptyList(), 123, 0, 1);
		abilityUsed1.setTimeSource(fts);
		abilityUsed1.setHappenedAt(fts.now());
		master.pushEventAndWait(abilityUsed1);
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		AbilityUsedEvent abilityUsed2 = new AbilityUsedEvent(otherAbility, caster, target, Collections.emptyList(), 123, 0, 1);
		fts.setNewTime(now.plusMillis(500));
		abilityUsed2.setHappenedAt(fts.now());
		abilityUsed2.setTimeSource(fts);
		master.pushEventAndWait(abilityUsed2);

		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 1);
			TtsRequest theCall = calls.get(0);
			Assert.assertEquals(theCall.getTtsString(), "true");
			Assert.assertEquals(groovy.makeBinding().getVariable("foo"), "bar");
		}

	}

	@Test
	void testGroovyTrigger() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("event.line.startsWith('foo')");
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertTrue(gef.test(etc, new EchoEvent("foobar")));
		Assert.assertFalse(gef.test(etc, new EchoEvent("notbar")));
	}

	@Test
	void testGroovyTriggerBadSyntax() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("asdfasdfasdfasdf");
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertFalse(gef.test(etc, new EchoEvent("notbar")));
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
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertFalse(gef.test(etc, new EchoEvent("notbar")));
		MatcherAssert.assertThat(gef.getLastError(), Matchers.instanceOf(MissingPropertyException.class));
	}

	@Test
	void testGroovyTriggerSandboxViolation() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("new java.io.File(\"Foo\") != null");
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertFalse(gef.test(etc, new EchoEvent("notbar")));
		MatcherAssert.assertThat(gef.getLastError(), Matchers.instanceOf(RejectedAccessException.class));
	}

	@Test
	void testGroovyConditionBinding() {
		MutablePicoContainer pico = ExampleSetup.setup();
		ChatLineRegexFilter clrf = new ChatLineRegexFilter();
		clrf.regex = Pattern.compile("foo (?<stuff>\\d+)");
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.setGroovyScript("match.group('stuff') == '456'");
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<ChatLineEvent> easy = new EasyTrigger<>();
		easy.setEventType(ChatLineEvent.class);
		easy.addCondition(clrf);
		easy.addCondition(gef);
		CalloutAction ca = new CalloutAction();
		ca.setText("{match.group('stuff')}");
		ca.setTts("{match.group('stuff')}");
		easy.addAction(ca);
		ez1.addTrigger(easy);

		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(coll);

		{
			dist.acceptEvent(new ChatLineEvent(123, null, "foo bar"));
			List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
			Assert.assertEquals(calls.size(), 0);
		}
		{
			dist.acceptEvent(new ChatLineEvent(123, null, "foo 123"));
			List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
			Assert.assertEquals(calls.size(), 0);
		}
		{
			dist.acceptEvent(new ChatLineEvent(123, null, "foo 456"));
			List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
			Assert.assertEquals(calls.size(), 1);
			CalloutEvent theCall = calls.get(0);
			Assert.assertEquals(theCall.getVisualText(), "456");
			Assert.assertEquals(theCall.getCallText(), "456");
		}

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
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertTrue(gef.test(etc, new EchoEvent("foobar")));
		Assert.assertFalse(gef.test(etc, new EchoEvent("notbar")));
	}

	@Test
	void testGroovyTriggerBadSyntaxNoSbx() {
		MutablePicoContainer pico = ExampleSetup.setup();
		GroovyEventFilter gef = new GroovyEventFilter(pico.getComponent(GroovyManager.class));
		gef.eventType = EchoEvent.class;
		gef.setGroovyScript("asdfasdfasdfasdf");
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertFalse(gef.test(etc, new EchoEvent("notbar")));
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
		EasyTriggerContext etc = new EasyTriggerContext(null);
		Assert.assertTrue(gef.test(etc, new EchoEvent("foobar")));
	}

	@Test
	void testCastDurationAction() {
		MutablePicoContainer pico = ExampleSetup.setup();
		pico.getComponent(FakeTimeSource.class);
		TestEventCollector coll = new TestEventCollector();
		EventMaster master = pico.getComponent(EventMaster.class);
		{
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
		}
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<AbilityCastStart> trig1 = new EasyTrigger<>();
		trig1.setEventType(AbilityCastStart.class);
		WaitCastDurationAction action1 = new WaitCastDurationAction();
		action1.remainingDurationMs = 15_000;
		CalloutAction action2 = new CalloutAction();
		action2.setText("Foo");
		action2.setTts("Bar");

		trig1.addAction(action1);
		trig1.addAction(action2);
		ez1.addTrigger(trig1);

		FakeTimeSource fts = new FakeTimeSource();
		Instant startTime = Instant.now();
		fts.setNewTime(startTime);

		XivCombatant theCbt = new XivCombatant(0x1000_0001, "Foo");
		AbilityCastStart acs = new AbilityCastStart(new XivAbility(125), theCbt, theCbt, 18.0);
		acs.setTimeSource(fts);
		acs.setHappenedAt(fts.now());
		master.pushEventAndWait(acs);
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(2_999));
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(3_001));
		// Need to push a dummy event due to fake time source
		master.pushEventAndWait(new BaseEvent() {
		});
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 1);
			TtsRequest theCall = calls.get(0);
			Assert.assertEquals(theCall.getTtsString(), "Bar");
		}
	}
	@Test
	void testBuffDurationWaitAction() {
		MutablePicoContainer pico = ExampleSetup.setup();
		pico.getComponent(FakeTimeSource.class);
		TestEventCollector coll = new TestEventCollector();
		EventMaster master = pico.getComponent(EventMaster.class);
		{
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
		}
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<BuffApplied> trig1 = new EasyTrigger<>();
		trig1.setEventType(BuffApplied.class);
		WaitBuffDurationAction action1 = new WaitBuffDurationAction(pico.getComponent(StatusEffectRepository.class));
		action1.remainingDurationMs = 15_000;
		CalloutAction action2 = new CalloutAction();
		action2.setText("Foo");
		action2.setTts("Bar");

		trig1.addAction(action1);
		trig1.addAction(action2);
		ez1.addTrigger(trig1);

		FakeTimeSource fts = new FakeTimeSource();
		Instant startTime = Instant.now();
		fts.setNewTime(startTime);
		XivCombatant theCbt = new XivCombatant(0x1000_0001, "Foo");
		BuffApplied ba = new BuffApplied(new XivStatusEffect(0x9E), 18.0, theCbt, theCbt, 0);
		ba.setTimeSource(fts);
		ba.setHappenedAt(fts.now());
		master.pushEventAndWait(ba);
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(2_999));
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(3_001));
		// Need to push a dummy event due to fake time source
		master.pushEventAndWait(new BaseEvent() {
		});
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 1);
			TtsRequest theCall = calls.get(0);
			Assert.assertEquals(theCall.getTtsString(), "Bar");
		}
	}

	@Test
	void testBuffDurationWaitActionRefresh() {
		MutablePicoContainer pico = ExampleSetup.setup();
		pico.getComponent(FakeTimeSource.class);
		TestEventCollector coll = new TestEventCollector();
		EventMaster master = pico.getComponent(EventMaster.class);
		{
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
		}
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<BuffApplied> trig1 = new EasyTrigger<>();
		trig1.setEventType(BuffApplied.class);
		WaitBuffDurationAction action1 = new WaitBuffDurationAction(pico.getComponent(StatusEffectRepository.class));
		action1.remainingDurationMs = 15_000;
		CalloutAction action2 = new CalloutAction();
		action2.setText("Foo");
		action2.setTts("Bar");

		trig1.addAction(action1);
		trig1.addAction(action2);
		ez1.addTrigger(trig1);

		FakeTimeSource fts = new FakeTimeSource();
		Instant startTime = Instant.now();
		fts.setNewTime(startTime);
		XivCombatant theCbt = new XivCombatant(0x1000_0001, "Foo");
		BuffApplied ba = new BuffApplied(new XivStatusEffect(0x9E), 18.0, theCbt, theCbt, 0);
		ba.setTimeSource(fts);
		ba.setHappenedAt(fts.now());
		master.pushEventAndWait(ba);
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(2_999));
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		BuffApplied ba2 = new BuffApplied(new XivStatusEffect(0x9E), 18.0, theCbt, theCbt, 0);
		ba2.setTimeSource(fts);
		ba2.setHappenedAt(fts.now());
		master.pushEventAndWait(ba2);
		fts.setNewTime(startTime.plusMillis(3_001));
		// Need to push a dummy event due to fake time source
		master.pushEventAndWait(new BaseEvent() {
		});
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(6_002));
		// Need to push a dummy event due to fake time source
		master.pushEventAndWait(new BaseEvent() {
		});
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 1);
			TtsRequest theCall = calls.get(0);
			Assert.assertEquals(theCall.getTtsString(), "Bar");
		}
	}

	@Test
	void testBuffDurationWaitActionCancel() {
		MutablePicoContainer pico = ExampleSetup.setup();
		pico.getComponent(FakeTimeSource.class);
		TestEventCollector coll = new TestEventCollector();
		EventMaster master = pico.getComponent(EventMaster.class);
		{
			EventDistributor dist = pico.getComponent(EventDistributor.class);
			dist.registerHandler(coll);
		}
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		EasyTrigger<BuffApplied> trig1 = new EasyTrigger<>();
		trig1.setEventType(BuffApplied.class);
		WaitBuffDurationAction action1 = new WaitBuffDurationAction(pico.getComponent(StatusEffectRepository.class));
		action1.remainingDurationMs = 15_000;
		action1.stopIfGone = true;
		CalloutAction action2 = new CalloutAction();
		action2.setText("Foo");
		action2.setTts("Bar");

		trig1.addAction(action1);
		trig1.addAction(action2);
		ez1.addTrigger(trig1);

		FakeTimeSource fts = new FakeTimeSource();
		Instant startTime = Instant.now();
		fts.setNewTime(startTime);
		XivCombatant theCbt = new XivCombatant(0x1000_0001, "Foo");
		BuffApplied ba = new BuffApplied(new XivStatusEffect(0x9E), 18.0, theCbt, theCbt, 0);
		ba.setTimeSource(fts);
		ba.setHappenedAt(fts.now());
		master.pushEventAndWait(ba);
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(2_999));
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		BuffRemoved br = new BuffRemoved(new XivStatusEffect(0x9E), 18.0, theCbt, theCbt, 0);
		br.setTimeSource(fts);
		br.setHappenedAt(fts.now());
		master.pushEventAndWait(br);
		fts.setNewTime(startTime.plusMillis(3_001));
		// Need to push a dummy event due to fake time source
		master.pushEventAndWait(new BaseEvent() {
		});
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
		fts.setNewTime(startTime.plusMillis(6_002));
		// Need to push a dummy event due to fake time source
		master.pushEventAndWait(new BaseEvent() {
		});
		{
			List<TtsRequest> calls = coll.getEventsOf(TtsRequest.class);
			Assert.assertEquals(calls.size(), 0);
		}
	}
}