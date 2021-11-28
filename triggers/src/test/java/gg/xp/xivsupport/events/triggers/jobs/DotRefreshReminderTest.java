package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivdata.jobs.DotBuff;
import gg.xp.xivdata.jobs.Job;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.XivMain;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

public class DotRefreshReminderTest {

	private static final XivPlayerCharacter thePlayer = new XivPlayerCharacter(0x123L, "Player", Job.WHM, XivWorld.of(), true, 1, null, null, null, 0, 0, 0, 0, 0);
	private static final XivPlayerCharacter otherPlayer = new XivPlayerCharacter(0x222L, "Other", Job.WHM, XivWorld.of(), false, 1, null, null, null, 0, 0, 0, 0, 0);
	private static final XivCombatant enemy1 = new XivCombatant(0x456, "The Enemy");
	private static final XivCombatant enemy2 = new XivCombatant(0xABC, "The Other Enemy");
	private static final XivCombatant enemy3 = new XivCombatant(0xDEF, "The Third Enemy");


	@Test
	void testDots() throws InterruptedException {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		dist.registerHandler(coll);

		DotRefreshReminders dots = container.getComponent(DotRefreshReminders.class);
		dots.suppressSpamCallouts = false;
		dots.getDotRefreshAdvance().set(8000);

		// My dot
		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo1"),
				9,
				thePlayer,
				enemy1,
				1));
		// My dot, same enemy, should still emit delayed callout event even if it won't apply
		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo2"),
				9,
				thePlayer,
				enemy1,
				1));
		// Other player, doesn't count
		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo3"),
				9,
				otherPlayer,
				enemy2,
				1));
		// My player, second enemy
		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo4"),
				9,
				thePlayer,
				enemy2,
				1));
		// Dot from enemy
		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo5"),
				9,
				enemy2,
				thePlayer,
				1));
		// My player, second enemy, outside of the time window
		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo6"),
				11,
				thePlayer,
				enemy3,
				1));

		{
			List<BaseDelayedEvent> delayedEvents = coll.getEventsOf(BaseDelayedEvent.class);
			Assert.assertEquals(delayedEvents.size(), 0);
			coll.clear();

		}

		Thread.sleep(2000);

		{
			List<BaseDelayedEvent> delayedEvents = coll.getEventsOf(BaseDelayedEvent.class);
			Assert.assertEquals(delayedEvents.size(), 3);
			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			List<String> calloutData = callouts.stream().map(CalloutEvent::getCallText).collect(Collectors.toList());
			MatcherAssert.assertThat(calloutData, Matchers.contains("Foo2", "Foo4"));
			List<TtsRequest> ttsEvents = coll.getEventsOf(TtsRequest.class);
			List<String> ttsData = ttsEvents.stream().map(TtsRequest::getTtsString).collect(Collectors.toList());
			MatcherAssert.assertThat(ttsData, Matchers.contains("Foo2", "Foo4"));
			coll.clear();
		}

		Thread.sleep(4000);
		{
			List<BaseDelayedEvent> delayedEvents = coll.getEventsOf(BaseDelayedEvent.class);
			Assert.assertEquals(delayedEvents.size(), 1);
			List<CalloutEvent> callouts = coll.getEventsOf(CalloutEvent.class);
			List<String> calloutData = callouts.stream().map(CalloutEvent::getCallText).collect(Collectors.toList());
			MatcherAssert.assertThat(calloutData, Matchers.contains("Foo6"));
			List<TtsRequest> ttsEvents = coll.getEventsOf(TtsRequest.class);
			List<String> ttsData = ttsEvents.stream().map(TtsRequest::getTtsString).collect(Collectors.toList());
			MatcherAssert.assertThat(ttsData, Matchers.contains("Foo6"));
			coll.clear();
		}

	}

	@Test
	void testDisable() throws InterruptedException {
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.acceptEvent(new InitEvent());
		dist.registerHandler(coll);

		DotRefreshReminders dots = container.getComponent(DotRefreshReminders.class);
		dots.getEnabledDots().get(DotBuff.WHM_Aero).set(false);

		dist.acceptEvent(new BuffApplied(
				new XivStatusEffect(0x74fL, "Foo1"),
				1,
				thePlayer,
				enemy1,
				1));
		Thread.sleep(1000);
		List<BaseDelayedEvent> events = coll.getEventsOf(BaseDelayedEvent.class);
		Assert.assertTrue(events.isEmpty());

	}
}
