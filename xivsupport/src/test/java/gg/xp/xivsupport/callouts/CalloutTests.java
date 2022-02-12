package gg.xp.xivsupport.callouts;

import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
		mch.getSameText().set(false);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertEquals(modified.getVisualText(), "456");
		}
		mch.getSameText().set(true);
		{
			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertEquals(modified.getVisualText(), "123");
		}
		mch.getSameText().set(false);
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
			CalloutEvent scratch = mc.getModified(Map.of("target", "Foo"));

			CalloutEvent modified = mc.getModified();
			Assert.assertEquals(modified.getCallText(), "Tankbuster on Error");
			Assert.assertEquals(modified.getVisualText(), "Tankbuster on Error");


			CalloutEvent modifiedWithArgs = mc.getModified(modified, Map.of("target", "Foo"));
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

	@Test
	public static void testReplacementsAdvanced() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "{event.getBuff().getId()}:{event.getBuff().getName().toUpperCase()} {event.getInitialDuration()} {event.getStacks()} {event.isRefresh()} {event.getSource().getId()}:{event.getSource().getName()} {event.getTarget().getId()}:{event.getTarget().getName()}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			BuffApplied ba = new BuffApplied(new XivStatusEffect(123, "FooStatus"), 15, new XivCombatant(1, "Cbt1"), new XivCombatant(2, "Cbt2"), 5, 5, false);

			CalloutEvent modified = mc.getModified(ba);
			{
				Assert.assertEquals(modified.getCallText(), "123:FOOSTATUS 15.0 5 false 1:Cbt1 2:Cbt2");
				Assert.assertEquals(modified.getVisualText(), "123:FOOSTATUS 15.0 5 false 1:Cbt1 2:Cbt2");
			}

			ba.setIsRefresh(true);
			{
				// Same callout - should update visual text only
				Assert.assertEquals(modified.getCallText(), "123:FOOSTATUS 15.0 5 false 1:Cbt1 2:Cbt2");
				Assert.assertEquals(modified.getVisualText(), "123:FOOSTATUS 15.0 5 true 1:Cbt1 2:Cbt2");
			}
			{
				// New callout - should update both
				CalloutEvent modified2 = mc.getModified(ba);
				Assert.assertEquals(modified2.getCallText(), "123:FOOSTATUS 15.0 5 true 1:Cbt1 2:Cbt2");
				Assert.assertEquals(modified2.getVisualText(), "123:FOOSTATUS 15.0 5 true 1:Cbt1 2:Cbt2");
			}
		}
	}
	@Test
	@Ignore // This doesn't seem to work well
	public static void testReplacementsAdvanced2() {
//		ModifiableCallout mc = new ModifiableCallout("Foo", "{String.format(\"%.02X\", event.getBuff().getId()}");
		ModifiableCallout mc = new ModifiableCallout("Foo", "{String.format(\"%X\", event.getBuff().getId(), 123L)} : {\"%X\".formatted(event.getBuff().getId())} : {\"asdf\".toUpperCase()}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			BuffApplied ba = new BuffApplied(new XivStatusEffect(123, "FooStatus"), 15, new XivCombatant(1, "Cbt1"), new XivCombatant(2, "Cbt2"), 5);
			String.format("%X", ba.getBuff().getId());
			"X".formatted(ba.getBuff().getId());

			CalloutEvent modified = mc.getModified(ba);
			{
				Assert.assertEquals(modified.getCallText(), "123:FooStatus 15 5 false 1:Cbt1 2:Cbt2");
				Assert.assertEquals(modified.getVisualText(), "123:FooStatus 15 5 false 1:Cbt1 2:Cbt2");
			}
		}
	}

	@Test
	void testDynamic() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Tankbuster on {target}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);


		AtomicInteger counter = new AtomicInteger(1);
		CalloutEvent target = mc.getModified(Map.of("target", new Object() {
			@Override
			public String toString() {
				return String.valueOf(counter.get());
			}
		}));
		Assert.assertEquals(target.getVisualText(), "Tankbuster on 1");
		counter.incrementAndGet();
		Assert.assertEquals(target.getVisualText(), "Tankbuster on 2");
		counter.incrementAndGet();
		Assert.assertEquals(target.getVisualText(), "Tankbuster on 3");

	}
}
