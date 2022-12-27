package gg.xp.xivsupport.persistence;

import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.EnumListSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class PersistenceTests {

	private static final String originalString = "Foo Bar";
	private static final boolean originalBool = false;
	private static final int originalInt = 123;
	private static final String terribleKey = "SpecialKey`~!@#$%^&*()-=_+[]{}\\|;:'\",<.>/?";
	private static final String specialValue = "SpecialValue`~!@#$%^&*()-=_+[]{}\\\\|;:'\\\",<.>/?";

	private static void testPersistenceWrite(PersistenceProvider persistence) {
		persistence.save("Str", originalString);
		persistence.save("Bool", originalBool);
		persistence.save("Int", originalInt);
		persistence.save(terribleKey, specialValue);

		new BooleanSetting(persistence, "BoolSettingExample", false).set(true);
		new LongSetting(persistence, "LongSettingExample", 54321L).set(1234567654321L);
		new EnumListSetting<>(Job.class, persistence, "JobList1", EnumListSetting.BadKeyBehavior.OMIT, null).set(List.of(Job.WHM, Job.SCH, Job.BLU));
		persistence.save("JobList2", "WHM,SCH,ABC");
		if (persistence instanceof PropertiesFilePersistenceProvider propPers) {
			propPers.flush();
		}

	}

	private static void testPersistenceRead(PersistenceProvider persistence) {

		String retrievedString = persistence.get("Str", String.class, null);
		boolean retrievedBool = persistence.get("Bool", boolean.class, false);
		int retrievedInt = persistence.get("Int", int.class, 0);
		String retrievedSpecial = persistence.get(terribleKey, String.class, null);

		Assert.assertEquals(retrievedString, originalString);
		Assert.assertEquals(retrievedBool, originalBool);
		Assert.assertEquals(retrievedInt, originalInt);
		Assert.assertEquals(retrievedSpecial, specialValue);
		Assert.assertTrue(new BooleanSetting(persistence, "BoolSettingExample", false).get());
		Assert.assertEquals(new LongSetting(persistence, "LongSettingExample", 54321L).get(), 1234567654321L);
		Assert.assertEquals(new EnumListSetting<>(Job.class, persistence, "JobList1", EnumListSetting.BadKeyBehavior.OMIT, null).get(), List.of(Job.WHM, Job.SCH, Job.BLU));
		Assert.assertEquals(persistence.get("JobList1", String.class, null), "WHM,SCH,BLU");
		Assert.assertEquals(new EnumListSetting<>(Job.class, persistence, "JobList2", EnumListSetting.BadKeyBehavior.OMIT, null).get(), List.of(Job.WHM, Job.SCH));
		Assert.assertNull(new EnumListSetting<>(Job.class, persistence, "JobList2", EnumListSetting.BadKeyBehavior.RETURN_DEFAULT, null).get());
		Assert.assertThrows(IllegalArgumentException.class, () -> new EnumListSetting<>(Job.class, persistence, "JobList2", EnumListSetting.BadKeyBehavior.THROW, null).get());

		Assert.assertEquals(new EnumListSetting<>(Job.class, persistence, "JobList3", EnumListSetting.BadKeyBehavior.OMIT, List.of(Job.BRD, Job.AST)).get(), List.of(Job.BRD, Job.AST));
		Assert.assertEquals(new EnumListSetting<>(Job.class, persistence, "JobList3", EnumListSetting.BadKeyBehavior.RETURN_DEFAULT, List.of(Job.BRD, Job.AST)).get(), List.of(Job.BRD, Job.AST));
		Assert.assertEquals(new EnumListSetting<>(Job.class, persistence, "JobList3", EnumListSetting.BadKeyBehavior.THROW, List.of(Job.BRD, Job.AST)).get(), List.of(Job.BRD, Job.AST));
	}

	@Test
	public void testInMemoryPersistence() {
		InMemoryMapPersistenceProvider persistence = new InMemoryMapPersistenceProvider();
		testPersistenceWrite(persistence);
		testPersistenceRead(persistence);
	}

	@Test
	public void testFilePersistence() {
		PropertiesFilePersistenceProvider persistence = new PropertiesFilePersistenceProvider(new File("target/testdata/foo.properties"));
		testPersistenceWrite(persistence);
		// Wait for file flush
		persistence.flush();
		testPersistenceRead(new PropertiesFilePersistenceProvider(new File("target/testdata/foo.properties")));
	}

}
