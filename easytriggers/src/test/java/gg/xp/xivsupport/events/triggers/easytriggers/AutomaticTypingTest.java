package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.HasMutableEventType;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class AutomaticTypingTest {
	@Test
	void conditionsTest() {
		MutablePicoContainer pico = ExampleSetup.setup();
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		List<ConditionDescription<?, ?>> conditions = ez1.getConditions();
		for (ConditionDescription<?, ?> desc : conditions) {
			Class<?> appliesTo = desc.appliesTo();
			Condition<?> inst = desc.newInst();
			// If it's settable, ignore a mismatch
			if (inst instanceof HasMutableEventType hmet) {
				continue;
			}
			Class<?> eventType = inst.getEventType();
			Assert.assertEquals(eventType, appliesTo, inst.getClass().getSimpleName());
		}
	}

	@Test
	void actionsTest() {
		MutablePicoContainer pico = ExampleSetup.setup();
		EasyTriggers ez1 = pico.getComponent(EasyTriggers.class);
		List<ActionDescription<?, ?>> actions = ez1.getActions();
		for (ActionDescription<?, ?> desc : actions) {
			Class<?> appliesTo = desc.appliesTo();
			Action<?> inst = desc.newInst();
			Class<?> eventType = inst.getEventType();
			Assert.assertEquals(eventType, appliesTo, inst.getClass().getSimpleName());
		}
	}
}
