package gg.xp.events.actlines;

import gg.xp.events.actlines.events.HeadMarkerEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line27Test extends AbstractACTLineTest<HeadMarkerEvent> {

	public Line27Test() {
		super(HeadMarkerEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "27|2021-05-11T13:48:45.3370000-04:00|40000950|Copied Knave|0000|0000|0117|0000|0000|0000||fa2e93fccf397a41aac73a3a38aa7410";
		HeadMarkerEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getMarkerId(), 0x117);
		Assert.assertEquals(event.getTarget().getId(), 0x40000950);

		Assert.assertEquals(event.getTarget().getName(), "Copied Knave");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
