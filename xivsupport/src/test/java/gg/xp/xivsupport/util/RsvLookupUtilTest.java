package gg.xp.xivsupport.util;

import gg.xp.xivdata.data.*;
import gg.xp.xivdata.data.rsv.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.function.Function;

public class RsvLookupUtilTest {

	private static final RsvLibrary fakeRsvLibrary = item -> {
		if (item.startsWith("_rsv_")) {
			return "foo_" + item.substring(5);
		}
		else {
			return null;
		}
	};

	@Test
	synchronized void testRsvLookupGoodInitial() {
		DefaultRsvLibrary.setLibrary(new NoopRsvLibrary());
		String value = RsvLookupUtil.lookup(0x123, "Good Name", ActionLibrary::forId, ActionInfo::name);
		Assert.assertEquals(value, "Good Name");
	}

	@Test
	synchronized void testRsvLookupDataFileGood() {
		DefaultRsvLibrary.setLibrary(new NoopRsvLibrary());
		String value = RsvLookupUtil.lookup(0x8, null, ActionLibrary::forId, ActionInfo::name);
		Assert.assertEquals(value, "Shot");
	}

	@Test
	synchronized void testRsvLookupDataFileGoodOverridesRsv() {
		DefaultRsvLibrary.setLibrary(new NoopRsvLibrary());
		String value = RsvLookupUtil.lookup(0x8, "_rsv_123", ActionLibrary::forId, ActionInfo::name);
		Assert.assertEquals(value, "Shot");
	}

	@Test
	synchronized void testPrimaryRSV() {
		DefaultRsvLibrary.setLibrary(fakeRsvLibrary);
		String value = RsvLookupUtil.lookup(0x88888, "_rsv_asdf", ActionLibrary::forId, ActionInfo::name);
		Assert.assertEquals(value, "foo_asdf");
	}

	@Test
	synchronized void testGoodInfoNameOverridesPrimaryRSV() {
		DefaultRsvLibrary.setLibrary(fakeRsvLibrary);
		String value = RsvLookupUtil.lookup(0x8, "_rsv_asdf", ActionLibrary::forId, ActionInfo::name);
		Assert.assertEquals(value, "Shot");
	}

	@Test
	synchronized void testGoodNameOverridesSecondaryRSV() {
		DefaultRsvLibrary.setLibrary(fakeRsvLibrary);
		String value = RsvLookupUtil.lookup(0x8, "Bar", ignored -> "_rsv_zxcv", Function.identity());
		Assert.assertEquals(value, "Bar");
	}

	@Test
	synchronized void testPrimaryRsvOverridesSecondaryRSV() {
		DefaultRsvLibrary.setLibrary(fakeRsvLibrary);
		String value = RsvLookupUtil.lookup(0x8, "_rsv_asdf", ignored -> "_rsv_zxcv", Function.identity());
		Assert.assertEquals(value, "foo_asdf");
	}


	@Test
	synchronized void testRsvLookupUnknown() {
		DefaultRsvLibrary.setLibrary(new NoopRsvLibrary());
		String value = RsvLookupUtil.lookup(0x55555, null, ActionLibrary::forId, ActionInfo::name);
		Assert.assertEquals(value, "Unknown_55555");
	}

}