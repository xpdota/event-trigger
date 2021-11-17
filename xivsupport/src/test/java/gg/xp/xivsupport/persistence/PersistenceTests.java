package gg.xp.xivsupport.persistence;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

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
	}

	@Test
	public void testInMemoryPersistence() {
		InMemoryMapPersistenceProvider persistence = new InMemoryMapPersistenceProvider();
		testPersistenceWrite(persistence);
		testPersistenceRead(persistence);
	}

	@Test
	public void testFilePersistence() throws InterruptedException {
		testPersistenceWrite(new PropertiesFilePersistenceProvider(new File("target/testdata/foo.properties")));
		// Wait for file flush
		Thread.sleep(200);
		testPersistenceRead(new PropertiesFilePersistenceProvider(new File("target/testdata/foo.properties")));
	}

	@Test
	public void testDefaultPropsLocation() throws InterruptedException {
		testPersistenceWrite(PropertiesFilePersistenceProvider.inUserDataFolder("integration-test"));
		// Wait for file flush
		Thread.sleep(200);
		testPersistenceRead(PropertiesFilePersistenceProvider.inUserDataFolder("integration-test"));

	}


}
