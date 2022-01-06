package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class CalloutTests {


	@Test
	void testNonModifiedCallout() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar");
		CalloutEvent modified = mc.getModified();
		Assert.assertEquals(modified.getCallText(), "Bar");
		Assert.assertEquals(modified.getVisualText(), "Bar");
	}

	@Test
	void testModifiedCallout() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "Bar");
			Assert.assertEquals(modified.getVisualText(), "Bar");
		}
		mch.getTtsSetting().set("123");
		mch.getTextSetting().set("456");
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertEquals(modified.getVisualText(), "456");
		}
		mch.getEnableTts().set(false);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertNull(modified.getCallText());
			Assert.assertEquals(modified.getVisualText(), "456");
		}
		mch.getEnableTts().set(true);
		mch.getEnableText().set(false);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnableTts().set(false);
		mch.getEnableText().set(false);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnable().set(false);
		mch.getEnableText().set(true);
		mch.getEnableText().set(true);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnable().set(true);
		mch.getEnableText().set(true);
		mch.setEnabledByParent(false);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
	}

	@Test
	void testReplacements() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Tankbuster on {target}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "Tankbuster on {target}");
			Assert.assertEquals(modified.getVisualText(), "Tankbuster on {target}");

			CalloutEvent modifiedWithArgs = mc.getModified(Map.of("target", "Foo"));
			Assert.assertEquals(modifiedWithArgs.getCallText(), "Tankbuster on Foo");
			Assert.assertEquals(modifiedWithArgs.getVisualText(), "Tankbuster on Foo");

			CalloutEvent modifiedWithOtherPlayer = mc.getModified(Map.of("target", new XivCombatant(0x123, "Foo")));
			Assert.assertEquals(modifiedWithOtherPlayer.getCallText(), "Tankbuster on Foo");
			Assert.assertEquals(modifiedWithOtherPlayer.getVisualText(), "Tankbuster on Foo");

			CalloutEvent modifiedWithThePlayer = mc.getModified(Map.of("target", new XivCombatant(0x123, "Bar", true, true, 1, null, null, null, 0, 0, 0, 0, 0)));
			Assert.assertEquals(modifiedWithThePlayer.getCallText(), "Tankbuster on YOU");
			Assert.assertEquals(modifiedWithThePlayer.getVisualText(), "Tankbuster on YOU");
		}
	}
}
