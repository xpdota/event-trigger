package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

public class EasyTriggersTest {

	@Test
	void simpleTest() {
		XivCombatant caster = new XivCombatant(10, "Caster");
		XivAbility matchingAbility = new XivAbility(123, "Foo Ability");
		XivAbility otherAbility = new XivAbility(456, "Bar Ability");
		XivCombatant target = new XivCombatant(11, "Target");
		AbilityUsedEvent matchingEvent = new AbilityUsedEvent(matchingAbility, caster, target, Collections.emptyList(), 123, 0, 1);
		AbilityUsedEvent nonMatchingEvent = new AbilityUsedEvent(otherAbility, caster, target, Collections.emptyList(), 123, 0, 1);
		ZoneChangeEvent wrongTypeEvent = new ZoneChangeEvent(new XivZone(987, "Some Zone"));

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

			dist.acceptEvent(nonMatchingEvent);
			dist.acceptEvent(wrongTypeEvent);
			dist.acceptEvent(matchingEvent);

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

			dist.acceptEvent(nonMatchingEvent);
			dist.acceptEvent(wrongTypeEvent);
			dist.acceptEvent(matchingEvent);

			{
				List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
				Assert.assertEquals(calls.size(), 1);
				CalloutEvent theCall = calls.get(0);
				Assert.assertEquals(theCall.getVisualText(), "123");
				Assert.assertEquals(theCall.getCallText(), "123");
			}
		}



	}

}