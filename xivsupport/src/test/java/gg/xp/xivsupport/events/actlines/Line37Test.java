package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.ActionSyncEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line37Test extends AbstractACTLineTest<ActionSyncEvent> {

	public Line37Test() {
		super(ActionSyncEvent.class);
	}

	@Test
	public void positiveTest() {
		String goodLine = "37|2021-11-30T21:18:08.7080000-08:00|4001651F|Vortexer|00007863|794467|892000|8840|10000|0||0.5340576|-323.9033|-7.999999|3.111104|0|0|0|03|08BD|0|41BF3734|10669D22|020000BD|0|41E9F1A7|1078DB88|030010F4|0|41E9F1A7|1078DB88||994cef7f5fc8f3961840bed49e5976c3";
		ActionSyncEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getSequenceId(), 0x00007863);

		Assert.assertEquals(event.getTarget().getId(), 0x4001651F);
		Assert.assertEquals(event.getTarget().getName(), "Vortexer");


	}

	// The functionality of actually getting the HP is elsewhere
	@Test
	public void testNoMaxHp() {
		String goodLine = "37|2021-11-30T21:18:08.7080000-08:00|4001651F|Vortexer|00007863|794467||8840|10000|0||0.5340576|-323.9033|-7.999999|3.111104|0|0|0|03|08BD|0|41BF3734|10669D22|020000BD|0|41E9F1A7|1078DB88|030010F4|0|41E9F1A7|1078DB88||994cef7f5fc8f3961840bed49e5976c3";
		ActionSyncEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getSequenceId(), 0x00007863);

		Assert.assertEquals(event.getTarget().getId(), 0x4001651F);
		Assert.assertEquals(event.getTarget().getName(), "Vortexer");
	}

	// The functionality of actually getting the HP is elsewhere
	@Test
	public void testNoDetails() {
		String goodLine = "37|2021-11-30T21:18:08.7080000-08:00|4001651F|Vortexer|00007863|794467||||||||||0|0|0|03|08BD|0|41BF3734|10669D22|020000BD|0|41E9F1A7|1078DB88|030010F4|0|41E9F1A7|1078DB88||994cef7f5fc8f3961840bed49e5976c3";
		ActionSyncEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getSequenceId(), 0x00007863);

		Assert.assertEquals(event.getTarget().getId(), 0x4001651F);
		Assert.assertEquals(event.getTarget().getName(), "Vortexer");
	}


	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
