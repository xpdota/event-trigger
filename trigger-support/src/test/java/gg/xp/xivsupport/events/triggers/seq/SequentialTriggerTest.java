package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.parsers.FakeTimeSource;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.misc.EchoEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SequentialTriggerTest {


	XivCombatant cbt = new XivCombatant(1, "Combatant");
	AbilityUsedEvent nonMatchingEvent = new AbilityUsedEvent(new XivAbility(789), cbt, cbt, Collections.emptyList(), 1, 1, 1);
	AbilityUsedEvent firstMatchingEvent = new AbilityUsedEvent(new XivAbility(123), cbt, cbt, Collections.emptyList(), 2, 1, 1);
	AbilityUsedEvent secondMatchingEvent = new AbilityUsedEvent(new XivAbility(123), cbt, cbt, Collections.emptyList(), 3, 1, 1);
	AbilityUsedEvent altMatchingEvent = new AbilityUsedEvent(new XivAbility(456), cbt, cbt, Collections.emptyList(), 4, 1, 1);
	AbilityUsedEvent altMatchingEventInFuture = new AbilityUsedEvent(new XivAbility(456), cbt, cbt, Collections.emptyList(), 4, 1, 1);

	{
		altMatchingEventInFuture.setHappenedAt(altMatchingEventInFuture.getHappenedAt().plusSeconds(6));
	}

	@Test
	void basicTest() {
		SequentialTrigger<AbilityUsedEvent> mySeqTrig = new SequentialTrigger<>(5000, AbilityUsedEvent.class, e -> e.getAbility().getId() == 123, (e1, s) -> {
			s.accept(new TtsRequest("Foo: " + e1.getSequenceId()));
			AbilityUsedEvent e2 = s.waitEvent(AbilityUsedEvent.class, e -> e.getAbility().getId() == 123);
			s.accept(new TtsRequest("Bar: " + e2.getSequenceId()));
			AbilityUsedEvent e3 = s.waitEvent(AbilityUsedEvent.class, e -> e.getAbility().getId() == 456);
			s.accept(new TtsRequest("Baz: " + e3.getSequenceId()));
		});

		MutablePicoContainer pico = XivMain.testingMinimalInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(AbilityUsedEvent.class, mySeqTrig::feed);

		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		dist.acceptEvent(nonMatchingEvent);
		// First event
		dist.acceptEvent(firstMatchingEvent);
		dist.acceptEvent(nonMatchingEvent);
		// Second event
		dist.acceptEvent(secondMatchingEvent);
		// These should do nothing
		dist.acceptEvent(secondMatchingEvent);
		dist.acceptEvent(secondMatchingEvent);

		dist.acceptEvent(nonMatchingEvent);
		// The third event
		dist.acceptEvent(altMatchingEvent);
		// Does nothing, trigger completed
		dist.acceptEvent(altMatchingEvent);
		dist.acceptEvent(altMatchingEvent);
		dist.acceptEvent(nonMatchingEvent);
		// Do the stuff again
		dist.acceptEvent(firstMatchingEvent);
		dist.acceptEvent(secondMatchingEvent);
		dist.acceptEvent(nonMatchingEvent);
		dist.acceptEvent(altMatchingEvent);

		List<String> ttsEvents = coll.getEventsOf(TtsRequest.class).stream().map(TtsRequest::getTtsString).toList();

		MatcherAssert.assertThat(ttsEvents, Matchers.equalTo(List.of("Foo: 2", "Bar: 3", "Baz: 4", "Foo: 2", "Bar: 3", "Baz: 4")));

	}

	@Test
	void backToBackTest() throws InterruptedException {
		SequentialTrigger<AbilityUsedEvent> mySeqTrig = new SequentialTrigger<>(5000, AbilityUsedEvent.class, e -> e.getAbility().getId() == 123, (e1, s) -> {
			s.accept(new TtsRequest("Foo: " + e1.getSequenceId()));
			AbilityUsedEvent e2 = s.waitEvent(AbilityUsedEvent.class, e -> e.getAbility().getId() == 123);
			s.accept(new TtsRequest("Bar: " + e2.getSequenceId()));
			AbilityUsedEvent e3 = s.waitEvent(AbilityUsedEvent.class, e -> e.getAbility().getId() == 456);
			s.accept(new TtsRequest("Baz: " + e3.getSequenceId()));
		});

		MutablePicoContainer pico = XivMain.testingMinimalInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(AbilityUsedEvent.class, mySeqTrig::feed);

		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		// First event
		dist.acceptEvent(firstMatchingEvent);
		// Second event
		dist.acceptEvent(secondMatchingEvent);
		// The third event
		dist.acceptEvent(altMatchingEvent);
//		Thread.sleep(100);
		// TODO: the reason this is sometimes failing is because the above event gets the timeout, NOT an issue of waiting!
		// First event
		dist.acceptEvent(firstMatchingEvent);
		// Second event
		dist.acceptEvent(secondMatchingEvent);
		// The third event
		dist.acceptEvent(altMatchingEvent);

		List<String> ttsEvents = coll.getEventsOf(TtsRequest.class).stream().map(TtsRequest::getTtsString).toList();

		MatcherAssert.assertThat(ttsEvents, Matchers.equalTo(List.of("Foo: 2", "Bar: 3", "Baz: 4", "Foo: 2", "Bar: 3", "Baz: 4")));

	}

	@Test
	void timeoutTest() throws InterruptedException {
		SequentialTrigger<AbilityUsedEvent> mySeqTrig = new SequentialTrigger<>(5000, AbilityUsedEvent.class, e -> e.getAbility().getId() == 123, (e1, s) -> {
			s.accept(new TtsRequest("Foo: " + e1.getSequenceId()));
			AbilityUsedEvent e2 = s.waitEvent(AbilityUsedEvent.class, e -> e.getAbility().getId() == 123);
			s.accept(new TtsRequest("Bar: " + e2.getSequenceId()));
			AbilityUsedEvent e3 = s.waitEvent(AbilityUsedEvent.class, e -> e.getAbility().getId() == 456);
			s.accept(new TtsRequest("Baz: " + e3.getSequenceId()));
		});

		MutablePicoContainer pico = XivMain.testingMinimalInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(AbilityUsedEvent.class, mySeqTrig::feed);

		TestEventCollector coll = new TestEventCollector();
		dist.registerHandler(coll);

		// First event
		dist.acceptEvent(firstMatchingEvent);
		// Second event
		dist.acceptEvent(secondMatchingEvent);
		// The third event should time out
		Thread.sleep(6000);
		dist.acceptEvent(altMatchingEventInFuture);
		// First event
		dist.acceptEvent(firstMatchingEvent);
		// Second event
		dist.acceptEvent(secondMatchingEvent);
		// The third event
		dist.acceptEvent(altMatchingEvent);

		List<String> ttsEvents = coll.getEventsOf(TtsRequest.class).stream().map(TtsRequest::getTtsString).toList();

		MatcherAssert.assertThat(ttsEvents, Matchers.equalTo(List.of("Foo: 2", "Bar: 3", "Foo: 2", "Bar: 3", "Baz: 4")));

	}

	@Test
	void testAutoWipeReset() {

		AtomicInteger aint = new AtomicInteger();
		SequentialTrigger<BaseEvent> trigger = SqtTemplates.sq(10_000, EchoEvent.class, e -> e.getLine().startsWith("Foo"), (e1, s) -> {
			aint.incrementAndGet();
			EchoEvent bar = s.waitEvent(EchoEvent.class, e -> e.getLine().startsWith("Bar"));
			aint.incrementAndGet();
		});
		MutablePicoContainer pico = XivMain.testingMinimalInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(BaseEvent.class, trigger::feed);

		dist.acceptEvent(new DebugCommand("Not interested in this event"));
		dist.acceptEvent(new EchoEvent("Foo1"));
		dist.acceptEvent(new EchoEvent("Bar1"));
		Assert.assertEquals(aint.get(), 2);
		dist.acceptEvent(new EchoEvent("Foo1"));
		dist.acceptEvent(new WipeEvent());
		// Should be skipped because we wiped
		dist.acceptEvent(new EchoEvent("Bar1"));
		Assert.assertEquals(aint.get(), 3);
		dist.acceptEvent(new EchoEvent("Foo1"));
		dist.acceptEvent(new EchoEvent("Bar1"));
		Assert.assertEquals(aint.get(), 5);
	}

	@Test
	void testMultiInvocation() {

		List<String> values = new ArrayList<>();
		SequentialTrigger<BaseEvent> trigger = SqtTemplates.multiInvocation(10_000, EchoEvent.class, e -> e.getLine().startsWith("Foo"), (e1, s) -> {
					values.add(e1.getLine());
					EchoEvent bar = s.waitEvent(EchoEvent.class, e -> e.getLine().startsWith("Bar"));
					values.add(bar.getLine());
				},
				(e1, s) -> {
					values.add('X' + e1.getLine());
					EchoEvent bar = s.waitEvent(EchoEvent.class, e -> e.getLine().startsWith("Bar"));
					values.add('X' + bar.getLine());

				});
		MutablePicoContainer pico = XivMain.testingMinimalInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(BaseEvent.class, trigger::feed);

		dist.acceptEvent(new DebugCommand("Not interested in this event"));
		dist.acceptEvent(new EchoEvent("Foo1"));
		dist.acceptEvent(new EchoEvent("Bar1"));
		Assert.assertEquals(values, List.of("Foo1", "Bar1"));
		dist.acceptEvent(new EchoEvent("Foo2"));
		dist.acceptEvent(new WipeEvent());
		Assert.assertEquals(values, List.of("Foo1", "Bar1", "XFoo2"));
		// Should be skipped because we wiped
		dist.acceptEvent(new EchoEvent("Bar2"));
		Assert.assertEquals(values, List.of("Foo1", "Bar1", "XFoo2"));
		dist.acceptEvent(new EchoEvent("Foo3"));
		dist.acceptEvent(new EchoEvent("Bar3"));
		Assert.assertEquals(values, List.of("Foo1", "Bar1", "XFoo2", "Foo3", "Bar3"));
		dist.acceptEvent(new EchoEvent("Foo4"));
		dist.acceptEvent(new EchoEvent("Bar4"));
		Assert.assertEquals(values, List.of("Foo1", "Bar1", "XFoo2", "Foo3", "Bar3", "XFoo4", "XBar4"));
		dist.acceptEvent(new EchoEvent("Foo5"));
		dist.acceptEvent(new EchoEvent("Bar5"));
		Assert.assertEquals(values, List.of("Foo1", "Bar1", "XFoo2", "Foo3", "Bar3", "XFoo4", "XBar4"));
	}

	@Test
	void testDelay() {
		SequentialTrigger<BaseEvent> trigger = SqtTemplates.sq(30_000, EchoEvent.class, e -> e.getLine().startsWith("Foo"),
				(e1, s) -> {
					s.accept(new DebugCommand("Bar1"));
					s.waitMs(500);
					s.accept(new DebugCommand("Bar2"));
				});

		EchoEvent initial = new EchoEvent("Foo");
		FakeTimeSource fts = new FakeTimeSource();
		Instant time = Instant.EPOCH;
		fts.setNewTime(time);
		initial.setTimeSource(fts);
		initial.setHappenedAt(time);

		MutablePicoContainer pico = XivMain.testingMinimalInit();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(BaseEvent.class, trigger::feed);
		TestEventCollector tec = new TestEventCollector();
		dist.registerHandler(tec);

		EventMaster master = pico.getComponent(EventMaster.class);
		master.pushEventAndWait(initial);

		{
			time = Instant.EPOCH.plusMillis(499);
			fts.setNewTime(time);
			EchoEvent echo2 = new EchoEvent("Foo");
			echo2.setHappenedAt(time);
			echo2.setTimeSource(fts);
			master.pushEventAndWait(echo2);
		}
		{
			List<DebugCommand> debugs = tec.getEventsOf(DebugCommand.class);
			MatcherAssert.assertThat(debugs, Matchers.hasSize(1));
		}
		{
			time = Instant.EPOCH.plusMillis(501);
			fts.setNewTime(time);
			EchoEvent echo2 = new EchoEvent("Foo");
			echo2.setTimeSource(fts);
			echo2.setHappenedAt(time);
			master.pushEventAndWait(echo2);
		}
		{
			List<DebugCommand> debugs = tec.getEventsOf(DebugCommand.class);
			MatcherAssert.assertThat(debugs, Matchers.hasSize(2));
		}
	}

}