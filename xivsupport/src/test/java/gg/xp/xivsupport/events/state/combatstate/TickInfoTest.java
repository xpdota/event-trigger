package gg.xp.xivsupport.events.state.combatstate;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Instant;

import static org.testng.Assert.*;

public class TickInfoTest {

	@Test
	void testInterval() {
		Instant basis = Instant.ofEpochMilli(10_000);
		TickInfo tickInfo = new TickInfo(basis.toEpochMilli(), 3_000);
		Assert.assertEquals(tickInfo.getMsToNextTick(basis), 0);
		Assert.assertEquals(tickInfo.getMsFromLastTick(basis), 0);
		Assert.assertEquals(tickInfo.getNextTick(basis), basis);
		Assert.assertEquals(tickInfo.getPrevTick(basis), basis);

		Instant oneSecondLater = Instant.ofEpochMilli(11_000);
		Instant nextTick = Instant.ofEpochMilli(13_000);

		Assert.assertEquals(tickInfo.getMsToNextTick(oneSecondLater), 2_000);
		Assert.assertEquals(tickInfo.getMsFromLastTick(oneSecondLater), 1_000);
		Assert.assertEquals(tickInfo.getNextTick(oneSecondLater), nextTick);
		Assert.assertEquals(tickInfo.getPrevTick(oneSecondLater), basis);

		Instant oneSecondBefore  = Instant.ofEpochMilli(9_000);
		Instant prevTick = Instant.ofEpochMilli(7_000);

		Assert.assertEquals(tickInfo.getMsToNextTick(oneSecondBefore), 1_000);
		Assert.assertEquals(tickInfo.getMsFromLastTick(oneSecondBefore), 2_000);
		Assert.assertEquals(tickInfo.getNextTick(oneSecondBefore), basis);
		Assert.assertEquals(tickInfo.getPrevTick(oneSecondBefore), prevTick);

		Instant fiveSecondsAfter = Instant.ofEpochMilli(15_000);
		Instant tickAfterNext = Instant.ofEpochMilli(16_000);

		Assert.assertEquals(tickInfo.getMsToNextTick(fiveSecondsAfter), 1_000);
		Assert.assertEquals(tickInfo.getMsFromLastTick(fiveSecondsAfter), 2_000);
		Assert.assertEquals(tickInfo.getNextTick(fiveSecondsAfter), tickAfterNext);
		Assert.assertEquals(tickInfo.getPrevTick(fiveSecondsAfter), nextTick);



	}

}