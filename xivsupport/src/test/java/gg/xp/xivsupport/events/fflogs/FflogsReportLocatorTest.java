package gg.xp.xivsupport.events.fflogs;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class FflogsReportLocatorTest {

	@Test
	void testNonFightUrl() {
		FflogsReportLocator locator = FflogsReportLocator.fromURL("https://www.fflogs.com/reports/JX23Kd1YRyBtxkDZ");
		Assert.assertEquals(locator.report(), "JX23Kd1YRyBtxkDZ");
		Assert.assertNull(locator.fight());
		Assert.assertFalse(locator.fightSpecified());
	}

	@Test
	void testOldFightUrl() {
		FflogsReportLocator locator = FflogsReportLocator.fromURL("https://www.fflogs.com/reports/JX23Kd1YRyBtxkDZ#fight=3");
		Assert.assertEquals(locator.report(), "JX23Kd1YRyBtxkDZ");
		Assert.assertEquals(locator.fight(), 3);
		Assert.assertTrue(locator.fightSpecified());
	}

	@Test
	void testNewFightUrl() {
		FflogsReportLocator locator = FflogsReportLocator.fromURL("https://www.fflogs.com/reports/JX23Kd1YRyBtxkDZ?fight=3");
		Assert.assertEquals(locator.report(), "JX23Kd1YRyBtxkDZ");
		Assert.assertEquals(locator.fight(), 3);
		Assert.assertTrue(locator.fightSpecified());
	}

	@Test
	void testNewFightUrlWithJunk() {
		FflogsReportLocator locator = FflogsReportLocator.fromURL("https://www.fflogs.com/reports/JX23Kd1YRyBtxkDZ?foo=bar&fight=3&baz=true");
		Assert.assertEquals(locator.report(), "JX23Kd1YRyBtxkDZ");
		Assert.assertEquals(locator.fight(), 3);
		Assert.assertTrue(locator.fightSpecified());
	}


}