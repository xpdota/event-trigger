package gg.xp.xivsupport.events.actlines;

import gg.xp.xivsupport.events.actlines.events.LimitBreakGaugeEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Line36Test extends AbstractACTLineTest<LimitBreakGaugeEvent> {

	public Line36Test() {
		super(LimitBreakGaugeEvent.class);
	}

	/**
	 * One partial bar
	 */
	@Test
	public void positiveTest1() {
		String goodLine = "36|2024-07-30T18:14:21.3130000-05:00|24B8|3|";
		LimitBreakGaugeEvent event = expectEvent(goodLine);
		Assert.assertEquals(event.getTotalBars(), 3);
		Assert.assertEquals(event.getCurrentRawValue(), 0x24B8);
		Assert.assertEquals(event.getMaxRawValue(), 30_000);
		Assert.assertEquals(event.getFullBars(), 0);
		MatcherAssert.assertThat((double) event.getCurrentValue(), Matchers.closeTo(0.94, 0.0001));
		Assert.assertEquals(event.getPrimaryValue(), "9400 / 30000 (No LB)");
		Assert.assertTrue(event.isLbAvailable());
		Assert.assertFalse(event.isLbUsable());
		Assert.assertFalse(event.isEmpty());
		Assert.assertFalse(event.isFull());
	}

	/**
	 * Two bars and change.
	 */
	@Test
	public void positiveTest2() {
		String goodLine = "36|2024-07-30T18:17:33.3080000-05:00|5E10|3|";
		LimitBreakGaugeEvent event = expectEvent(goodLine);
		Assert.assertEquals(event.getTotalBars(), 3);
		Assert.assertEquals(event.getCurrentRawValue(), 0x5E10);
		Assert.assertEquals(event.getMaxRawValue(), 30_000);
		Assert.assertEquals(event.getFullBars(), 2);
		MatcherAssert.assertThat((double) event.getCurrentValue(), Matchers.closeTo(2.408, 0.0001));
		Assert.assertEquals(event.getPrimaryValue(), "24080 / 30000 (LB2)");
		Assert.assertTrue(event.isLbAvailable());
		Assert.assertTrue(event.isLbUsable());
		Assert.assertFalse(event.isEmpty());
		Assert.assertFalse(event.isFull());
	}

	/**
	 * Three full bars
	 */
	@Test
	public void positiveTest3() {
		String goodLine = "36|2024-07-30T18:18:39.3320000-05:00|7530|3|";
		LimitBreakGaugeEvent event = expectEvent(goodLine);
		Assert.assertEquals(event.getTotalBars(), 3);
		Assert.assertEquals(event.getCurrentRawValue(), 0x7530);
		Assert.assertEquals(event.getMaxRawValue(), 30_000);
		Assert.assertEquals(event.getFullBars(), 3);
		Assert.assertEquals(event.getCurrentValue(), 3.0);
		Assert.assertEquals(event.getPrimaryValue(), "30000 / 30000 (LB3)");
		Assert.assertTrue(event.isLbAvailable());
		Assert.assertTrue(event.isLbUsable());
		Assert.assertFalse(event.isEmpty());
		Assert.assertTrue(event.isFull());
	}

	/**
	 * Empty bars immediately after using LB3.
	 */
	@Test
	public void positiveTest4() {
		String goodLine = "36|2024-07-30T18:21:11.4730000-05:00|0000|3|";
		LimitBreakGaugeEvent event = expectEvent(goodLine);
		Assert.assertEquals(event.getTotalBars(), 3);
		Assert.assertEquals(event.getCurrentRawValue(), 0);
		Assert.assertEquals(event.getMaxRawValue(), 30_000);
		Assert.assertEquals(event.getFullBars(), 0);
		Assert.assertEquals(event.getCurrentValue(), 0.0);
		Assert.assertEquals(event.getPrimaryValue(), "0 / 30000 (No LB)");
		Assert.assertTrue(event.isLbAvailable());
		Assert.assertFalse(event.isLbUsable());
		Assert.assertTrue(event.isEmpty());
		Assert.assertFalse(event.isFull());
	}

	/**
	 * No LB bar
	 */
	@Test
	public void positiveTest5() {
		String goodLine = "36|2024-09-07T08:28:50.5240000-07:00|0000|0|";
		LimitBreakGaugeEvent event = expectEvent(goodLine);
		Assert.assertEquals(event.getTotalBars(), 0);
		Assert.assertEquals(event.getCurrentRawValue(), 0);
		Assert.assertEquals(event.getMaxRawValue(), 0);
		Assert.assertEquals(event.getFullBars(), 0);
		Assert.assertEquals(event.getCurrentValue(), 0.0);
		Assert.assertEquals(event.getPrimaryValue(), "0 / 0 (LB Unavailable)");
		Assert.assertFalse(event.isLbAvailable());
		Assert.assertFalse(event.isLbUsable());
		Assert.assertTrue(event.isEmpty());
		Assert.assertTrue(event.isFull());
	}

	@Test
	public void negativeTest() {
		assertNoEvent("25|2021-11-06T09:46:46.4900000-07:00|107361AF|Foo Bar|200524E|Item_524E|107361AD|The Target|33C|20000|1B|270A8000|0|0|0|0|0|0|0|0|0|0|0|0|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|170781|170781|10000|10000|0|1000|-46.33868|20.93576|1.6|-1.167042|000BACE5|0|104137929bde2acb55f6b35d58ffb560");
	}
}
