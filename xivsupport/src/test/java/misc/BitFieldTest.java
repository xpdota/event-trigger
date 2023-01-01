package misc;

import gg.xp.util.BitField;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

public class BitFieldTest {
	@Test
	void basicTest() throws IllegalAccessException, NoSuchFieldException {
		BitField bitField = new BitField();
		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(4, false);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(8, true);

		Assert.assertFalse(bitField.get(0));
		Assert.assertTrue(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(8, false);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(500, true);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(Integer.MAX_VALUE, false);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(Integer.MAX_VALUE * 10L, false);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE * 10L));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(0, true);

		Assert.assertTrue(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE * 10L));
		Assert.assertFalse(bitField.get(-1));

		Field arrayField = BitField.class.getDeclaredField("array");
		arrayField.setAccessible(true);
		Assert.assertEquals(((long[]) arrayField.get(bitField)).length, 8);
	}

	@Ignore // ignored by default due to memory
	@Test
	void extendedTest() {
		BitField bitField = new BitField();
		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(4, false);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(8, true);

		Assert.assertFalse(bitField.get(0));
		Assert.assertTrue(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(8, false);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(500, true);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertFalse(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(Integer.MAX_VALUE, true);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertTrue(bitField.get(Integer.MAX_VALUE));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(Integer.MAX_VALUE * 10L, true);

		Assert.assertFalse(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertTrue(bitField.get(Integer.MAX_VALUE));
		Assert.assertTrue(bitField.get(Integer.MAX_VALUE * 10L));
		Assert.assertFalse(bitField.get(-1));

		bitField.set(0, true);

		Assert.assertTrue(bitField.get(0));
		Assert.assertFalse(bitField.get(8));
		Assert.assertFalse(bitField.get(30));
		Assert.assertTrue(bitField.get(500));
		Assert.assertTrue(bitField.get(Integer.MAX_VALUE));
		Assert.assertTrue(bitField.get(Integer.MAX_VALUE * 10L));
		Assert.assertFalse(bitField.get(-1));
	}

}
