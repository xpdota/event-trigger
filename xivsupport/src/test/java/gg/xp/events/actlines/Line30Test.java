package gg.xp.events.actlines;

import gg.xp.events.actlines.events.BuffRemoved;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line30Test extends AbstractACTLineTest<BuffRemoved> {

	public Line30Test() {
		super(BuffRemoved.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "30|2021-04-26T14:23:38.7560000-04:00|13b|Whispering Dawn|21.00|4000B283|Selene|10FF0002|The Player|4000016E|00|51893|49487||c7400f0eed1fe9d29834369affc22d3b";
		BuffRemoved event = expectEvent(goodLine);

		Assert.assertEquals(event.getBuff().getId(), 0x13B);
		Assert.assertEquals(event.getSource().getId(), 0x4000B283);
		Assert.assertEquals(event.getTarget().getId(), 0x10FF0002);

		Assert.assertEquals(event.getBuff().getName(), "Whispering Dawn");
		Assert.assertEquals(event.getSource().getName(), "Selene");
		Assert.assertEquals(event.getTarget().getName(), "The Player");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
