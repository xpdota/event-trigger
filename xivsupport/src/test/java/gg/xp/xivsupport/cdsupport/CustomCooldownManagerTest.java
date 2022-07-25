package gg.xp.xivsupport.cdsupport;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class CustomCooldownManagerTest {

	@Test
	void testCustomCooldownPersistence() {
		// TODO: failed triggers test
		MutablePicoContainer pico = XivMain.testingMasterInit();
//		pico.getComponent(PersistenceProvider.class).save("easy-triggers.my-triggers", "[\"bad\": \"data\"]");
		pico.getComponent(EventDistributor.class).acceptEvent(new InitEvent());

		CustomCooldownManager ccm = pico.getComponent(CustomCooldownManager.class);
		Assert.assertEquals(ccm.getCooldowns().size(), 0);

		{
			CustomCooldown cdWithLotsOfFields = new CustomCooldown();
			cdWithLotsOfFields.primaryAbilityId = 0x123;
			cdWithLotsOfFields.secondaryAbilityIds = new long[]{1, 2, 3};
			cdWithLotsOfFields.buffIds = new long[]{4, 5, 6};
			cdWithLotsOfFields.cooldown = 123.45;
			cdWithLotsOfFields.duration = 78.9;
			cdWithLotsOfFields.maxCharges = 5;
			cdWithLotsOfFields.nameOverride = "foo";
			ccm.addCooldown(cdWithLotsOfFields);
		}

		Assert.assertEquals(ccm.getCooldowns().size(), 1);

		{
			CustomCooldown basicCd = new CustomCooldown();
			basicCd.primaryAbilityId = 555;
			ccm.addCooldown(basicCd);
		}

		Assert.assertEquals(ccm.getCooldowns().size(), 2);

		CustomCooldownManager newCcm = new CustomCooldownManager(pico.getComponent(PersistenceProvider.class), pico.getComponent(EventMaster.class));


		List<CustomCooldown> cdsAfter = newCcm.getCooldowns();
		Assert.assertEquals(cdsAfter.size(), 2);

		{
			CustomCooldown cdWithLotsOfFields = cdsAfter.get(0);
			Assert.assertEquals(cdWithLotsOfFields.primaryAbilityId, 0x123);
			Assert.assertEquals(cdWithLotsOfFields.secondaryAbilityIds, new long[]{1, 2, 3});
			Assert.assertEquals(cdWithLotsOfFields.buffIds, new long[]{4, 5, 6});
			Assert.assertEquals(cdWithLotsOfFields.cooldown, (Double) 123.45);
			Assert.assertEquals(cdWithLotsOfFields.duration, (Double) 78.9);
			Assert.assertEquals(cdWithLotsOfFields.maxCharges, (Integer) 5);
			Assert.assertEquals(cdWithLotsOfFields.nameOverride, "foo");
		}
		{
			CustomCooldown basicCd = cdsAfter.get(1);
			Assert.assertEquals(basicCd.primaryAbilityId, 555);
			Assert.assertEquals(basicCd.secondaryAbilityIds, new long[]{});
			Assert.assertEquals(basicCd.buffIds, new long[]{});
			assertNull(basicCd.duration);
			assertNull(basicCd.maxCharges);
			assertTrue(basicCd.autoBuffs);
			assertNull(basicCd.nameOverride);

		}


	}

}