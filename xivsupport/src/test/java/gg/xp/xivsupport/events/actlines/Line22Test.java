package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line22Test extends AbstractACTLineTest<AbilityUsedEvent> {

	public Line22Test() {
		super(AbilityUsedEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "22|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560";
		AbilityUsedEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getAbility().getId(), 0x200524E);
		Assert.assertEquals(event.getSource().getId(), 0x107361AF);
		Assert.assertEquals(event.getTarget().getId(), 0x107361AD);

		Assert.assertEquals(event.getAbility().getName(), "Item_524E");
		Assert.assertEquals(event.getSource().getName(), "Foo Bar");
		Assert.assertEquals(event.getTarget().getName(), "The Target");
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
