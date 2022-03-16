package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line21Test extends AbstractACTLineTest<AbilityUsedEvent> {

	public Line21Test() {
		super(AbilityUsedEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "21|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|1|104137929bde2acb55f6b35d58ffb560";
		AbilityUsedEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getAbility().getId(), 0x200524E);
		Assert.assertEquals(event.getSource().getId(), 0x107361AF);
		Assert.assertEquals(event.getTarget().getId(), 0x107361AD);
		Assert.assertEquals(event.getSequenceId(), 0x000BACE5);

		Assert.assertEquals(event.getTargetIndex(), 0);
		Assert.assertEquals(event.getNumberOfTargets(), 1);

		Assert.assertEquals(event.getAbility().getName(), "Item_524E");
		Assert.assertEquals(event.getSource().getName(), "Foo Bar");
		Assert.assertEquals(event.getTarget().getName(), "The Target");
	}

	@Test
	public void damageTests() {
		String lineTemplate = "21|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|%x|%x|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|1|104137929bde2acb55f6b35d58ffb560";
		{
			AbilityUsedEvent event = expectEvent(String.format(lineTemplate, 0x750003, 0x47280000));
			long damage = event.getDamage();
			Assert.assertEquals(damage, 18216);
		}
		{
			AbilityUsedEvent event = expectEvent(String.format(lineTemplate, 0x750003, 0x426B4001));
			long damage = event.getDamage();
			Assert.assertEquals(damage, 82538);
		}
		{
			AbilityUsedEvent event = expectEvent(String.format(lineTemplate, 0x750003, 0x565D0000));
			long damage = event.getDamage();
			Assert.assertEquals(damage, 22109);
		}
//		{
//			AbilityUsedEvent event = expectEvent(String.format(lineTemplate, 0x313, 0x4C3));
//			long damage = event.getDamage();
//			Assert.assertEquals(damage, 15732);
//		}
		{
			AbilityUsedEvent event = expectEvent(String.format(lineTemplate, 0x33, 0));
			long damage = event.getDamage();
			Assert.assertEquals(damage, 0);
		}
		{
			AbilityUsedEvent event = expectEvent(String.format(lineTemplate, 0, 0));
			long damage = event.getDamage();
			Assert.assertEquals(damage, 0);
		}

	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|1|104137929bde2acb55f6b35d58ffb560");
	}

	@Test
	public void testHugeDamage() {
		String goodLine = "22|2022-01-20T18:11:59.9720000-08:00|40031036|Sparkfledged|66E6|Ashen Eye|10679943|Foo Bar|3|967F4098|1B|66E68000|0|0|0|0|0|0|0|0|0|0|0|0|61186|61186|10000|10000|||100.14|91.29|-0.02|3.08|69200|69200|10000|10000|||100.11|106.76|0.00|3.14|000133E7|2|4|64f5cd5254f9f411";
		AbilityUsedEvent event = expectEvent(goodLine);
		long damage = event.getDamage();
		MatcherAssert.assertThat(damage, Matchers.greaterThan(0L));


	}
}
