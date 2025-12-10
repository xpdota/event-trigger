package gg.xp.xivsupport.timelines;

import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.timelines.cbevents.CbEventType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class CustomEventSyncControllerTest {
	@Test
	void testAbility() {
		var cesc = new CustomEventSyncController(CbEventType.Ability, Map.of("id", List.of("1234ABCD")));
		Assert.assertTrue(cesc.shouldSync(new AbilityUsedEvent(new XivAbility(0x1234ABCD), null, null, List.of(), 0, 0, 0)));
		Assert.assertFalse(cesc.shouldSync(new AbilityUsedEvent(new XivAbility(0x5678), null, null, List.of(), 0, 0, 0)));
	}
}
