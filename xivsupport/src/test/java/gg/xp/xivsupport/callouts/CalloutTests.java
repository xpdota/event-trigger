package gg.xp.xivsupport.callouts;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.models.XivStatusEffect;
import gg.xp.xivsupport.models.XivWorld;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CalloutTests {

	private final CalloutProcessor proc;
	
	{
		MutablePicoContainer pico = XivMain.testingMasterInit();
		pico.getComponent(EventMaster.class).pushEventAndWait(new InitEvent());
		proc = pico.getComponent(CalloutProcessor.class);
	}

	@Test
	void testNonModifiedCallout() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar");
		CalloutEvent modified = proc.processCallout(mc.getModified());
		Assert.assertEquals(modified.getCallText(), "Bar");
		Assert.assertEquals(modified.getVisualText(), "Bar");
	}

	@Test
	void testSameFlag1() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		{
			ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
			mc.attachHandle(mch);
			Assert.assertFalse(mch.getSameText().get());
			Assert.assertFalse(mch.getSameText().isSet());
		}
	}

	@Test
	void testSameFlag2() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar", "Bar2", (e) -> true);
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		{
			ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
			mc.attachHandle(mch);
			Assert.assertFalse(mch.getSameText().get());
			Assert.assertFalse(mch.getSameText().isSet());
			Assert.assertEquals(mch.getEffectiveTts(), "Bar");
			Assert.assertEquals(mch.getEffectiveText(), "Bar2");
			mch.getTextSetting().set("Bar");
			Assert.assertEquals(mch.getEffectiveTts(), "Bar");
			Assert.assertEquals(mch.getEffectiveText(), "Bar");
		}
		{
			ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
			mc.attachHandle(mch);
			Assert.assertFalse(mch.getSameText().get());
			Assert.assertFalse(mch.getSameText().isSet());
			Assert.assertEquals(mch.getEffectiveTts(), "Bar");
			Assert.assertEquals(mch.getEffectiveText(), "Bar");
			mch.getTtsSetting().set("Bar3");
			mch.getSameText().set(true);
			Assert.assertEquals(mch.getEffectiveTts(), "Bar3");
			Assert.assertEquals(mch.getEffectiveText(), "Bar3");
		}
		{
			ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
			mc.attachHandle(mch);
			Assert.assertTrue(mch.getSameText().get());
			Assert.assertTrue(mch.getSameText().isSet());
			Assert.assertEquals(mch.getEffectiveTts(), "Bar3");
			Assert.assertEquals(mch.getEffectiveText(), "Bar3");
			mch.getSameText().set(false);
			Assert.assertEquals(mch.getEffectiveTts(), "Bar3");
			Assert.assertEquals(mch.getEffectiveText(), "Bar");
		}
		{
			ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
			mc.attachHandle(mch);
			Assert.assertFalse(mch.getSameText().get());
			Assert.assertTrue(mch.getSameText().isSet());
			Assert.assertEquals(mch.getEffectiveTts(), "Bar3");
			Assert.assertEquals(mch.getEffectiveText(), "Bar");
		}
	}

	@Test
	void testModifiedCallout() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Bar");
			Assert.assertEquals(modified.getVisualText(), "Bar");
		}
		mch.getSameText().set(true);
		mch.getTtsSetting().set("123");
		mch.getTextSetting().set("456");
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertEquals(modified.getVisualText(), "123");
		}
		mch.getSameText().set(false);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertEquals(modified.getVisualText(), "456");
		}
		mch.getSameText().set(true);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertEquals(modified.getVisualText(), "123");
		}
		mch.getSameText().set(false);
		mch.getEnableTts().set(false);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertNull(modified.getCallText());
			Assert.assertEquals(modified.getVisualText(), "456");
		}
		mch.getEnableTts().set(true);
		mch.getEnableText().set(false);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "123");
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnableTts().set(false);
		mch.getEnableText().set(false);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnable().set(false);
		mch.getEnableText().set(true);
		mch.getEnableText().set(true);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnable().set(true);
		mch.getEnableText().set(true);
		mch.setEnabledByParent(false);
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
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
			CalloutEvent scratch = proc.processCallout(mc.getModified(Map.of("target", "Foo")));

			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Tankbuster on Error");
			Assert.assertEquals(modified.getVisualText(), "Tankbuster on Error");


			CalloutEvent modifiedWithArgs = proc.processCallout(mc.getModified(modified, Map.of("target", "Foo")));
			Assert.assertEquals(modifiedWithArgs.getCallText(), "Tankbuster on Foo");
			Assert.assertEquals(modifiedWithArgs.getVisualText(), "Tankbuster on Foo");

			CalloutEvent modifiedWithOtherPlayer = proc.processCallout(mc.getModified(Map.of("target", new XivCombatant(0x123, "Foo"))));
			Assert.assertEquals(modifiedWithOtherPlayer.getCallText(), "Tankbuster on Foo");
			Assert.assertEquals(modifiedWithOtherPlayer.getVisualText(), "Tankbuster on Foo");

			CalloutEvent modifiedWithThePlayer = proc.processCallout(mc.getModified(Map.of("target", new XivPlayerCharacter(0x123, "Bar", Job.WHM, XivWorld.of(), true, 1, null, null, null, 0, 0, 0, 0, 0, 0))));
			Assert.assertEquals(modifiedWithThePlayer.getCallText(), "Tankbuster on YOU");
			Assert.assertEquals(modifiedWithThePlayer.getVisualText(), "Tankbuster on YOU");
		}
	}

	@Test
	void testReplacementsDoubleBracket() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Tankbuster on {{ {target.name + \"Bar\"}() }}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
//			CalloutEvent scratch = proc.processCallout(mc.getModified(Map.of("target", "Foo")));

			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Tankbuster on Error");
			Assert.assertEquals(modified.getVisualText(), "Tankbuster on Error");


//			CalloutEvent modifiedWithArgs = proc.processCallout(mc.getModified(modified, Map.of("target", "Foo")));
//			Assert.assertEquals(modifiedWithArgs.getCallText(), "Tankbuster on FooBar");
//			Assert.assertEquals(modifiedWithArgs.getVisualText(), "Tankbuster on FooBar");

			CalloutEvent modifiedWithOtherPlayer = proc.processCallout(mc.getModified(Map.of("target", new XivCombatant(0x123, "Foo"))));
			Assert.assertEquals(modifiedWithOtherPlayer.getCallText(), "Tankbuster on FooBar");
			Assert.assertEquals(modifiedWithOtherPlayer.getVisualText(), "Tankbuster on FooBar");

			CalloutEvent modifiedWithThePlayer = proc.processCallout(mc.getModified(Map.of("target", new XivCombatant(0x123, "Bar", true, true, 1, null, null, null, 0, 0, 0, 0, 0, 0))));
			Assert.assertEquals(modifiedWithThePlayer.getCallText(), "Tankbuster on BarBar");
			Assert.assertEquals(modifiedWithThePlayer.getVisualText(), "Tankbuster on BarBar");
		}
	}

	@Test
	void testReplacementsDoubleBracketUnbalancedLeft() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Tankbuster on {{ { }} { target }");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
//			CalloutEvent scratch = proc.processCallout(mc.getModified(Map.of("target", "Foo")));

			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Tankbuster on Error Error");
			Assert.assertEquals(modified.getVisualText(), "Tankbuster on Error Error");


			CalloutEvent modifiedWithArgs = proc.processCallout(mc.getModified(modified, Map.of("target", "Foo")));
			Assert.assertEquals(modifiedWithArgs.getCallText(), "Tankbuster on Error Foo");
			Assert.assertEquals(modifiedWithArgs.getVisualText(), "Tankbuster on Error Foo");

			CalloutEvent modifiedWithOtherPlayer = proc.processCallout(mc.getModified(Map.of("target", new XivCombatant(0x123, "Foo"))));
			Assert.assertEquals(modifiedWithOtherPlayer.getCallText(), "Tankbuster on Error Foo");
			Assert.assertEquals(modifiedWithOtherPlayer.getVisualText(), "Tankbuster on Error Foo");

			CalloutEvent modifiedWithThePlayer = proc.processCallout(mc.getModified(Map.of("target", new XivPlayerCharacter(0x123, "Bar", Job.WHM, XivWorld.of(), true, 1, null, null, null, 0, 0, 0, 0, 0, 0))));
			Assert.assertEquals(modifiedWithThePlayer.getCallText(), "Tankbuster on Error YOU");
			Assert.assertEquals(modifiedWithThePlayer.getVisualText(), "Tankbuster on Error YOU");
		}
	}

	@Test
	public void testReplacementsAdvanced() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "{event.getBuff().getId()}:{event.getBuff().getName().toUpperCase()} {event.getInitialDuration()} {event.getStacks()} {event.isRefresh()} {event.getSource().getId()}:{event.getSource().getName()} {event.getTarget().getId()}:{event.getTarget().getName()}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			BuffApplied ba = new BuffApplied(new XivStatusEffect(123, "FooStatus"), 15, new XivCombatant(1, "Cbt1"), new XivCombatant(2, "Cbt2"), 5, 5, false);

			CalloutEvent modified = proc.processCallout(mc.getModified(ba));
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
				CalloutEvent modified2 = proc.processCallout(mc.getModified(ba));
				Assert.assertEquals(modified2.getCallText(), "123:FOOSTATUS 15.0 5 true 1:Cbt1 2:Cbt2");
				Assert.assertEquals(modified2.getVisualText(), "123:FOOSTATUS 15.0 5 true 1:Cbt1 2:Cbt2");
			}
		}
	}

	@Test
	void testConcurrent() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "{val1} {val2}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);

		CalloutEvent ce1 = proc.processCallout(mc.getModified(Map.of("val1", 123, "val2", 456)));
		Assert.assertEquals(ce1.getCallText(), "123 456");
		Assert.assertEquals(ce1.getVisualText(), "123 456");

		CalloutEvent ce2 = proc.processCallout(mc.getModified(Map.of("val1", 789)));

		Assert.assertEquals(ce1.getCallText(), "123 456");
		Assert.assertEquals(ce2.getCallText(), "789 Error");

		Assert.assertEquals(ce1.getVisualText(), "123 456");
		Assert.assertEquals(ce2.getVisualText(), "789 Error");
	}

	@Test
	@Ignore // This doesn't seem to work well
	public void testReplacementsAdvanced2() {
//		ModifiableCallout mc = new ModifiableCallout("Foo", "{String.format(\"%.02X\", event.getBuff().getId()}");
		ModifiableCallout mc = new ModifiableCallout("Foo", "{String.format(\"%X\", event.getBuff().getId(), 123L)} : {\"%X\".formatted(event.getBuff().getId())} : {\"asdf\".toUpperCase()}");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		{
			BuffApplied ba = new BuffApplied(new XivStatusEffect(123, "FooStatus"), 15, new XivCombatant(1, "Cbt1"), new XivCombatant(2, "Cbt2"), 5);
			String.format("%X", ba.getBuff().getId());
//			"X".formatted(ba.getBuff().getId());

			CalloutEvent modified = proc.processCallout(mc.getModified(ba));
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
		CalloutEvent target = proc.processCallout(mc.getModified(Map.of("target", new Object() {
			@Override
			public String toString() {
				return String.valueOf(counter.get());
			}
		})));
		Assert.assertEquals(target.getVisualText(), "Tankbuster on 1");
		counter.incrementAndGet();
		Assert.assertEquals(target.getVisualText(), "Tankbuster on 2");
		counter.incrementAndGet();
		Assert.assertEquals(target.getVisualText(), "Tankbuster on 3");

	}

	@Test
	void testEnableDisable() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar");
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		Assert.assertTrue(mch.getEnable().get());
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Bar");
			Assert.assertEquals(modified.getVisualText(), "Bar");
		}
		mch.getEnable().set(false);
		Assert.assertFalse(mch.getEnable().get());
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnable().set(true);
		Assert.assertTrue(mch.getEnable().get());
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Bar");
			Assert.assertEquals(modified.getVisualText(), "Bar");
		}
	}

	@Test
	void testDisabledByDefault() {
		ModifiableCallout mc = new ModifiableCallout("Foo", "Bar").disabledByDefault();
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		BooleanSetting enableAll = new BooleanSetting(pers, "foo", true);
		ModifiedCalloutHandle mch = new ModifiedCalloutHandle(pers, "fooCallout", mc, enableAll, enableAll);
		mc.attachHandle(mch);
		Assert.assertFalse(mch.getEnable().get());
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
		mch.getEnable().set(true);
		Assert.assertTrue(mch.getEnable().get());
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertEquals(modified.getCallText(), "Bar");
			Assert.assertEquals(modified.getVisualText(), "Bar");
		}
		mch.getEnable().set(false);
		Assert.assertFalse(mch.getEnable().get());
		{
			CalloutEvent modified = proc.processCallout(mc.getModified());
			Assert.assertNull(modified.getCallText());
			Assert.assertNull(modified.getVisualText());
		}
	}
}
