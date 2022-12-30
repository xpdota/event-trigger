package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Action;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.Condition;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EventDescription;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

public class AllSerDeTest {

	@Test
	void testAllSerDe() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		EasyTriggers ets = pico.getComponent(EasyTriggers.class);
		List<EventDescription<?>> eds = ets.getEventDescriptions();
		for (EventDescription<?> ed : eds) {
			EasyTrigger<?> etDummy = ed.newEmptyInst(null);
			List<ConditionDescription<?, ?>> conditions = ets.getConditionsApplicableTo(etDummy);
			for (ConditionDescription<?, ?> condition : conditions) {
				try {
					EasyTrigger<?> trigger = ed.newEmptyInst(null);
					Condition condBefore = condition.newInst();
					trigger.addCondition(condBefore);
					String exported = ets.exportToString(Collections.singletonList(trigger));
					List<EasyTrigger<?>> imported = ets.importFromString(exported);
					String reExported = ets.exportToString(imported);
					Assert.assertEquals(reExported, exported);
				}
				catch (Throwable t) {
					throw new AssertionError("Error on event description %s, condition %s".formatted(ed.type().getSimpleName(), condition.description()), t);
				}
			}
			List<ActionDescription<?, ?>> actions = ets.getActionsApplicableTo(etDummy);
			for (ActionDescription<?, ?> action : actions) {
				try {
					EasyTrigger<?> trigger = ed.newEmptyInst(null);
					Action actionBefore = action.newInst();
					trigger.addAction(actionBefore);
					String exported = ets.exportToString(Collections.singletonList(trigger));
					List<EasyTrigger<?>> imported = ets.importFromString(exported);
					String reExported = ets.exportToString(imported);
					Assert.assertEquals(reExported, exported);
				}
				catch (Throwable t) {
					throw new AssertionError("Error on event description %s, action %s".formatted(ed.type().getSimpleName(), action.description()), t);
				}
			}
		}
	}
}

