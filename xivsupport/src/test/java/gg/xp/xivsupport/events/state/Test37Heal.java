package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffectType;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class Test37Heal {
	@Test
	void testPureHeal() {
		String line22 = "22|2022-01-10T17:32:06.0200000-08:00|10694638|Caster Name|83|Cure III|107A597C|Target Name|4|25570000|1B|838000|0|0|0|0|0|0|0|0|0|0|0|0|51191|51191|5945|10000|||104.11|102.97|0.00|0.00|51215|51215|1504|10000|||99.26|98.68|0.00|0.40|00017984|6|7|e351ad46b76dcd16";
		String line37 = "37|2022-01-10T17:32:07.7990000-08:00|107A597C|Target Name|00017984|31378||||||104.21|103.24|0.00|0.29|c4aa0cdbd3268a52";

		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);

		dist.acceptEvent(new ACTLogLineEvent(line22));
		dist.acceptEvent(new ACTLogLineEvent(line37));

		List<AbilityUsedEvent> snapped = coll.getEventsOf(AbilityUsedEvent.class);
		List<AbilityResolvedEvent> resolved = coll.getEventsOf(AbilityResolvedEvent.class);

		Assert.assertEquals(snapped.size(), 1);
		Assert.assertEquals(resolved.size(), 1);

		Assert.assertEquals(snapped.get(0).getEffects().get(0).getEffectType(), AbilityEffectType.HEAL);
		Assert.assertEquals(resolved.get(0).getEffects().get(0).getEffectType(), AbilityEffectType.HEAL);


	}
}
