package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import org.apache.commons.lang3.mutable.MutableInt;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ParentedBooleanSettingTest {

	@Test
	void theTest() {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		MutableInt parentCount = new MutableInt();
		MutableInt middleCount = new MutableInt();
		MutableInt childCount = new MutableInt();
		BooleanSetting parent = new BooleanSetting(pers, "parent", false);
		ParentedBooleanSetting middle = new ParentedBooleanSetting(pers, "middle", parent);
		ParentedBooleanSetting child = new ParentedBooleanSetting(pers, "child", middle);
		parent.addListener(parentCount::increment);
		middle.addListener(middleCount::increment);
		child.addListener(childCount::increment);

		{
			Assert.assertFalse(parent.get());
			Assert.assertFalse(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 0);
			Assert.assertFalse(middle.get());
			Assert.assertFalse(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 0);
			Assert.assertFalse(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 0);
		}

		{
			parent.set(false);
			Assert.assertFalse(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 1);
			Assert.assertFalse(middle.get());
			Assert.assertFalse(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 1);
			Assert.assertFalse(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 1);
		}
		{
			parent.set(true);
			Assert.assertTrue(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 2);
			Assert.assertTrue(middle.get());
			Assert.assertFalse(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 2);
			Assert.assertTrue(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 2);
		}
		{
			middle.set(false);
			Assert.assertTrue(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 2);
			Assert.assertFalse(middle.get());
			Assert.assertTrue(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 3);
			Assert.assertFalse(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 3);
		}
		{
			middle.delete();
			Assert.assertTrue(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 2);
			Assert.assertTrue(middle.get());
			Assert.assertFalse(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 4);
			Assert.assertTrue(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 4);
		}
		{
			child.set(false);
			Assert.assertTrue(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 2);
			Assert.assertTrue(middle.get());
			Assert.assertFalse(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 4);
			Assert.assertFalse(child.get());
			Assert.assertTrue(child.isSet());
			Assert.assertEquals(childCount.intValue(), 5);
		}
		{
			middle.set(true);
			Assert.assertTrue(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 2);
			Assert.assertTrue(middle.get());
			Assert.assertTrue(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 5);
			Assert.assertFalse(child.get());
			Assert.assertTrue(child.isSet());
			Assert.assertEquals(childCount.intValue(), 6);
		}
		{
			child.delete();
			Assert.assertTrue(parent.get());
			Assert.assertTrue(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 2);
			Assert.assertTrue(middle.get());
			Assert.assertTrue(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 5);
			Assert.assertTrue(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 7);
		}
		{
			parent.delete();
			Assert.assertFalse(parent.get());
			Assert.assertFalse(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 3);
			Assert.assertTrue(middle.get());
			Assert.assertTrue(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 6);
			Assert.assertTrue(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 8);
		}
		{
			middle.delete();
			Assert.assertFalse(parent.get());
			Assert.assertFalse(parent.isSet());
			Assert.assertEquals(parentCount.intValue(), 3);
			Assert.assertFalse(middle.get());
			Assert.assertFalse(middle.isSet());
			Assert.assertEquals(middleCount.intValue(), 7);
			Assert.assertFalse(child.get());
			Assert.assertFalse(child.isSet());
			Assert.assertEquals(childCount.intValue(), 9);
		}

	}

}
