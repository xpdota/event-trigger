package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import org.testng.Assert;
import org.testng.annotations.Test;


public class IntSettingTest {

	@Test
	void testIntSetting() {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		int firstValue = 50;
		int secondValue = 555;
		{
			int dflt = 123456;
			IntSetting setting = new IntSetting(pers, "key", dflt);

			Assert.assertNull(pers.get("key", String.class, null));
			Assert.assertEquals(setting.get(), dflt);

			{
				setting.set(firstValue);
				Assert.assertEquals(pers.get("key", String.class, null), "50");
				Assert.assertEquals(setting.get(), firstValue);
			}

			{
				setting.set(secondValue);
				Assert.assertEquals(pers.get("key", String.class, null), "555");
				Assert.assertEquals(setting.get(), secondValue);
			}
		}

		{
			int dflt = 888;
			IntSetting setting = new IntSetting(pers, "key", dflt);
			Assert.assertEquals(setting.get(), secondValue);
			Assert.assertEquals(pers.get("key", String.class, null), "555");
		}
	}

	@Test
	void testLimits() {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		int dflt = 123456;
		int min = 5;
		int max = 10;
		IntSetting setting = new IntSetting(pers, "key", dflt, min, max);
		// Okay for default to be outside of range, need to think about best way to fix
		Assert.assertEquals(setting.get(), dflt);

		setting.set(7);
		Assert.assertEquals(setting.get(), 7);

		setting.set(5);
		Assert.assertEquals(setting.get(), 5);
		setting.set(10);
		Assert.assertEquals(setting.get(), 10);

		Assert.expectThrows(IllegalArgumentException.class, () -> setting.set(4));
		Assert.expectThrows(IllegalArgumentException.class, () -> setting.set(11));
	}

}