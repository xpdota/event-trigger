package gg.xp.xivsupport.gameversion;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GameVersionTests {
	// TODO: update this. This technically works but the values don't make sense. Game versions
	// are along the lines of "2024.06.18.0000.0000", not the user-friendly game version.
	@Test
	void testVersion1() {
		var controller = new GameVersionController();
		controller.setVersion(GameVersion.fromString("7.0"));

		Assert.assertTrue(controller.isAtLeast(GameVersion.fromString("6.0")));
		Assert.assertTrue(controller.isAtLeast(GameVersion.fromString("7")));
		Assert.assertTrue(controller.isAtLeast(GameVersion.fromString("7.0")));
		Assert.assertTrue(controller.isAtLeast(GameVersion.fromString("7.0.0")));
		Assert.assertFalse(controller.isAtLeast(GameVersion.fromString("8.0")));

		Assert.assertTrue(controller.isNewerThan(GameVersion.fromString("6.0")));
		Assert.assertFalse(controller.isNewerThan(GameVersion.fromString("7")));
		Assert.assertFalse(controller.isNewerThan(GameVersion.fromString("7.0")));
		Assert.assertFalse(controller.isNewerThan(GameVersion.fromString("7.0.0")));
		Assert.assertFalse(controller.isNewerThan(GameVersion.fromString("8.0")));

		Assert.assertFalse(controller.isExactly(GameVersion.fromString("6.0")));
		Assert.assertTrue(controller.isExactly(GameVersion.fromString("7")));
		Assert.assertTrue(controller.isExactly(GameVersion.fromString("7.0")));
		Assert.assertTrue(controller.isExactly(GameVersion.fromString("7.0.0")));
		Assert.assertFalse(controller.isExactly(GameVersion.fromString("8.0")));

		Assert.assertFalse(controller.isUpTo(GameVersion.fromString("6.0")));
		Assert.assertTrue(controller.isUpTo(GameVersion.fromString("7")));
		Assert.assertTrue(controller.isUpTo(GameVersion.fromString("7.0")));
		Assert.assertTrue(controller.isUpTo(GameVersion.fromString("7.0.0")));
		Assert.assertTrue(controller.isUpTo(GameVersion.fromString("8.0")));

		Assert.assertFalse(controller.isOlderThan(GameVersion.fromString("6.0")));
		Assert.assertFalse(controller.isOlderThan(GameVersion.fromString("7")));
		Assert.assertFalse(controller.isOlderThan(GameVersion.fromString("7.0")));
		Assert.assertFalse(controller.isOlderThan(GameVersion.fromString("7.0.0")));
		Assert.assertTrue(controller.isOlderThan(GameVersion.fromString("8.0")));
	}

	@Test
	void testVersionPointFive() {
		var controller = new GameVersionController();
		controller.setVersion(GameVersion.fromString("6.55"));

		Assert.assertEquals(Math.signum(controller.getVersion().compareTo(GameVersion.fromString("6.55"))), 0);
		Assert.assertEquals(Math.signum(controller.getVersion().compareTo(GameVersion.fromString("6.5"))), 1);
		Assert.assertEquals(Math.signum(controller.getVersion().compareTo(GameVersion.fromString("6.45"))), 1);
		Assert.assertEquals(Math.signum(controller.getVersion().compareTo(GameVersion.fromString("6.58"))), -1);
		Assert.assertEquals(Math.signum(controller.getVersion().compareTo(GameVersion.fromString("6.100"))), 1);

	}
}
