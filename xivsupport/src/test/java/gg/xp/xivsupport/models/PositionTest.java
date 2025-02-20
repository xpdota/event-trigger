package gg.xp.xivsupport.models;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

public class PositionTest {

	@Test
	void testDistance2D() {
		Position p1 = new Position(5, 5, 10, 1.234);
		Position p2 = new Position(2, 9, 10, 1.234);
		double distance = p1.distanceFrom2D(p2);
		MatcherAssert.assertThat(distance, Matchers.closeTo(5, 0.01));
	}

	@Test
	void testAbsTranslate() {
		Position p1 = new Position(5, 5, 10, 1.234);
		Position p2 = p1.translateAbsolute(3, -2);
		MatcherAssert.assertThat(p2, Matchers.equalTo(new Position(8, 3, 10, 1.234)));
	}

	@Test
	void testRelTranslate1() {
		// Facing north
		Position before = new Position(5, 5, 10, Math.PI);
		Position after = before.translateRelative(3, -2);
		assertPositionsSame(after, new Position(8, 7, 10, Math.PI));
	}

	@Test
	void testRelTranslate2() {
		// Also facing north
		Position before = new Position(5, 5, 10, -1 * Math.PI);
		Position after = before.translateRelative(3, -2);
		assertPositionsSame(after, new Position(8, 7, 10, -1.0 * Math.PI));
	}

	@Test
	void testRelTranslate3() {
		// Facing south
		Position before = new Position(5, 5, 10, 0);
		Position after = before.translateRelative(3, -2);
		assertPositionsSame(after, new Position(2, 3, 10, 0));
	}

	@Test
	void testRelTranslate4() {
		// Facing east
		Position before = new Position(5, 5, 10, Math.PI / 2.0);
		Position after = before.translateRelative(3, -2);
		assertPositionsSame(after, new Position(3, 8, 10, Math.PI / 2.0));
	}

	@Test
	void testRelTranslate5() {
		// Facing west
		Position before = new Position(5, 5, 10, Math.PI / -2.0);
		Position after = before.translateRelative(3, -2);
		assertPositionsSame(after, new Position(7, 2, 10, Math.PI / -2.0));
	}

	@Test
	void testNormalize1() {
		// Let the basis face east
		Position basis = new Position(5, 7, 10, Math.PI / 2.0);
		// Position the thing in question north and a little east of it, facing north
		Position other = new Position(6.25, -1, 11, Math.PI);
		Position normalized = other.normalizedTo(basis);
		// Should now be west, a little north, facing west
		Position expected = new Position(-8, -1.25, 1, Math.PI / -2.0);
		assertPositionsSame(normalized, expected);
	}

	@Test
	void testNormalize2() {
		// Let the basis face east
		Position basis = new Position(5, 7, 10, Math.PI / 2.0);
		// Position the thing in question north and a little east of it, facing north +1
		Position other = new Position(6.25, -1, 11, Math.PI + 1);
		Position normalized = other.normalizedTo(basis);
		// Should now be west, a little north, facing west +1
		Position expected = new Position(-8, -1.25, 1, Math.PI / -2.0 + 1);
		assertPositionsSame(normalized, expected);
	}

	@Test
	void testNormalize3() {
		// Let the basis face west
		Position basis = new Position(5, 7, 10, Math.PI / -2.0);
		// Position the thing in question north and a little east of it, facing north
		Position other = new Position(6.25, -1, 11, Math.PI);
		Position normalized = other.normalizedTo(basis);
		// Should now be east, a little south, facing east
		Position expected = new Position(8, 1.25, 1, Math.PI / 2.0);
		assertPositionsSame(normalized, expected);
	}

	@Test
	void testPerpendicularIntersection() {
		// South, facing north
		Position a = new Position(0, 10, 1, ArenaSector.NORTH.facingAngle());
		// East, facing west
		Position b = new Position(10, 0, 0, ArenaSector.WEST.facingAngle());
		{
			Position result = Position.perpendicularIntersection(a, b);
			// These two should intersect at 10,10, as we would draw a line east from A and south from B
			// Since this is the outer heading, the heading should be SE
			assertPositionsSame(result, new Position(10, 10, 1, 0));
		}
	}
	@Test
	void testPerpendicularIntersection2() {
		// Flip one direction. Nothing should change.
		// South, facing north
		Position a = new Position(0, 10, 1, ArenaSector.NORTH.facingAngle());
		// East, facing east
		Position b = new Position(10, 0, 0, ArenaSector.EAST.facingAngle());
		{
			Position result = Position.perpendicularIntersection(a, b);
			// These two should intersect at 10,10, as we would draw a line east from A and south from B
			// Since this is the outer heading, the heading should be SE
			assertPositionsSame(result, new Position(10, 10, 1, 0));
		}
	}
	@Test
	void testPerpendicularIntersection3() {
		// Flip one direction. Nothing should change.
		// South, facing south
		Position a = new Position(0, 10, 1, ArenaSector.SOUTH.facingAngle());
		// East, facing east
		Position b = new Position(10, 0, 0, ArenaSector.EAST.facingAngle());
		{
			Position result = Position.perpendicularIntersection(a, b);
			// These two should intersect at 10,10, as we would draw a line east from A and south from B
			// Since this is the outer heading, the heading should be SE
			assertPositionsSame(result, new Position(10, 10, 1, 0));
		}
	}
	@Test
	void testPerpendicularIntersection4() {
		// South, facing south
		Position a = new Position(0, 10, 1, ArenaSector.SOUTH.facingAngle());
		// East, facing west
		Position b = new Position(10, 0, 0, ArenaSector.WEST.facingAngle());
		{
			Position result = Position.perpendicularIntersection(a, b);
			// These two should intersect at 10,10, as we would draw a line east from A and south from B
			// Since this is the outer heading, the heading should be SE
			assertPositionsSame(result, new Position(10, 10, 1, 0));
		}

	}

	@Test
	void testIntersection2() {
		// South, facing north
		Position a = new Position(0, 0, 0, ArenaSector.EAST.facingAngle());
		// East, facing west
		Position b = new Position(1, 1, 0, ArenaSector.SOUTH.facingAngle());
		Position result = Position.intersection(a, b);
		// These two should intersect at 10,10, as we would draw a line east from A and south from B
		assertPositionsSame(result, new Position(1, 0, 0, 0));
	}

	private void assertPositionsSame(Position actual, Position expected) {
		String message = String.format("Expected positions to be equal. Actual: %s; Expected: %s", actual, expected);
		MatcherAssert.assertThat(message, actual.getX(), Matchers.closeTo(expected.getX(), 0.01));
		MatcherAssert.assertThat(message, actual.getY(), Matchers.closeTo(expected.getY(), 0.01));
		MatcherAssert.assertThat(message, actual.getZ(), Matchers.closeTo(expected.getZ(), 0.01));
		MatcherAssert.assertThat(message, actual.getHeading(), Matchers.closeTo(expected.getHeading(), 0.01));
	}

}