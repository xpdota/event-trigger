package gg.xp.xivsupport.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.jails.FinalTitanJailsSolvedEvent;
import gg.xp.xivsupport.events.triggers.jails.JailSolver;
import gg.xp.xivsupport.events.triggers.jails.UnsortedTitanJailsSolvedEvent;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkKeyHandler;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkRequest;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkSlotRequest;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.ws.ActWsRawMsg;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.ParentedCalloutEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JailExampleTest {

	private static final Logger log = LoggerFactory.getLogger(JailExampleTest.class);

	private static final List<Class<? extends BaseEvent>> eventsWeCareAbout = List.of(
			ACTLogLineEvent.class,
			AbilityUsedEvent.class,
			UnsortedTitanJailsSolvedEvent.class,
			FinalTitanJailsSolvedEvent.class,
			ParentedCalloutEvent.class,
			TtsRequest.class,
			AutoMarkRequest.class,
			ClearAutoMarkRequest.class,
			AutoMarkSlotRequest.class,
			AutoMarkKeyHandler.KeyPressRequest.class
	);

	/**
	 * End to end example for titan jails
	 */
	@Test
	public void jailTest() throws InterruptedException {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		XivState state = container.getComponent(XivState.class);
//		// TODO: still need better support for this...
//		Thread.sleep(1000);


		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				ParentedCalloutEvent.class,
				// Slotted requests
				AutoMarkSlotRequest.class, AutoMarkSlotRequest.class, AutoMarkSlotRequest.class,
				// TTS
				TtsRequest.class,
				// Key Presses
				AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class

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

		List<Integer> keyPresses = collector.getEventsOf(AutoMarkKeyHandler.KeyPressRequest.class).stream().map(AutoMarkKeyHandler.KeyPressRequest::getKeyCode)
				.collect(Collectors.toList());
		Assert.assertEquals(keyPresses, List.of(KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD1));

		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 1);

	}

	@Test
	public void testWrongZone() {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);


		dist.acceptEvent(new ZoneChangeEvent(new XivZone(0x123, "Stuff")));

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		Assert.assertEquals(collector.getEventsOf(UnsortedTitanJailsSolvedEvent.class).size(), 0);
	}

	@Test
	public void testZoneLockOverride() {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		JailSolver jails = container.getComponent(JailSolver.class);
		jails.getOverrideZoneLock().set(true);

		dist.acceptEvent(new ZoneChangeEvent(new XivZone(0x123, "Stuff")));

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		Assert.assertEquals(collector.getEventsOf(UnsortedTitanJailsSolvedEvent.class).size(), 1);
	}

	@Test
	public void testResets() {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);


		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Change zone
		dist.acceptEvent(new ZoneChangeEvent(new XivZone(0x309, "Stuff")));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Wipe
		dist.acceptEvent(new WipeEvent());
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		dist.acceptEvent(new DutyRecommenceEvent());
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		dist.acceptEvent(new WipeEvent());
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		Assert.assertEquals(collector.getEventsOf(UnsortedTitanJailsSolvedEvent.class).size(), 0);
	}

	@Test
	void testDisableTts() throws InterruptedException {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		JailSolver jail = container.getComponent(JailSolver.class);
		jail.getEnableTts().set(false);

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				// Slotted requests
				AutoMarkSlotRequest.class, AutoMarkSlotRequest.class, AutoMarkSlotRequest.class,
				// Key Presses
				AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class
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
		Assert.assertEquals(callouts.size(), 0);

		List<Integer> keyPresses = collector.getEventsOf(AutoMarkKeyHandler.KeyPressRequest.class).stream().map(AutoMarkKeyHandler.KeyPressRequest::getKeyCode)
				.collect(Collectors.toList());
		Assert.assertEquals(keyPresses, List.of(KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD1));

		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 1);
	}

	@Test
	public void disableAmTest() throws InterruptedException {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		JailSolver jail = container.getComponent(JailSolver.class);
		jail.getEnableAutomark().set(false);

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				// Personal callout since the player was one of the three
				ParentedCalloutEvent.class,
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
		Assert.assertEquals(automarks.size(), 0);

		List<CalloutEvent> callouts = collector.getEventsOf(CalloutEvent.class);
		Assert.assertEquals(callouts.size(), 1);
		Assert.assertEquals(callouts.get(0).getCallText(), "Third");

		List<TtsRequest> ttsEvents = collector.getEventsOf(TtsRequest.class);
		Assert.assertEquals(ttsEvents.size(), 1);
		Assert.assertEquals(ttsEvents.get(0).getTtsString(), "Third");
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
	}

	@Test
	public void resetOrderTest() throws InterruptedException {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		JailSolver jail = container.getComponent(JailSolver.class);
		List<Job> currentJailSort = new ArrayList<>(jail.getSort().getCurrentJailSort());
		Collections.reverse(currentJailSort);
		jail.getSort().setJailSort(currentJailSort);

		jail.getSort().resetJailSort();

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				ParentedCalloutEvent.class,
				// Slotted requests
				AutoMarkSlotRequest.class, AutoMarkSlotRequest.class, AutoMarkSlotRequest.class,
				// TTS
				TtsRequest.class,
				// Key Presses
				AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class

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

		List<Integer> keyPresses = collector.getEventsOf(AutoMarkKeyHandler.KeyPressRequest.class).stream().map(AutoMarkKeyHandler.KeyPressRequest::getKeyCode)
				.collect(Collectors.toList());
		Assert.assertEquals(keyPresses, List.of(KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD1));

		List<CalloutEvent> callouts = collector.getEventsOf(CalloutEvent.class);
		Assert.assertEquals(callouts.size(), 1);
		Assert.assertEquals(callouts.get(0).getCallText(), "Third");

		List<TtsRequest> ttsEvents = collector.getEventsOf(TtsRequest.class);
		Assert.assertEquals(ttsEvents.size(), 1);
		Assert.assertEquals(ttsEvents.get(0).getTtsString(), "Third");
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 1);
	}

	@Test
	public void customOrderTest() throws InterruptedException {
		MutablePicoContainer container = setup();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		// Test setup
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		JailSolver jail = container.getComponent(JailSolver.class);
		List<Job> currentJailSort = new ArrayList<>(jail.getSort().getCurrentJailSort());
		Collections.reverse(currentJailSort);
		jail.getSort().setJailSort(currentJailSort);


		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				ParentedCalloutEvent.class,
				// Slotted requests
				AutoMarkSlotRequest.class, AutoMarkSlotRequest.class, AutoMarkSlotRequest.class,
				// TTS
				TtsRequest.class,
				// Key Presses
				AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class

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
		Assert.assertEquals(jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.toList()), List.of("Foo Bar", "Some Guy", "Random Person"));

		List<AutoMarkRequest> automarks = collector.getEventsOf(AutoMarkRequest.class);
		Assert.assertEquals(automarks.stream().map(am -> am.getPlayerToMark().getName()).collect(Collectors.toList()), List.of("Foo Bar", "Some Guy", "Random Person"));

		List<Integer> keyPresses = collector.getEventsOf(AutoMarkKeyHandler.KeyPressRequest.class).stream().map(AutoMarkKeyHandler.KeyPressRequest::getKeyCode)
				.collect(Collectors.toList());
		Assert.assertEquals(keyPresses, List.of(KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD2));

		List<CalloutEvent> callouts = collector.getEventsOf(CalloutEvent.class);
		Assert.assertEquals(callouts.size(), 1);
		Assert.assertEquals(callouts.get(0).getCallText(), "First");

		List<TtsRequest> ttsEvents = collector.getEventsOf(TtsRequest.class);
		Assert.assertEquals(ttsEvents.size(), 1);
		Assert.assertEquals(ttsEvents.get(0).getTtsString(), "First");

		PersistenceProvider persistence = container.getComponent(PersistenceProvider.class);
		String sortString = persistence.get("jail-solver.job-order", String.class, null);
		Assert.assertEquals(sortString, "SGE,AST,SCH,WHM,CNJ,DNC,MCH,BRD,ARC,BLU,RDM,SMN,ACN,BLM,THM,GNB,DRK,WAR,PLD,MRD,GLA,RPR,SAM,NIN,ROG,DRG,MNK,LNC,PGL");
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 1);
	}

	@Test
	public void orderValidation() {

		MutablePicoContainer container = setup();
		JailSolver jail = container.getComponent(JailSolver.class);
		// Insufficient size
		Assert.assertThrows(IllegalArgumentException.class, () -> jail.getSort().setJailSort(List.of(Job.WHM, Job.SCH)));
		{
			List<Job> current = new ArrayList<>(jail.getSort().getCurrentJailSort());
			// Make a duplicate
			current.set(0, current.get(1));
			Assert.assertThrows(IllegalArgumentException.class, () -> jail.getSort().setJailSort(current));
		}
		{
			List<Job> current = new ArrayList<>(jail.getSort().getCurrentJailSort());
			// Too many
			current.add(current.get(0));
			Assert.assertThrows(IllegalArgumentException.class, () -> jail.getSort().setJailSort(current));
		}
	}

	// Test for load order for importing a legacy setting without the new jobs
	@Test
	public void testLoadOrderIncomplete() throws InterruptedException {
		String customSort = "AST,SCH,WHM,CNJ,DNC,MCH,BRD,ARC,BLU,RDM,SMN,ACN,BLM,THM,GNB,DRK,WAR,PLD,MRD,GLA,SAM,NIN,ROG,DRG,MNK,LNC,PGL";
		MutablePicoContainer container = XivMain.testingMasterInit();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		PersistenceProvider persistence = container.getComponent(PersistenceProvider.class);

		persistence.save("jail-solver.job-order", customSort);

		// MUST use manual version of these - we need to jump in before plugin load
		doEvents(dist);
		finishSetup(container);

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				ParentedCalloutEvent.class,
				// Slotted requests
				AutoMarkSlotRequest.class, AutoMarkSlotRequest.class, AutoMarkSlotRequest.class,
				// TTS
				TtsRequest.class,
				// Key Presses
				AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class

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

		// Since the list was incomplete

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

		List<Integer> keyPresses = collector.getEventsOf(AutoMarkKeyHandler.KeyPressRequest.class).stream().map(AutoMarkKeyHandler.KeyPressRequest::getKeyCode)
				.collect(Collectors.toList());
		Assert.assertEquals(keyPresses, List.of(KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD1));

		String sortString = persistence.get("jail-solver.job-order", String.class, null);
		Assert.assertEquals(sortString, customSort);

		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 1);
	}

	@Test
	public void testLoadOrder() throws InterruptedException {
		String customSort = "AST,SCH,SGE,WHM,CNJ,DNC,MCH,BRD,ARC,BLU,RDM,SMN,ACN,BLM,THM,GNB,DRK,WAR,PLD,MRD,GLA,SAM,NIN,ROG,DRG,MNK,LNC,PGL,RPR";
		MutablePicoContainer container = XivMain.testingMasterInit();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		TestEventCollector collector = new TestEventCollector();
		dist.registerHandler(collector);

		PersistenceProvider persistence = container.getComponent(PersistenceProvider.class);

		persistence.save("jail-solver.job-order", customSort);

		// MUST use manual version of these - we need to jump in before plugin load
		doEvents(dist);
		finishSetup(container);

		// Send events
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|13|Random Person|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// This ACTLogLineEvent also causes an AbilityUsedEvent, since that is what the 21-line represents
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 2);

		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|16|Foo Bar|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));
		// Same here, so now we have 4
		Assert.assertEquals(collector.getEventsOf(eventsWeCareAbout).size(), 4);

		// But now, after this final line, we have all that, plus all the jail stuff!
		dist.acceptEvent(new ACTLogLineEvent("21|2021-09-30T19:43:43.1650000-07:00|40016AA1|Titan|2B6C|Rock Throw|11|Some Guy|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|42489|50128|9900|10000|0|1000|86.77625|95.90898|-4.091016E-13|1.591002|2477238|4476950|0|10000|0|1000|113.7886|86.21142|-1.378858E-12|-0.7854581|00009CA2|0|9|cd69a51d5f584b836fa20c4a5b356612"));

		List<? extends Event> finalEvents = collector.getEventsOf(eventsWeCareAbout);
		log.info("Final time of events (filtered): {}", finalEvents.size());
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
				ParentedCalloutEvent.class,
				// Slotted requests
				AutoMarkSlotRequest.class, AutoMarkSlotRequest.class, AutoMarkSlotRequest.class,
				// TTS
				TtsRequest.class,
				// Key Presses
				AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class, AutoMarkKeyHandler.KeyPressRequest.class

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
		Assert.assertEquals(jailedPlayers.stream().map(XivEntity::getName).collect(Collectors.toList()), List.of("Foo Bar", "Some Guy", "Random Person"));

		List<AutoMarkRequest> automarks = collector.getEventsOf(AutoMarkRequest.class);
		Assert.assertEquals(automarks.stream().map(am -> am.getPlayerToMark().getName()).collect(Collectors.toList()), List.of("Foo Bar", "Some Guy", "Random Person"));

		List<CalloutEvent> callouts = collector.getEventsOf(CalloutEvent.class);
		Assert.assertEquals(callouts.size(), 1);
		Assert.assertEquals(callouts.get(0).getCallText(), "First");

		List<TtsRequest> ttsEvents = collector.getEventsOf(TtsRequest.class);
		Assert.assertEquals(ttsEvents.size(), 1);
		Assert.assertEquals(ttsEvents.get(0).getTtsString(), "First");

		List<Integer> keyPresses = collector.getEventsOf(AutoMarkKeyHandler.KeyPressRequest.class).stream().map(AutoMarkKeyHandler.KeyPressRequest::getKeyCode)
				.collect(Collectors.toList());
		Assert.assertEquals(keyPresses, List.of(KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD8, KeyEvent.VK_NUMPAD2));

		String sortString = persistence.get("jail-solver.job-order", String.class, null);
		Assert.assertEquals(sortString, customSort);

		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 0);
		Thread.sleep(1200);
		Assert.assertEquals(collector.getEventsOf(ClearAutoMarkRequest.class).size(), 1);
	}

	private static MutablePicoContainer setup() {
		// TODO: make this a template for integration testing
		MutablePicoContainer container = XivMain.testingMasterInit();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		doEvents(dist);
		finishSetup(container);
		return container;
	}

	private static void finishSetup(PicoContainer container) {
		BasicEventQueue queue = container.getComponent(BasicEventQueue.class);
		queue.waitDrain();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		XivState state = container.getComponent(XivState.class);
		// TODO: find actual solution to race conditions in tests
		try {
			Assert.assertEquals(state.getPartyList().size(), 8);
			Assert.assertEquals(state.getCombatantsListCopy().size(), 8);
		}
		catch (Throwable e) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			Assert.assertEquals(state.getPartyList().size(), 8);
			Assert.assertEquals(state.getCombatantsListCopy().size(), 8);
		}
		queue.waitDrain();

		JailSolver jail = container.getComponent(JailSolver.class);
		jail.getJailClearDelay().set(1000);
	}

	private static void doEvents(EventDistributor dist) {

		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangePrimaryPlayer\",\"charID\":22,\"charName\":\"Foo Bar\"}"));
		dist.acceptEvent(new ActWsRawMsg("{\"type\":\"ChangeZone\",\"zoneID\":777,\"zoneName\":\"the Weapon's Refrain (Ultimate)\"}"));
		// This player should be sorted first because they are the actual player
		dist.acceptEvent(new ActWsRawMsg("""
				{
				  "type": "PartyChanged",
				  "party": [
				    {
				      "id": "10",
				      "name": "Player One",
				      "worldId": 63,
				      "job": 22,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "13",
				      "name": "Random Person",
				      "worldId": 65,
				      "job": 21,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "12",
				      "name": "Who Dis",
				      "worldId": 73,
				      "job": 25,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "11",
				      "name": "Some Guy",
				      "worldId": 65,
				      "job": 27,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "14",
				      "name": "Other Alliance",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "16",
				      "name": "Foo Bar",
				      "worldId": 65,
				      "job": 24,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "15",
				      "name": "Pf Hero",
				      "worldId": 79,
				      "job": 33,
				      "level": 0,
				      "inParty": true
				    },
				    {
				      "id": "17",
				      "name": "Last Guy",
				      "worldId": 65,
				      "job": 38,
				      "level": 0,
				      "inParty": true
				    }
				  ]
				}
				"""));
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
						      "ID": 16,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 22,
						      "Level": 80,
						      "Name": "Player One",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 17,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 27,
						      "Level": 80,
						      "Name": "Some Guy",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 18,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 25,
						      "Level": 80,
						      "Name": "Who Dis",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 19,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 21,
						      "Level": 80,
						      "Name": "Random Person",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 20,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 33,
						      "Level": 80,
						      "Name": "Other Alliance",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 21,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 33,
						      "Level": 80,
						      "Name": "Pf Hero",
						      "CurrentHP": 122700,
						      "MaxHP": 122700,
						      "CurrentMP": 10000,
						      "MaxMP": 10000,
						      "PosX": 114.926422,
						      "PosY": -83.86734,
						      "PosZ": 44.3433,
						      "Heading": -1.66136408
						    },
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
						    },
						    {
						      "CurrentWorldID": 65,
						      "WorldID": 65,
						      "WorldName": "Midgardsormr",
						      "BNpcID": 0,
						      "BNpcNameID": 0,
						      "PartyType": 0,
						      "ID": 23,
						      "OwnerID": 0,
						      "type": 1,
						      "Job": 38,
						      "Level": 80,
						      "Name": "Last Guy",
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

	}

}
