package gg.xp.events.actlines;

import gg.xp.events.actlines.events.AbilityCastStart;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line20Test extends AbstractACTLineTest<AbilityCastStart> {

	public Line20Test() {
		super(AbilityCastStart.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "20|2021-07-27T12:48:36.1310000-04:00|40024FCE|The Manipulator|13D0|Seed Of The Sky|E0000000||2.70|8.055649|-17.03842|10.58736|-4.792213E-05||5377da9551e7ca470709dc08e996bb75";
		AbilityCastStart event = expectEvent(goodLine);

		Assert.assertEquals(event.getAbility().getId(), 0x13D0);
		Assert.assertEquals(event.getSource().getId(), 0x40024FCEL);
		Assert.assertEquals(event.getTarget().getId(), 0xE0000000L);

		Assert.assertEquals(event.getAbility().getName(), "Seed Of The Sky");
		Assert.assertEquals(event.getSource().getName(), "The Manipulator");
		Assert.assertFalse(event.getSource().isEnvironment());
		Assert.assertEquals(event.getTarget().getName(), "ENVIRONMENT");
		Assert.assertTrue(event.getTarget().isEnvironment());
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
