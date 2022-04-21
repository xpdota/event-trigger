package gg.xp.xivsupport.events.triggers.seq;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

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
	void backToBackTest() {
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
		Thread.sleep(1000);
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

}