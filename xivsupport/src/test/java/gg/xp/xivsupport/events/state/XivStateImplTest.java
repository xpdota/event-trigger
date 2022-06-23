package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.models.CombatantType;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class XivStateImplTest {

	@Test
	void testFakeDetection() {
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());
		XivStateImpl state = pico.getComponent(XivStateImpl.class);
		state.setPlayer(new XivEntity(1, "Player"));
		state.setCombatants(List.of(
				new RawXivCombatantInfo(1, "Player", 10, 1, 1000, 2000, 5000, 10000, 90, 0, 0, 0, 0, 0, "FOO", 0, 0, 1, 0),
				new RawXivCombatantInfo(0x4000_0001, "Real Boss", 0, 2, 1_000_000, 1_000_000, 10000, 10000, 60, 0, 0, 0, 0, 0, "", 333, 444, 0, 0),
				// Fakes by way of same NPC Name ID
				new RawXivCombatantInfo(0x4000_0002, "Fake Boss", 0, 2, 20_000, 20_000, 10000, 10000, 60, 0, 0, 0, 0, 0, "", 334, 444, 0, 0),
				new RawXivCombatantInfo(0x4000_0003, "Fake Boss", 0, 2, 20_000, 20_000, 10000, 10000, 60, 0, 0, 0, 0, 0, "", 335, 444, 0, 0),
				// Fake by way of 9020
				new RawXivCombatantInfo(0x4000_0005, "Fake Boss 9020", 0, 2, 20_000, 20_000, 10000, 10000, 80, 0, 0, 0, 0, 0, "", 9020, 444, 0, 0)
		));

		Assert.assertEquals(state.getCombatant(1).getType(), CombatantType.PC);
		Assert.assertEquals(state.getCombatant(0x4000_0001).getType(), CombatantType.NPC);
		Assert.assertEquals(state.getCombatant(0x4000_0002).getType(), CombatantType.FAKE);
		Assert.assertEquals(state.getCombatant(0x4000_0003).getType(), CombatantType.FAKE);
		Assert.assertEquals(state.getCombatant(0x4000_0005).getType(), CombatantType.FAKE);


	}

}