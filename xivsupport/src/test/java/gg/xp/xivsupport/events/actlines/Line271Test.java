package gg.xp.xivsupport.events.actlines;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.Position;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line271Test {
	@Test
	void testPos() {
		String line = "271|2024-12-26T13:15:21.8910000-08:00|4000249E|-1.5745|00|00|73.5000|100.0000|0.0000|4843ca62b169bc5a";
		String dummyLine = "21|2024-12-26T13:15:21.9360000-08:00|4000249E|Cloud of Darkness|9E22|_rsv_40482_-1_1_0_0_SE2DC5B04_EE2DC5B04|E0000000||0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|||||||||||44|44|0|10000|||100.00|100.00|0.00|2.76|0000B1C5|0|0|00||01|9E22|9E22|1.100|3FD9|47c31102dc03ad85";
		MutablePicoContainer container = XivMain.testingMasterInit();
		TestEventCollector coll = new TestEventCollector();
		EventDistributor dist = container.getComponent(EventDistributor.class);
		dist.registerHandler(coll);
		dist.acceptEvent(new ACTLogLineEvent(line));
		XivState state = container.getComponent(XivState.class);
		{
			XivCombatant cbt = state.getCombatant(0x4000249E);
			Assert.assertEquals(cbt.getPos(), new Position(73.5000, 100.0000, 0.0000, -1.5745));
		}
		// Now submit a 21-line with a stale position - it should not do anything
		dist.acceptEvent(new ACTLogLineEvent(dummyLine));
		{
			XivCombatant cbt = state.getCombatant(0x4000249E);
			Assert.assertEquals(cbt.getPos(), new Position(73.5000, 100.0000, 0.0000, -1.5745));
		}
	}
}
