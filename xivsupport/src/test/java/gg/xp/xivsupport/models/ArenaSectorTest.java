package gg.xp.xivsupport.models;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class ArenaSectorTest {

	@Test
	void testNorthCcwSort() {
		List<ArenaSector> actual = ArenaSector.all.stream().sorted(ArenaSector.northCcwSort).toList();
		List<ArenaSector> expected = List.of(
				ArenaSector.NORTH,
				ArenaSector.NORTHWEST,
				ArenaSector.WEST,
				ArenaSector.SOUTHWEST,
				ArenaSector.SOUTH,
				ArenaSector.SOUTHEAST,
				ArenaSector.EAST,
				ArenaSector.NORTHEAST
		);
		Assert.assertEquals(actual, expected);
	}

}