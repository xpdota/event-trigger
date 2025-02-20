package gg.xp.xivsupport.models;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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

	@Test
	void testFacingAngles() {
		double error = 0.00001;
		MatcherAssert.assertThat(ArenaSector.NORTH.facingAngle(), Matchers.closeTo(Math.PI, error));
		MatcherAssert.assertThat(ArenaSector.NORTHEAST.facingAngle(), Matchers.closeTo(0.75 * Math.PI, error));
		MatcherAssert.assertThat(ArenaSector.EAST.facingAngle(), Matchers.closeTo(0.5 * Math.PI, error));
		MatcherAssert.assertThat(ArenaSector.SOUTHEAST.facingAngle(), Matchers.closeTo(0.25 * Math.PI, error));
		MatcherAssert.assertThat(ArenaSector.SOUTH.facingAngle(), Matchers.closeTo(0.0, error));
		MatcherAssert.assertThat(ArenaSector.SOUTHWEST.facingAngle(), Matchers.closeTo(-0.25 * Math.PI, error));
		MatcherAssert.assertThat(ArenaSector.WEST.facingAngle(), Matchers.closeTo(-0.5 * Math.PI, error));
		MatcherAssert.assertThat(ArenaSector.NORTHWEST.facingAngle(), Matchers.closeTo(-0.75 * Math.PI, error));
	}

}