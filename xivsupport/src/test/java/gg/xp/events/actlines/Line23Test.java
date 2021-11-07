package gg.xp.events.actlines;

import gg.xp.events.actlines.events.AbilityCastCancel;
import gg.xp.events.actlines.events.AbilityCastStart;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line23Test extends AbstractACTLineTest<AbilityCastCancel> {

	public Line23Test() {
		super(AbilityCastCancel.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "23|2021-07-27T13:04:39.0930000-04:00|40000132|Garm|D10|The Dragon's Voice|Interrupted||bd936fde66bab0e8cf2874ebd75df77c";
		AbilityCastCancel event = expectEvent(goodLine);

		Assert.assertEquals(event.getAbility().getId(), 0xD10);
		Assert.assertEquals(event.getSource().getId(), 0x40000132);

		Assert.assertEquals(event.getAbility().getName(), "The Dragon's Voice");
		Assert.assertEquals(event.getSource().getName(), "Garm");

		Assert.assertEquals(event.getReason(), "Interrupted");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
