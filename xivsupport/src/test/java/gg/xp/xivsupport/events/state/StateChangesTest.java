package gg.xp.xivsupport.events.state;

import gg.xp.reevent.context.StateStore;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.reevent.events.EventDistributor;
import gg.xp.xivsupport.sys.XivMain;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Ignore // Using ACTWS for zone/player change
public class StateChangesTest {

	@Test
	public void testZoneAndPlayerChange() {
		EventDistributor dist = XivMain.testingMasterInit().getComponent(EventDistributor.class);
		dist.acceptEvent(new ACTLogLineEvent("01|2021-04-26T14:13:17.9930000-04:00|326|Kugane Ohashi|b9f401c0aa0b8bc454b239b201abc1b8"));
		dist.acceptEvent(new ACTLogLineEvent("02|2021-04-26T14:11:31.0200000-04:00|10ff0001|New Player|5b0a5800460045f29db38676e0c3f79a"));
		StateStore stateStore = dist.getStateStore();
		XivState xivState = stateStore.get(XivState.class);

		Assert.assertEquals(xivState.getZone().getId(), 0x326);
		Assert.assertEquals(xivState.getPlayer().getId(), 0x10ff0001);

		Assert.assertEquals(xivState.getZone().getName(), "Kugane Ohashi");
		Assert.assertEquals(xivState.getPlayer().getName(), "New Player");
	}

}
