package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.LogLineRegexFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class ActLegacyImportTest {

	@Test
	void testImport() {
		EasyTrigger<ACTLogLineEvent> trigger = ActLegacyTriggerImport.parseTriggerXml("<Trigger R=\"asdf\" SD=\"&quot;qwer&quot;\" ST=\"2\" CR=\"F\" C=\"zxcv\" T=\"F\" TN=\"tyui\" Ta=\"T\" />");
		Assert.assertEquals(trigger.getEventType(), ACTLogLineEvent.class);
		Assert.assertEquals(trigger.getText(), "\"qwer\"");
		Assert.assertEquals(trigger.getTts(), "\"qwer\"");
		Assert.assertEquals(trigger.getName(), "asdf");
		Assert.assertEquals(trigger.getConditions().size(), 1);
		LogLineRegexFilter condition = (LogLineRegexFilter) trigger.getConditions().get(0);
		Assert.assertEquals(condition.regex.pattern(), "asdf");
	}

	@Test
	void testImportWithCaptureGroups() {
		// TODO: test for escaping
		EasyTrigger<ACTLogLineEvent> trigger = ActLegacyTriggerImport.parseTriggerXml("<Trigger R=\"Battle (?<verb>[a-z]+) in (\\d+) seconds!\" SD=\"Hey, $2 seconds until ${verb}!\" ST=\"2\" CR=\"F\" C=\"zxcv\" T=\"F\" TN=\"tyui\" Ta=\"T\" />");
		Assert.assertEquals(trigger.getEventType(), ACTLogLineEvent.class);
		Assert.assertEquals(trigger.getText(), "Hey, {match.group(2)} seconds until {match.group('verb')}!");
		Assert.assertEquals(trigger.getTts(), "Hey, {match.group(2)} seconds until {match.group('verb')}!");
		Assert.assertEquals(trigger.getName(), "Battle (?<verb>[a-z]+) in (\\d+) seconds!");
		Assert.assertEquals(trigger.getConditions().size(), 1);
		LogLineRegexFilter condition = (LogLineRegexFilter) trigger.getConditions().get(0);
		Assert.assertEquals(condition.regex.pattern(), "Battle (?<verb>[a-z]+) in (\\d+) seconds!");

		MutablePicoContainer pico = ExampleSetup.setup();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = pico.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		ez1.addTrigger(trigger);

		dist.acceptEvent(new ACTLogLineEvent("00|2022-06-06T17:43:24.0000000+08:00|0139||Battle commencing in 15 seconds! (DRK Player)|d25e02d29eef53f7"));
		{
			List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
			Assert.assertEquals(calls.size(), 1);
			CalloutEvent theCall = calls.get(0);
			Assert.assertEquals(theCall.getVisualText(), "Hey, 15 seconds until commencing!");
			Assert.assertEquals(theCall.getCallText(), "Hey, 15 seconds until commencing!");
		}
	}

}