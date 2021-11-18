package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

// TODO
public class Line33Test extends AbstractACTLineTest<ActorControlEvent> {

	public Line33Test() {
		super(ActorControlEvent.class);
	}
	// TODO

	@Test
	public void positiveTest() {
		String goodLine = "33|2021-04-26T17:23:28.6780000-04:00|80034E6C|40000010|B5D|1234|5678|ABCD|f777621829447c53c82c9a24aa25348f";
		ActorControlEvent event = expectEvent(goodLine);


		Assert.assertEquals(event.getInstance(), 0x80034E6CL);
		Assert.assertEquals(event.getUpdateTypeRaw(), 0x8003L);
		Assert.assertEquals(event.getInstanceContentTypeRaw(), 0x4E6CL);
		Assert.assertEquals(event.getCommand(), 0x40000010L);
		Assert.assertEquals(event.getData0(), 0xB5DL);
		Assert.assertEquals(event.getData1(), 0x1234L);
		Assert.assertEquals(event.getData2(), 0x5678L);
		Assert.assertEquals(event.getData3(), 0xABCDL);

	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
