package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line25Test extends AbstractACTLineTest<EntityKilledEvent> {

	public Line25Test() {
		super(EntityKilledEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "25|2021-07-27T13:11:11.6840000-04:00|4000016E|Angra Mainyu|10FF0002|Player Name||0b79669140c20f9aa92ad5559be75022";
		EntityKilledEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getSource().getId(), 0x10FF0002L);
		Assert.assertEquals(event.getTarget().getId(), 0x4000016EL);

		Assert.assertEquals(event.getSource().getName(), "Player Name");
		Assert.assertEquals(event.getTarget().getName(), "Angra Mainyu");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("22|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
