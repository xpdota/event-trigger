package gg.xp.events.actlines;

import gg.xp.events.actlines.events.ZeroLogLineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line00Test extends AbstractACTLineTest<ZeroLogLineEvent> {

	private static final Logger log = LoggerFactory.getLogger(Line00Test.class);

	public Line00Test() {
		super(ZeroLogLineEvent.class);
	}

	@Test
	public void positiveTest() {
		log.info("XXX: {}", this);
		String goodLine = "00|2021-04-26T14:12:30.0000000-04:00|0839|Stuff|You change to warrior.|d8c450105ea12854e26eb687579564df";
		ZeroLogLineEvent event = expectEvent(goodLine);

		Assert.assertEquals(event.getCode(), 0x839);
		Assert.assertEquals(event.getName(), "Stuff");
		Assert.assertEquals(event.getLine(), "You change to warrior.");
	}

	@Test
	public void negativeTest() {
		log.info("XXX: {}", this);
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
