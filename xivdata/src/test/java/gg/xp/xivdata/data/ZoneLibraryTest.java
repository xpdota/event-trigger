package gg.xp.xivdata.data;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class ZoneLibraryTest {
	@Test
	void testZoneLibrary() {
		Map<Integer, ZoneInfo> all = ZoneLibrary.getFileValues();
		ZoneInfo alex = all.get(887);
		Assert.assertEquals(alex.name(), "the Epic of Alexander (Ultimate)");
		Assert.assertEquals(alex.dutyName(), "the Epic of Alexander (Ultimate)");
		Assert.assertEquals(alex.placeName(), "Liminal Space");
	}
}
