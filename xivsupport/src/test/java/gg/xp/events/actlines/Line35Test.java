package gg.xp.events.actlines;

import gg.xp.events.actlines.events.TetherEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line35Test extends AbstractACTLineTest<TetherEvent> {

	public Line35Test() {
		super(TetherEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "35|2021-06-13T17:41:34.2230000-04:00|10FF0001|First Player|10FF0002|Second Player|0000|0000|006E|1068E3EF|000F|0000||c022382c6803d1d6c1f84681b7d8db20";
		TetherEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getId(), 0x6E);
		Assert.assertEquals(event.getSource().getId(), 0x10FF0001);
		Assert.assertEquals(event.getTarget().getId(), 0x10FF0002);

		Assert.assertEquals(event.getSource().getName(), "First Player");
		Assert.assertEquals(event.getTarget().getName(), "Second Player");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
