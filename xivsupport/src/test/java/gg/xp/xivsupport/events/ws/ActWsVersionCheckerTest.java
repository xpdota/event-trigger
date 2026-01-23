package gg.xp.xivsupport.events.ws;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ActWsVersionCheckerTest {

	/**
	 * Tests for the isOutOfDate method in the ActWsVersionChecker class.
	 * The isOutOfDate method evaluates whether the provided version string
	 * is considered out of date compared to the expected version.
	 */

	@Test
	public void testVersionIsUpToDate() {
		// Given
		String actualVersion = "0.19.15";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertFalse(result, "Expected the version to be up-to-date, but it was marked as out-of-date.");
	}

	@Test
	public void testVersionIsOutOfDate_MajorVersionLower() {
		// Given
		String actualVersion = "0.18.14";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertTrue(result, "Expected the version to be out-of-date, but it was marked as up-to-date.");
	}

	@Test
	public void testVersionIsOutOfDate_MinorVersionLower() {
		// Given
		String actualVersion = "0.19.13";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertTrue(result, "Expected the version to be out-of-date, but it was marked as up-to-date.");
	}

	@Test
	public void testVersionIsOutOfDate_PatchVersionLower() {
		// Given
		String actualVersion = "0.19.12";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertTrue(result, "Expected the version to be out-of-date, but it was marked as up-to-date.");
	}

	@Test
	public void testVersionIsEqual() {
		// Given
		String actualVersion = "0.19.14";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertFalse(result, "Expected the version to be up-to-date, but it was marked as out-of-date.");
	}

	@Test
	public void testVersionHasMoreSegments() {
		// Given
		String actualVersion = "0.19.14.1";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertFalse(result, "Expected the version to be up-to-date, but it was marked as out-of-date.");
	}

	@Test
	public void testVersionHasMoreSegmentsNegative() {
		// Given
		String actualVersion = "0.18.14.1";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertTrue(result, "Expected the version to be up-to-date, but it was marked as out-of-date.");
	}

	@Test
	public void testVersionHasFewerSegments() {
		// Given
		String actualVersion = "0.19";

		// When
		boolean result = ActWsVersionChecker.isOutOfDate(actualVersion);

		// Then
		Assert.assertFalse(result, "Expected the version to be flagged as unusual but not as out-of-date.");
	}

	@Test
	public void testVersionIsUnusualFormat() {
		// Given
		String actualVersion = "invalid.version";

		// When
		try {
			ActWsVersionChecker.isOutOfDate(actualVersion);
			Assert.fail("Expected an exception for an invalid version format, but none was thrown.");
		}
		catch (NumberFormatException e) {
			// Expected an exception due to the invalid format
			Assert.assertTrue(true, "Expected exception was thrown.");
		}
	}
}