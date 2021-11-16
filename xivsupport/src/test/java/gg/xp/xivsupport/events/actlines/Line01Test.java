package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Ignore // Using ACTWS for zone/player change
public class Line01Test extends AbstractACTLineTest<ZoneChangeEvent> {

	public Line01Test() {
		super(ZoneChangeEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "01|2021-04-26T14:13:17.9930000-04:00|326|Kugane Ohashi|b9f401c0aa0b8bc454b239b201abc1b8";
		ZoneChangeEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getZone().getId(), 0x326);
		Assert.assertEquals(event.getZone().getName(), "Kugane Ohashi");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
